package production.carpscript;

import java.util.HashMap;
import java.util.Map;

/**
 * The script virtual machine. This is the beating heart of CarpScript.
 *
 * WHAT IT DOES:
 * ─────────────
 * Takes a ScriptState and executes its instructions one at a time until
 * the script either finishes or suspends. That's it. The VM itself has
 * no game-specific logic — all it knows is how to manipulate a stack,
 * handle control flow, and delegate to registered CommandHandlers.
 *
 * THE EXECUTION LOOP:
 * ───────────────────
 * The core is a while loop:
 *   1. Check if we should keep running (state == RUNNING, pc in bounds)
 *   2. Fetch the instruction at the current program counter
 *   3. Advance the program counter
 *   4. Switch on the opcode, do the right thing
 *   5. After INVOKE, check if the command suspended us — if so, break out
 *   6. Repeat
 *
 * SUSPEND AND RESUME:
 * ───────────────────
 * This is the most important concept to understand. When a script calls
 * p_delay(2), here's exactly what happens:
 *
 *   1. The VM executes the INVOKE for p_delay
 *   2. The p_delay CommandHandler sets state.state = SUSPENDED, state.delayTicks = 2
 *   3. The VM's loop checks state after INVOKE, sees SUSPENDED, and breaks out
 *   4. execute() returns — control goes back to your game engine
 *   5. Your game engine's tick loop counts down delayTicks each tick
 *   6. When delayTicks reaches 0, the engine sets state.state = RUNNING
 *   7. The engine calls execute(state, playerVars) AGAIN with the same ScriptState
 *   8. The VM picks up right where it left off — pc is already pointing at
 *      the instruction AFTER the p_delay INVOKE
 *
 * The ScriptState object is the "save file" for the script's execution.
 * The VM is stateless — it's just the code that reads and acts on that state.
 *
 * SAFETY LIMIT:
 * ─────────────
 * To protect against infinite loops (which our minimal language can't
 * even create yet, but defensive coding is good), the VM has a maximum
 * number of instructions it will execute in one call. If a script hits
 * this limit without finishing or suspending, the VM forces it to finish
 * with an error. In RuneScript, this was a real concern.
 */
public class ScriptVM {

    /**
     * Registered command handlers, keyed by command name.
     * When the VM hits an INVOKE instruction, it looks up the command
     * name in this map to find the Java code that should run.
     */
    private final Map<String, CommandHandler> commands;

    /**
     * Safety limit: max instructions per execute() call.
     * Prevents runaway scripts from hanging your game.
     */
    private static final int MAX_INSTRUCTIONS_PER_CYCLE = 5000;

    public ScriptVM() {
        this.commands = new HashMap<String, CommandHandler>();
    }

    // ── Command Registration ──────────────────────────────────────────

    /**
     * Register a command handler. Scripts can then call this command by name.
     *
     * Example:
     *   vm.registerCommand("mes", new MesCommandHandler());
     *
     * After this, any script that has INVOKE "mes" will call your handler.
     */
    public void registerCommand(String name, CommandHandler handler) {
        commands.put(name, handler);
    }

    // ── Execution ─────────────────────────────────────────────────────

