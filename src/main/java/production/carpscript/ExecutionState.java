package production.carpscript;

/**
 * The possible states a running script can be in.
 *
 * A script isn't always actively executing — it might be paused waiting
 * for a delay to expire, or waiting for the player to pick a dialogue
 * option. This enum tracks where in its lifecycle a script is.
 */
public enum ExecutionState {

    /**
     * The script is actively executing instructions.
     * The VM is stepping through its instruction array right now.
     */
    RUNNING,

    /**
     * The script is paused and waiting to be resumed.
     *
     * This happens when the script calls p_delay(N) — the VM saves its
     * state and hands control back to the game engine. After N game ticks,
     * the engine calls back into the VM to resume from where it left off.
     *
     * The key insight: the ScriptState object holds ALL the context needed
     * to resume — the program counter, the stack, local variables. So the
     * VM itself can be stateless; all the state lives in this object.
     */
    SUSPENDED,

    /**
     * The script has finished execution (hit RETURN or ran past the last
     * instruction). This ScriptState can be discarded.
     */
    FINISHED
}