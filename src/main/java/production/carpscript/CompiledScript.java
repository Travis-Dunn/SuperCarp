package production.carpscript;

/**
 * The output of parsing a single trigger block.
 *
 * A source file can contain multiple trigger blocks, so the parser returns
 * a list of these. Each one is a self-contained unit: a trigger key (type
 * + subject) paired with its compiled instruction array.
 *
 * At game init time, these get loaded into the TriggerRegistry. At runtime,
 * the engine looks up the right CompiledScript by trigger key, creates a
 * fresh ScriptState from its instructions, and hands it to the VM.
 *
 * Example:
 *   Source file contains:
 *     [opnpc,Guard]
 *     mes("Halt!")
 *
 *     [opnpc,Shopkeeper]
 *     mes("Welcome!")
 *
 *   Parser produces two CompiledScripts:
 *     { triggerType="opnpc", subject="Guard",      instructions=[...] }
 *     { triggerType="opnpc", subject="Shopkeeper",  instructions=[...] }
 */
public class CompiledScript {

    /** The trigger type, e.g. "opnpc", "opobj", "ai_timer". */
    public final String triggerType;

    /** The trigger subject, e.g. "Guard", "Coins", "Goblin". */
    public final String subject;

    /** The compiled instruction array, ready to be loaded into a ScriptState. */
    public final Instruction[] instructions;

    /** The source file this was compiled from (for error messages). */
    public final String sourceFile;

    public CompiledScript(String triggerType, String subject,
                          Instruction[] instructions, String sourceFile) {
        this.triggerType = triggerType;
        this.subject = subject;
        this.instructions = instructions;
        this.sourceFile = sourceFile;
    }

    /**
     * Create a fresh ScriptState for executing this script.
     * Called each time a trigger fires — every execution gets its own state.
     */
    public ScriptState createState() {
        return new ScriptState(instructions, triggerType, subject);
    }

    @Override
    public String toString() {
        return "[" + triggerType + "," + subject + "] (" +
                instructions.length + " instructions) from " + sourceFile;
    }
}