    /**
     * Execute (or resume) a script.
     *
     * Call this when:
     *   - A trigger fires and you have a fresh ScriptState to run
     *   - A suspended script's delay has expired and it's time to resume
     *
     * In both cases, the ScriptState's state should be RUNNING when you
     * call this. The method returns when the script finishes or suspends.
     *
     * @param state       The script execution state (fresh or previously suspended)
     * @param playerVars  The player's persistent variables (%vars). Pass the
     *                    same map every time for a given player.
     */
    public void execute(ScriptState state, Map<String, Object> playerVars) {
        // Safety check: don't try to execute a finished script
        if (state.state == ExecutionState.FINISHED) {
            return;
        }

        // Mark as running (in case we're resuming from SUSPENDED)
        state.state = ExecutionState.RUNNING;

        int instructionsExecuted = 0;

        // ── The Main Loop ─────────────────────────────────────────────
        while (state.state == ExecutionState.RUNNING) {

            // Check if we've run past the end of the instruction array.
            // This is the natural way scripts end — they simply run out
            // of instructions. Equivalent to reaching the closing brace
            // of the trigger block.
            if (state.pc >= state.instructions.length) {
                state.state = ExecutionState.FINISHED;
                break;
            }

            // Safety: prevent infinite/runaway execution
            instructionsExecuted++;
            if (instructionsExecuted > MAX_INSTRUCTIONS_PER_CYCLE) {
                System.err.println("ERROR: Script exceeded max instructions! " +
                        "Trigger: [" + state.triggerType + "," + state.triggerSubject + "]");
                state.state = ExecutionState.FINISHED;
                break;
            }

            // ── Fetch ─────────────────────────────────────────────────
            // Read the current instruction and advance the program counter.
            // We advance BEFORE executing so that when we resume from a
            // suspension, pc is already pointing at the next instruction.
            Instruction instr = state.instructions[state.pc];
            state.pc++;

            // ── Decode & Execute ──────────────────────────────────────
            switch (instr.opcode) {

                case PUSH_INT:
                    // Push an integer literal onto the stack.
                    // Example: PUSH_INT 42  →  stack: [..., 42]
                    state.push(instr.intOperand());
                    break;

                case PUSH_STRING:
                    // Push a string literal onto the stack.
                    // Example: PUSH_STRING "hello"  →  stack: [..., "hello"]
                    state.push(instr.stringOperand());
                    break;

                case INVOKE:
                    executeInvoke(state, instr, playerVars);
                    // CRITICAL: After any command executes, check if it
                    // suspended the script. If so, we must stop immediately.
                    // The pc is already advanced past this INVOKE, so when
                    // we resume later, we'll pick up at the next instruction.
                    // (no extra check needed here — the while-loop condition
                    //  checks state.state == RUNNING, so if the command set
                    //  it to SUSPENDED, the loop exits naturally)
                    break;

                case JUMP:
                    // Set the program counter to the target instruction.
                    // This is how we skip over else-blocks or loop back.
                    // Example: JUMP 14  →  pc becomes 14
                    state.pc = instr.intOperand();
                    break;

                case JUMP_IF_NOT:
                    // Pop the top of the stack. If it's 0 (false), jump.
                    // If it's non-zero (true), do nothing (continue to next).
                    //
                    // This is emitted right after a comparison instruction.
                    // Example flow:
                    //   PUSH_VARP "quest"     stack: [2]
                    //   PUSH_INT 0            stack: [2, 0]
                    //   INVOKE "eq" 2         stack: [0]      (2 != 0, so false)
                    //   JUMP_IF_NOT 10        pops 0, it IS 0, so jump to 10
                    int condition = state.popInt();
                    if (condition == 0) {
                        state.pc = instr.intOperand();
                    }
                    break;

                case PUSH_LOCAL:
                    // Read a local variable and push its value.
                    // The operand is the slot index assigned by the parser.
                    state.push(state.getLocal(instr.intOperand()));
                    break;

                case POP_LOCAL:
                    // Pop the stack and store the value in a local variable.
                    state.setLocal(instr.intOperand(), state.pop());
                    break;

                case PUSH_VARP:
                    // Read a player variable and push its value.
                    // If the variable hasn't been set yet, default to 0
                    // (this matches RuneScript behavior — new vars start at 0).
                {
                    String varName = instr.stringOperand();
                    Object value = playerVars.get(varName);
                    if (value == null) {
                        value = Integer.valueOf(0);
                    }
                    state.push(value);
                }
                break;

                case POP_VARP:
                    // Pop the stack and store the value as a player variable.
                {
                    String varName = instr.stringOperand();
                    Object value = state.pop();
                    playerVars.put(varName, value);
                }
                break;

                case RETURN:
                    // Immediately end execution.
                    state.state = ExecutionState.FINISHED;
                    break;

                default:
                    throw new RuntimeException("Unknown opcode: " + instr.opcode +
                            " at instruction " + (state.pc - 1));
            }
        }
    }

    // ── Command Dispatch ──────────────────────────────────────────────

    /**
     * Handle an INVOKE instruction: look up the command, pop its arguments
     * from the stack, call it, and push any return value.
     */
    private void executeInvoke(ScriptState state, Instruction instr,
                               Map<String, Object> playerVars) {
        String commandName = instr.stringOperand();
        int argCount = instr.operand2;

        // Look up the command handler
        CommandHandler handler = commands.get(commandName);
        if (handler == null) {
            throw new RuntimeException("Unknown command: " + commandName +
                    " — did you forget to register it? " +
                    "Trigger: [" + state.triggerType + "," + state.triggerSubject + "]");
        }

        // Pop arguments from the stack.
        // Arguments were pushed left-to-right, so we pop in reverse order
        // and then reverse to get them back in the original order.
        //
        // Script:  give(995, 1000)
        // Pushes:  PUSH_INT 995, PUSH_INT 1000
        // Stack:   [995, 1000]  (1000 is on top)
        // Pop:     pop → 1000, pop → 995
        // Args:    [995, 1000]  (we reverse to restore original order)
        Object[] args = new Object[argCount];
        for (int i = argCount - 1; i >= 0; i--) {
            args[i] = state.pop();
        }

        // Call the command
        Object result = handler.execute(state, args, playerVars);

        // If the command returned a value AND didn't suspend the script,
        // push the result onto the stack for the next instruction to use.
        // We don't push if suspended because the return value (if any)
        // isn't meaningful for commands like p_delay.
        if (result != null && state.state == ExecutionState.RUNNING) {
            state.push(result);
        }
    }
}