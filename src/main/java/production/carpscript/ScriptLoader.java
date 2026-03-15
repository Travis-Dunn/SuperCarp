package production.carpscript;

import java.util.List;

/**
 * Loads and compiles script files at game init time.
 *
 * USAGE:
 * ──────
 * During game startup, before anything else happens:
 *
 *   TriggerRegistry registry = new TriggerRegistry();
 *   ScriptLoader loader = new ScriptLoader(registry);
 *   loader.loadScripts("npcs/guard.cs2", "npcs/shopkeeper.cs2",
 *                       "quests/cooks_quest.cs2", "login.cs2");
 *   // registry is now populated and ready for runtime lookups
 *
 * FILE ORGANIZATION:
 * ──────────────────
 * Script files use the .cs2 extension (matching RuneScript convention)
 * and are loaded as classpath resources from the "scripts" directory.
 * You can organize them however you like:
 *
 *   scripts/
 *     npcs/
 *       guard.cs2         ← contains [opnpc,Guard]
 *       shopkeeper.cs2    ← contains [opnpc,Shopkeeper]
 *     quests/
 *       cooks_quest.cs2   ← contains multiple triggers for the quest
 *     login.cs2           ← contains [login,on_login]
 *
 * A single .cs2 file can contain multiple trigger blocks — the parser
 * handles that and returns a CompiledScript for each one.
 *
 * Individual file parsing is handled by ScriptFileParser, which follows
 * the same classpath-resource loading pattern used by the other
 * FileParser classes in the project.
 */
public class ScriptLoader {

    private final TriggerRegistry registry;

    /** Count of successfully loaded scripts (for init logging). */
    private int loadedCount;

    public ScriptLoader(TriggerRegistry registry) {
        this.registry = registry;
        this.loadedCount = 0;
    }

    /**
     * Load and compile a set of script files by filename.
     *
     * Each filename is resolved as a classpath resource under the
     * "scripts" directory by ScriptFileParser. Call this once during
     * game init, after registering commands.
     *
     * @param filenames  The script filenames relative to the scripts
     *                   directory (e.g., "npcs/guard.cs2", "login.cs2")
     */
    public void loadScripts(String... filenames) {
        long startTime = System.currentTimeMillis();
        System.out.println("[ScriptLoader] Loading scripts...");

        loadedCount = 0;

        for (String filename : filenames) {
            List<CompiledScript> scripts = ScriptFileParser.FromFile(filename);
            if (scripts == null) {
                continue;
            }
            for (CompiledScript script : scripts) {
                registry.register(script);
                loadedCount++;
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("[ScriptLoader] Done. Loaded " + loadedCount +
                " trigger(s) from " + filenames.length + " file(s) in " +
                elapsed + "ms.");
    }
}