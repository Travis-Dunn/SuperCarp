package production.carpscript;

import java.util.Map;

/**
 * The public API for the scripting system.
 *
 * This is the ONE class your game engine needs to interact with. It ties
 * together the registry, VM, and loader behind a clean interface. Your
 * engine code should never need to touch ScriptVM, TriggerRegistry, or
 * the parser directly.
 *
 * SETUP (during game init):
 * ─────────────────────────
 *   ScriptRunner scripts = new ScriptRunner();
 *
 *   // Register engine commands that scripts can call
 *   scripts.registerCommand("mes", new MesCommand());
 *   scripts.registerCommand("p_delay", new PDelayCommand());
 *   // ... etc
 *
 *   // Load script files (classpath resources under "scripts/")
 *   scripts.loadScripts("npcs/guard.cs2", "npcs/shopkeeper.cs2",
 *                        "quests/cooks_quest.cs2", "login.cs2");
 *
 * RUNTIME (when game events occur):
 * ──────────────────────────────────
 *   // Player clicks "Talk-to" on an NPC:
 *   ScriptState state = scripts.fireTrigger("opnpc", npc.getName(), player.getVars());
 *   if (state != null) {
 *       player.setActiveScript(state);  // store for suspend/resume
 *   }
 *
 *   // In your game tick loop, for each player with an active script:
 *   if (player.getActiveScript() != null) {
 *       ScriptState s = player.getActiveScript();
 *       if (s.state == ExecutionState.SUSPENDED) {
 *           s.delayTicks--;
 *           if (s.delayTicks <= 0) {
 *               scripts.resumeScript(s, player.getVars());
 *           }
 *       }
 *       if (s.state == ExecutionState.FINISHED) {
 *           player.setActiveScript(null);
 *       }
 *   }
 *
 * That tick loop pseudocode is essentially the entire integration surface
 * between your engine and the scripting system.
 */
public class ScriptRunner {

    private final ScriptVM vm;
    private final TriggerRegistry registry;

    public ScriptRunner() {
        this.vm = new ScriptVM();
        this.registry = new TriggerRegistry();
    }

    // ══════════════════════════════════════════════════════════════════
    //  Setup (called during game init)
    // ══════════════════════════════════════════════════════════════════

    /**
     * Register an engine command that scripts can call.
     * Must be called BEFORE loadScripts() so that commands are available
     * at runtime. (The parser doesn't validate command names — unknown
     * commands only cause errors at execution time.)
     *
     * @param name     The command name as used in scripts (e.g., "mes")
     * @param handler  The Java implementation of the command
     */
    public void registerCommand(String name, CommandHandler handler) {
        vm.registerCommand(name, handler);
    }

    /**
     * Load and compile script files.
     *
     * Each filename is resolved as a classpath resource under the
     * "scripts" directory (e.g., "npcs/guard.cs2" loads from
     * /scripts/npcs/guard.cs2 on the classpath). Call this once during
     * game init, after registering commands.
     *
     * @param filenames  The script filenames relative to the scripts
     *                   directory
     */
    public void loadScripts(String... filenames) {
        ScriptLoader loader = new ScriptLoader(registry);
        loader.loadScripts(filenames);
    }

    // ══════════════════════════════════════════════════════════════════
    //  Runtime (called when game events occur)
    // ══════════════════════════════════════════════════════════════════

    /**
     * Fire a trigger: look up the script, create a fresh execution state,
     * and start running it.
     *
     * Call this when a game event occurs that might have a script handler.
     * For example, when the player clicks "Talk-to" on an NPC named "Guard":
     *
     *   ScriptState state = scripts.fireTrigger("opnpc", "Guard", playerVars);
     *
     * @param triggerType  The trigger type (e.g., "opnpc", "login")
     * @param subject      The trigger subject (e.g., "Guard", "on_login")
     * @param playerVars   The player's persistent variable map
     * @return The ScriptState if a script was found and started (may be
     *         FINISHED or SUSPENDED), or null if no script handles this
     *         trigger. If SUSPENDED, the caller must store this state and
     *         resume it later via resumeScript().
     */
    public ScriptState fireTrigger(String triggerType, String subject,
                                   Map<String, Object> playerVars) {
        CompiledScript script = registry.lookup(triggerType, subject);
        if (script == null) {
            return null; // no script for this trigger — not an error
        }

        ScriptState state = script.createState();
        vm.execute(state, playerVars);
        return state;
    }

    /**
     * Resume a suspended script.
     *
     * Call this when a script's delay has expired. The state should have
     * been saved from a previous fireTrigger() call.
     *
     * @param state       The suspended ScriptState to resume
     * @param playerVars  The player's persistent variable map (same one)
     */
    public void resumeDelayedScript(ScriptState state, Map<String, Object> playerVars) {
        if (state.state != ExecutionState.DELAYED) {
            return; // nothing to resume
        }
        state.state = ExecutionState.RUNNING;
        vm.execute(state, playerVars);
    }

    public void resumeSuspendedScript(ScriptState state, Map<String,
            Object> playerVars) {
        if (state.state != ExecutionState.SUSPENDED) {
            return; // nothing to resume
        }
        state.state = ExecutionState.RUNNING;
        vm.execute(state, playerVars);
    }

    // ══════════════════════════════════════════════════════════════════
    //  Utility
    // ══════════════════════════════════════════════════════════════════

    /**
     * Check if a trigger has a script registered.
     * Useful for the engine to decide behavior — for example, if an NPC
     * has no opnpc script, maybe show "Nothing interesting happens."
     */
    public boolean hasScript(String triggerType, String subject) {
        return registry.has(triggerType, subject);
    }

    /**
     * Get the underlying registry (for debugging/development tools).
     */
    public TriggerRegistry getRegistry() {
        return registry;
    }
}