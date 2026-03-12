package production.carpscript;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Loads and compiles script files from a directory at game init time.
 *
 * USAGE:
 * ──────
 * During game startup, before anything else happens:
 *
 *   TriggerRegistry registry = new TriggerRegistry();
 *   ScriptLoader loader = new ScriptLoader(registry);
 *   loader.loadDirectory(new File("data/scripts"));
 *   // registry is now populated and ready for runtime lookups
 *
 * FILE ORGANIZATION:
 * ──────────────────
 * Script files use the .cs2 extension (matching RuneScript convention).
 * You can organize them however you like:
 *
 *   data/scripts/
 *     npcs/
 *       guard.cs2         ← contains [opnpc,Guard]
 *       shopkeeper.cs2    ← contains [opnpc,Shopkeeper]
 *     quests/
 *       cooks_quest.cs2   ← contains multiple triggers for the quest
 *     login.cs2           ← contains [login,on_login]
 *
 * The loader recursively scans all subdirectories, so structure is up to
 * you. A single .cs2 file can contain multiple trigger blocks — the parser
 * handles that and returns a CompiledScript for each one.
 *
 * ERROR HANDLING:
 * ───────────────
 * If a script file has a syntax error, the loader logs the error with the
 * filename and line number, then continues loading other files. One broken
 * script shouldn't prevent the entire game from starting. During development,
 * you'll want to check the console output for these errors.
 */
public class ScriptLoader {

    private final TriggerRegistry registry;

    /** Count of successfully loaded scripts (for init logging). */
    private int loadedCount;

    /** Count of files that had errors (for init logging). */
    private int errorCount;

    public ScriptLoader(TriggerRegistry registry) {
        this.registry = registry;
        this.loadedCount = 0;
        this.errorCount = 0;
    }

    /**
     * Recursively load all .cs2 files from a directory.
     *
     * Call this once at game init. It compiles every script file and
     * registers all trigger blocks in the registry.
     *
     * @param directory  The root scripts directory (e.g., "data/scripts")
     */
    public void loadDirectory(File directory) {
        if (!directory.exists()) {
            System.err.println("[ScriptLoader] Scripts directory not found: " +
                    directory.getAbsolutePath());
            return;
        }

        if (!directory.isDirectory()) {
            System.err.println("[ScriptLoader] Not a directory: " +
                    directory.getAbsolutePath());
            return;
        }

        long startTime = System.currentTimeMillis();
        System.out.println("[ScriptLoader] Loading scripts from: " +
                directory.getAbsolutePath());

        loadedCount = 0;
        errorCount = 0;
        scanDirectory(directory);

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("[ScriptLoader] Done. Loaded " + loadedCount +
                " trigger(s) with " + errorCount + " error(s) in " + elapsed + "ms.");
    }

    /**
     * Load a single script file. Can also be called directly if you want
     * to load one file outside the normal directory scan (useful for
     * development/testing).
     */
    public void loadFile(File file) {
        String source;
        try {
            source = readFile(file);
        } catch (IOException e) {
            System.err.println("[ScriptLoader] Failed to read " +
                    file.getPath() + ": " + e.getMessage());
            errorCount++;
            return;
        }

        // Skip empty files
        if (source.trim().isEmpty()) {
            return;
        }

        try {
            // Lex → Parse → Register
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();

            Parser parser = new Parser(tokens, file.getPath());
            List<CompiledScript> scripts = parser.parseFile();

            for (CompiledScript script : scripts) {
                registry.register(script);
                loadedCount++;
            }
        } catch (Lexer.LexerException e) {
            System.err.println("[ScriptLoader] " + file.getPath() + ": " + e.getMessage());
            errorCount++;
        } catch (Parser.ParseException e) {
            System.err.println("[ScriptLoader] " + file.getPath() + ": " + e.getMessage());
            errorCount++;
        } catch (Exception e) {
            System.err.println("[ScriptLoader] Unexpected error in " +
                    file.getPath() + ": " + e.getMessage());
            errorCount++;
        }
    }

    // ── Internals ─────────────────────────────────────────────────────

    /**
     * Recursively scan a directory for .cs2 files.
     */
    private void scanDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file);
            } else if (file.getName().endsWith(".cs2")) {
                loadFile(file);
            }
        }
    }

    /**
     * Read a file's entire contents into a String.
     *
     * Plain Java 7 compatible — no Files.readAllBytes() or try-with-resources
     * with multiple resources.
     */
    private String readFile(File file) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            return sb.toString();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore close errors
                }
            }
        }
    }
}