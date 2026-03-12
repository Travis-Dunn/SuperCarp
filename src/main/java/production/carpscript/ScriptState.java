package production.carpscript;

/**
 * ScriptState holds ALL the context needed to execute (or resume) a script.
 *
 * When the game engine says "run this script for this player," it creates a
 * ScriptState and hands it to the VM. The VM reads instructions from it,
 * modifies the program counter and stack, and returns when the script either
 * finishes or suspends.
 *
 * THE KEY DESIGN PRINCIPLE: The VM itself is stateless. All execution state
 * lives here in ScriptState. This means:
 *   - You can have many scripts running "concurrently" (each player might
 *     have a suspended dialogue script, a queued action, etc.)
 *   - Suspending is just "stop touching this ScriptState and come back later"
 *   - Resuming is just "hand the same ScriptState back to the VM"
 *
 * Think of ScriptState like a bookmark in a book — it remembers the page
 * (program counter), your notes (stack and locals), and which book
 * (the instruction array).
 *
 * Stack-Based Architecture:
 * ─────────────────────────
 * Instead of named registers, we use a stack. When the script says:
 *   mes("Hello")
 * The compiled instructions are:
 *   PUSH_STRING "Hello"   ← stack is now: ["Hello"]
 *   INVOKE "mes" 1        ← pop 1 arg → call mes("Hello") → stack is now: []
 *
 * For something with multiple args like a hypothetical "give(item, count)":
 *   PUSH_INT 995          ← stack: [995]
 *   PUSH_INT 1000         ← stack: [995, 1000]
 *   INVOKE "give" 2       ← pop 2 → call give(995, 1000) → stack: []
 *
 * Arguments are pushed left-to-right, so the first argument is deepest
 * in the stack and the last argument is on top.
 */
public class ScriptState {

    // ── Identity ──────────────────────────────────────────────────────

    /**
     * The compiled instructions for this script.
     * Set once when the ScriptState is created and never modified.
     */
    public final Instruction[] instructions;

    /**
     * Which trigger activated this script (e.g., "opnpc" + "Guard").
     * Useful for debugging — not strictly needed for execution.
     */
    public final String triggerType;
    public final String triggerSubject;

    // ── Execution State ───────────────────────────────────────────────

    /** Current state: RUNNING, SUSPENDED, or FINISHED. */
    public ExecutionState state;

    /**
     * Program counter — the index of the NEXT instruction to execute.
     *
     * The VM reads instructions[pc], then increments pc (unless the
     * instruction is a JUMP, which sets pc directly).
     *
     * When a script suspends, pc stays where it is. When the script
     * resumes, the VM picks up from this exact position.
     */
    public int pc;

    /**
     * How many game ticks remain before a suspended script should resume.
     * Set by p_delay(N). Each game tick, the engine decrements this.
     * When it reaches 0, the engine tells the VM to resume this script.
     */
    public int delayTicks;

    // ── Stack ─────────────────────────────────────────────────────────

    /**
     * The operand stack. We use Object[] so it can hold both Integers
     * and Strings (the two types our minimal language supports).
     *
     * A fixed-size array is fine — RuneScript itself used a fixed stack.
     * If a script somehow overflows this, it's a bug in the script.
     */
    private final Object[] stack;

    /**
     * Points to the next empty slot on the stack.
     *
     *   stackPtr = 0 means the stack is empty.
     *   stackPtr = 3 means there are 3 items (at indices 0, 1, 2).
     *   push puts a value at stack[stackPtr] then increments.
     *   pop decrements stackPtr then reads stack[stackPtr].
     */
    private int stackPtr;

    // ── Local Variables ───────────────────────────────────────────────

    /**
     * Local variable storage. Each $variable in the script gets a numeric
     * slot (0, 1, 2...) assigned by the parser. The slot is used as the
     * index into this array.
     *
     * These are created fresh for each script execution and discarded
     * when the script finishes. They survive suspension though — that's
     * the whole point of keeping them in ScriptState.
     */
    private final Object[] locals;

