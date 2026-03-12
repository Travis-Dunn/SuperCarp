package production.carpscript;

/**
 * Every type of instruction our script VM understands.
 *
 * Think of these like CPU instructions, but for our little scripting language.
 * A real CPU has instructions like MOV, ADD, JMP — ours are similar but
 * tailored to game scripting.
 *
 * The VM is "stack-based," which means most operations work by pushing values
 * onto a stack, then popping them off to do something. For example:
 *
 *   Script:  mes("Hello")
 *   Becomes: PUSH_STRING "Hello"    <- push the argument onto the stack
 *            INVOKE "mes" (1 arg)   <- pop 1 arg, call the mes() command
 *
 *   Script:  if (%quest_progress = 2)
 *   Becomes: PUSH_VARP "quest_progress"  <- push the variable's value
 *            PUSH_INT 2                   <- push the number to compare against
 *            INVOKE "eq" (2 args)         <- pop both, push 1 if equal, 0 if not
 *            JUMP_IF_NOT #14              <- pop result, jump if it was 0 (false)
 */
public enum Opcode {

    // ── Stack Operations ──────────────────────────────────────────────
    // These put values onto the stack for other instructions to consume.

    /** Push an integer literal onto the stack. Operand: the int value. */
    PUSH_INT,

    /** Push a string literal onto the stack. Operand: the string value. */
    PUSH_STRING,

    // ── Command Invocation ────────────────────────────────────────────

    /**
     * Call an engine command (like mes, p_delay, say, etc.)
     * Operand: the command name (String).
     * Second operand: argument count (int).
     *
     * The VM pops 'argCount' values off the stack (right-to-left),
     * passes them to the Java method registered under that name,
     * and if it returns a value, pushes it back on the stack.
     */
    INVOKE,

    // ── Control Flow ──────────────────────────────────────────────────

    /**
     * Unconditional jump. Sets the program counter to the operand value.
     * Operand: target instruction index (int).
     *
     * Used for: skipping over else-blocks, looping (if we add loops later).
     */
    JUMP,

    /**
     * Conditional jump. Pops the top of the stack:
     *   - If the value is 0 (false): jump to the target instruction.
     *   - If the value is non-zero (true): continue to the next instruction.
     * Operand: target instruction index (int).
     *
     * Used for: the "if" part of if/else. The parser emits this right after
     * a comparison so the VM skips the if-body when the condition is false.
     */
    JUMP_IF_NOT,

    // ── Variable Access ───────────────────────────────────────────────

    /**
     * Push a local variable's value onto the stack.
     * Operand: variable index (int) — each $local gets a numeric slot.
     *
     * Local variables only exist during one script execution.
     * $name in the script becomes an index like 0, 1, 2... assigned by the parser.
     */
    PUSH_LOCAL,

    /**
     * Pop the top of the stack and store it in a local variable slot.
     * Operand: variable index (int).
     */
    POP_LOCAL,

    /**
     * Push a player variable's value onto the stack.
     * Operand: variable name (String) — looked up on the Player object.
     *
     * Player variables (% vars) persist across script executions and are
     * saved to disk. Quest progress, flags, counters — all live here.
     */
    PUSH_VARP,

    /**
     * Pop the top of the stack and store it in a player variable.
     * Operand: variable name (String).
     */
    POP_VARP,

    // ── Lifecycle ─────────────────────────────────────────────────────

    /**
     * End script execution immediately.
     * No operand. The VM marks the script as FINISHED.
     *
     * Scripts also end naturally when the program counter moves past
     * the last instruction, but RETURN lets you exit early from inside
     * an if-block, for example.
     */
    RETURN
}