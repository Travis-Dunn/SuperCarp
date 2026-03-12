package production.carpscript;

/**
 * The interface between the script world and your Java game engine.
 *
 * Every command that scripts can call (mes, p_delay, say, inv_total, etc.)
 * is a Java class that implements this interface. The VM doesn't know or
 * care what these commands do — it just calls execute() and lets the
 * command interact with the game.
 *
 * WHY AN INTERFACE?
 * ─────────────────
 * This is the "engine bindings" component from our architecture list.
 * The power of this design is that the VM is completely generic — it knows
 * nothing about NPCs, items, chat messages, or any game concept. All of
 * that knowledge lives in CommandHandler implementations.
 *
 * Want to add a new command? Just implement this interface and register it.
 * The parser, VM, and everything else remain untouched.
 *
 * ARGUMENTS AND RETURN VALUES:
 * ────────────────────────────
 * Arguments arrive as an Object array, popped from the VM's stack.
 * The array is ordered left-to-right as written in the script:
 *   give(995, 1000)  →  args[0] = 995, args[1] = 1000
 *
 * If the command returns a value (like inv_total returns a count),
 * return it from execute() and the VM will push it onto the stack.
 * If the command doesn't return anything (like mes), return null.
 *
 * SUSPENSION:
 * ───────────
 * Some commands need to pause the script. p_delay pauses for N game ticks.
 * To signal this, the command sets scriptState.state = SUSPENDED and
 * scriptState.delayTicks = N. The VM checks the state after every command
 * and stops executing if the script suspended.
 */
public interface CommandHandler {

    /**
     * Execute this command.
     *
     * @param state  The current script execution state. Commands that need
     *               to suspend the script (like p_delay) modify this directly.
     * @param args   Arguments popped from the stack, in left-to-right order.
     *               The array length matches the arg count from the INVOKE
     *               instruction.
     * @param playerVars  The player's persistent variable storage. Commands
     *                    can read/write these, though typically only the VM
     *                    itself handles %vars via PUSH_VARP/POP_VARP.
     *                    Passed here so commands CAN access them if needed.
     * @return A value to push onto the stack, or null if this command
     *         doesn't return anything.
     */
    Object execute(ScriptState state, Object[] args, java.util.Map<String, Object> playerVars);
}