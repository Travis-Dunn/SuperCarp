package production.carpscript;

/**
 * A single instruction in a compiled script.
 *
 * An instruction is just an opcode (what to do) paired with an operand
 * (the data it operates on). Not every opcode needs an operand — RETURN
 * doesn't, for instance — so the operand can be null.
 *
 * Examples of what instructions look like after compilation:
 *
 *   Instruction(PUSH_STRING, "Hello world")   — operand is a String
 *   Instruction(PUSH_INT, 42)                 — operand is an Integer
 *   Instruction(INVOKE, "mes", 1)             — has both a name and arg count
 *   Instruction(JUMP, 14)                     — operand is instruction index
 *   Instruction(PUSH_LOCAL, 0)                — operand is variable slot index
 *   Instruction(RETURN, null)                 — no operand needed
 *
 * We store operands as Object because they can be different types.
 * In a more optimized system you'd use typed fields or encode everything
 * as ints with a constant pool — but Object keeps things simple for learning.
 */
public class Instruction {

    /** What operation to perform. */
    public final Opcode opcode;

    /**
     * Primary operand. Its meaning depends on the opcode:
     *   PUSH_INT    → Integer value
     *   PUSH_STRING → String value
     *   INVOKE      → String command name
     *   JUMP        → Integer target index
     *   JUMP_IF_NOT → Integer target index
     *   PUSH_LOCAL  → Integer variable slot
     *   POP_LOCAL   → Integer variable slot
     *   PUSH_VARP   → String variable name
     *   POP_VARP    → String variable name
     *   RETURN      → null
     */
    public final Object operand;

    /**
     * Secondary operand, used only by INVOKE to store the argument count.
     * For all other opcodes this is 0.
     *
     * Why does INVOKE need two operands? Because the VM needs to know both
     * WHICH command to call AND how many arguments to pop off the stack.
     */
    public final int operand2;

    /** Constructor for instructions with no operand (like RETURN). */
    public Instruction(Opcode opcode) {
        this(opcode, null, 0);
    }

    /** Constructor for instructions with one operand (most instructions). */
    public Instruction(Opcode opcode, Object operand) {
        this(opcode, operand, 0);
    }

    /** Constructor for instructions with two operands (INVOKE). */
    public Instruction(Opcode opcode, Object operand, int operand2) {
        this.opcode = opcode;
        this.operand = operand;
        this.operand2 = operand2;
    }

    /**
     * Helper to get the operand as an int. Convenience method so the VM
     * doesn't have to cast everywhere.
     */
    public int intOperand() {
        return ((Integer) operand).intValue();
    }

    /**
     * Helper to get the operand as a String.
     */
    public String stringOperand() {
        return (String) operand;
    }

    @Override
    public String toString() {
        if (operand == null) {
            return opcode.name();
        }
        if (opcode == Opcode.INVOKE) {
            return opcode.name() + " " + operand + " (args: " + operand2 + ")";
        }
        if (operand instanceof String) {
            return opcode.name() + " \"" + operand + "\"";
        }
        return opcode.name() + " " + operand;
    }
}