    // ── Constants ─────────────────────────────────────────────────────

    /** Maximum stack depth. 50 is generous for simple scripts. */
    private static final int MAX_STACK = 50;

    /** Maximum number of local variables per script. */
    private static final int MAX_LOCALS = 20;

    // ── Constructor ───────────────────────────────────────────────────

    public ScriptState(Instruction[] instructions, String triggerType, String triggerSubject) {
        this.instructions = instructions;
        this.triggerType = triggerType;
        this.triggerSubject = triggerSubject;

        this.state = ExecutionState.RUNNING;
        this.pc = 0;
        this.delayTicks = 0;

        this.stack = new Object[MAX_STACK];
        this.stackPtr = 0;

        this.locals = new Object[MAX_LOCALS];
    }

    // ── Stack Operations ──────────────────────────────────────────────

    /**
     * Push a value onto the stack.
     * Used by PUSH_INT, PUSH_STRING, PUSH_LOCAL, PUSH_VARP, and by
     * INVOKE when a command returns a value.
     */
    public void push(Object value) {
        if (stackPtr >= MAX_STACK) {
            throw new RuntimeException("Script stack overflow! Too many values on the stack. " +
                    "Trigger: [" + triggerType + "," + triggerSubject + "] at instruction " + pc);
        }
        stack[stackPtr++] = value;
    }

    /**
     * Pop a value off the stack.
     * Used by INVOKE (to get arguments), JUMP_IF_NOT (to get the condition),
     * POP_LOCAL, and POP_VARP.
     */
    public Object pop() {
        if (stackPtr <= 0) {
            throw new RuntimeException("Script stack underflow! Tried to pop from empty stack. " +
                    "Trigger: [" + triggerType + "," + triggerSubject + "] at instruction " + pc);
        }
        return stack[--stackPtr];
    }

    /** Pop and cast to int. Convenience for the common case. */
    public int popInt() {
        Object val = pop();
        if (val instanceof Integer) {
            return ((Integer) val).intValue();
        }
        throw new RuntimeException("Expected int on stack but got: " + val +
                " Trigger: [" + triggerType + "," + triggerSubject + "] at instruction " + pc);
    }

    /** Pop and cast to String. */
    public String popString() {
        Object val = pop();
        if (val instanceof String) {
            return (String) val;
        }
        throw new RuntimeException("Expected string on stack but got: " + val +
                " Trigger: [" + triggerType + "," + triggerSubject + "] at instruction " + pc);
    }

    /** Peek at the top of the stack without removing it. Useful for debugging. */
    public Object peek() {
        if (stackPtr <= 0) {
            return null;
        }
        return stack[stackPtr - 1];
    }

    /** How many values are currently on the stack. */
    public int stackSize() {
        return stackPtr;
    }

    // ── Local Variable Access ─────────────────────────────────────────

    /** Read a local variable by slot index. */
    public Object getLocal(int slot) {
        if (slot < 0 || slot >= MAX_LOCALS) {
            throw new RuntimeException("Invalid local variable slot: " + slot);
        }
        return locals[slot];
    }

    /** Write a local variable by slot index. */
    public void setLocal(int slot, Object value) {
        if (slot < 0 || slot >= MAX_LOCALS) {
            throw new RuntimeException("Invalid local variable slot: " + slot);
        }
        locals[slot] = value;
    }

    // ── Debug ─────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "ScriptState{[" + triggerType + "," + triggerSubject + "] " +
                "state=" + state + " pc=" + pc + " stackSize=" + stackPtr + "}";
    }

    /**
     * Print the full instruction listing with a marker at the current pc.
     * Handy for debugging.
     */
    public void dumpInstructions() {
        System.out.println("=== Script [" + triggerType + "," + triggerSubject + "] ===");
        for (int i = 0; i < instructions.length; i++) {
            String marker = (i == pc) ? " --> " : "     ";
            System.out.println(marker + String.format("%3d", i) + ": " + instructions[i]);
        }
        System.out.println("=== state=" + state + " stackSize=" + stackPtr + " ===");
    }
}