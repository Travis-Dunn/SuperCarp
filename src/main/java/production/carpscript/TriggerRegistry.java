package production.carpscript;

import java.util.HashMap;
import java.util.Map;

/**
 * The trigger registry maps game events to compiled scripts.
 *
 * WHAT IT DOES:
 * ─────────────
 * At game init, the ScriptLoader compiles all script files and registers
 * the results here. At runtime, when the game engine detects an event
 * (player talks to an NPC, logs in, etc.), it asks the registry: "is
 * there a script for this?" If so, it creates a fresh ScriptState and
 * hands it to the VM.
 *
 * TRIGGER KEYS:
 * ─────────────
 * A trigger is identified by two strings: a type and a subject.
 *
 *   Type       Subject       Meaning
 *   ────       ───────       ───────
 *   opnpc      Guard         Player interacts with the "Guard" NPC
 *   opnpc      Shopkeeper    Player interacts with the "Shopkeeper" NPC
 *   login      on_login      Player logs in / loads save
 *   logout     on_logout     Player logs out / saves
 *
 * The registry doesn't know or care what these types mean — it's just a
 * two-level map. The game engine is responsible for knowing WHEN to look
 * up "opnpc" + "Guard" (answer: when the player clicks "Talk-to" on a
 * guard NPC). This means adding new trigger types later (opobj for objects,
 * opheld for items, ai_timer for NPC AI) requires zero changes here —
 * you just start firing lookups from new places in your engine.
 *
 * The key is stored as "type:subject" (e.g. "opnpc:Guard") for simplicity.
 * Case-sensitive — "Guard" and "guard" are different triggers.
 *
 * DESIGN NOTE ON GENERICITY:
 * ──────────────────────────
 * You might wonder why this isn't an enum of trigger types, or why there's
 * no validation of trigger types. It's intentional. RuneScript started with
 * a handful of triggers and grew to dozens over the years. By keeping the
 * registry as a simple string→script map, new trigger types are free — you
 * don't need to modify any scripting system code to add them. The only
 * thing that needs to know about a trigger type is the engine code that
 * fires it.
 */
public class TriggerRegistry {

    /**
     * The main storage: trigger key → compiled script.
     * Key format is "type:subject", e.g. "opnpc:Guard".
     */
    private final Map<String, CompiledScript> scripts;

    public TriggerRegistry() {
        this.scripts = new HashMap<String, CompiledScript>();
    }

    // ── Registration (called at init time) ────────────────────────────

    /**
     * Register a compiled script. Called by the ScriptLoader during game init.
     *
     * If a trigger key is already registered, the new script replaces it.
     * This makes it easy to override scripts during development — just
     * reload and the latest version wins.
     *
     * @param script  A compiled trigger block from the parser.
     */
    public void register(CompiledScript script) {
        String key = makeKey(script.triggerType, script.subject);

        if (scripts.containsKey(key)) {
            System.out.println("[TriggerRegistry] Warning: overwriting existing trigger [" +
                    script.triggerType + "," + script.subject + "] " +
                    "(previously from " + scripts.get(key).sourceFile +
                    ", now from " + script.sourceFile + ")");
        }

        scripts.put(key, script);
    }

    // ── Lookup (called at runtime) ────────────────────────────────────

    /**
     * Look up a script by trigger type and subject.
     *
     * Returns the CompiledScript if one is registered, or null if no script
     * handles this trigger. A null result is normal — not every NPC needs
     * a script, not every object needs an interaction handler.
     *
     * The engine code that calls this is responsible for deciding what to
     * do when no script is found (e.g., show a default "Nothing interesting
     * happens." message).
     *
     * @param triggerType  The trigger type, e.g. "opnpc"
     * @param subject      The trigger subject, e.g. "Guard"
     * @return The compiled script, or null if none is registered.
     */
    public CompiledScript lookup(String triggerType, String subject) {
        return scripts.get(makeKey(triggerType, subject));
    }

    /**
     * Check if a trigger has a script registered, without retrieving it.
     */
    public boolean has(String triggerType, String subject) {
        return scripts.containsKey(makeKey(triggerType, subject));
    }

    // ── Utility ───────────────────────────────────────────────────────

    /** How many scripts are registered. Useful for init logging. */
    public int size() {
        return scripts.size();
    }

    /**
     * List all registered trigger keys. Useful for debugging and for
     * printing a summary at init time.
     */
    public void dump() {
        System.out.println("[TriggerRegistry] " + scripts.size() + " scripts registered:");
        for (Map.Entry<String, CompiledScript> entry : scripts.entrySet()) {
            CompiledScript cs = entry.getValue();
            System.out.println("  [" + cs.triggerType + "," + cs.subject + "] " +
                    cs.instructions.length + " instructions (from " + cs.sourceFile + ")");
        }
    }

    /**
     * Build the map key from type and subject.
     * Using a simple delimiter keeps the implementation trivial.
     */
    private String makeKey(String triggerType, String subject) {
        return triggerType + ":" + subject;
    }
}