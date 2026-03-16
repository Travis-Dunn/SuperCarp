package whitetail.loaders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;

/**
 * Centralized resolver for game asset streams.
 *
 * <p>Provides a single point of control for how assets are located and
 * opened. Supports two sources, checked in order:</p>
 *
 * <ol>
 *   <li><b>Filesystem</b> — an optional external base directory, intended
 *       for mod support or development use. If configured and the requested
 *       file exists there, a {@code FileInputStream} is returned.</li>
 *   <li><b>Classpath</b> — the jar-with-dependencies (or equivalent).
 *       Always available as a fallback.</li>
 * </ol>
 *
 * <p>The resolver is initialized once at startup via {@link #Init(File)}.
 * If no external directory is needed, call {@link #Init()} to use
 * classpath-only mode. All {@code -FileParser} classes should obtain
 * their streams through {@link #Open(String, String)} rather than
 * resolving paths themselves.</p>
 *
 * <p><b>Security:</b> filenames containing path separators or components
 * such as {@code ".."} are rejected to prevent directory traversal.</p>
 *
 * <p><b>Compatibility:</b> Java 5+, all platforms.</p>
 */
public final class AssetStreamResolver {

    private static File externalBaseDir = null;
    private static boolean initialized = false;

    private AssetStreamResolver() { }

    // -----------------------------------------------------------------
    //  Initialization
    // -----------------------------------------------------------------

    /**
     * Initializes the resolver in classpath-only mode.
     * Equivalent to {@code Init(null)}.
     */
    public static void Init() {
        Init(null);
    }

    /**
     * Initializes the resolver with an optional external base directory.
     *
     * <p>If {@code baseDir} is non-null it must be an existing, readable
     * directory. The path is immediately resolved to its canonical form
     * so that all subsequent lookups use a stable, absolute reference.</p>
     *
     * @param baseDir the external asset root, or {@code null} for
     *                classpath-only mode
     */
    public static void Init(File baseDir) {
        if (initialized) {
            LogFatalAndExit(ErrStrAlreadyInit());
            return;
        }

        if (baseDir != null) {
            if (!baseDir.isDirectory()) {
                LogFatalAndExit(ErrStrNotADir(baseDir));
                return;
            }
            if (!baseDir.canRead()) {
                LogFatalAndExit(ErrStrNotReadable(baseDir));
                return;
            }
            try {
                externalBaseDir = baseDir.getCanonicalFile();
            } catch (IOException e) {
                LogFatalAndExit(ErrStrCanonFailed(baseDir));
                return;
            }
        } else {
            externalBaseDir = null;
        }

        initialized = true;
    }

    // -----------------------------------------------------------------
    //  Public API
    // -----------------------------------------------------------------

    /**
     * Opens an asset stream for the given subdirectory and filename.
     *
     * <p>If an external base directory is configured and the file exists
     * there, a {@code FileInputStream} is returned. Otherwise the
     * classpath is searched. Returns {@code null} if the asset is not
     * found in either location.</p>
     *
     * <p>The caller is responsible for closing the returned stream.</p>
     *
     * @param subdir   the asset subdirectory (e.g. {@code "palettes"},
     *                 {@code "sprites"}). Must not be null or empty.
     * @param filename the asset filename (e.g. {@code "default.png"}).
     *                 Must not be null or empty, and must not contain
     *                 path separators or {@code ".."}.
     * @return an open {@code InputStream}, or {@code null} if the asset
     *         was not found
     */
    public static InputStream Open(String subdir, String filename) {
        if (!initialized) {
            LogFatalAndExit(ErrStrNotInit());
            return null;
        }

        if (subdir == null || subdir.length() == 0) {
            LogFatalAndExit(ErrStrBadSubdir(subdir));
            return null;
        }

        if (filename == null || filename.length() == 0) {
            LogFatalAndExit(ErrStrBadFilename(filename));
            return null;
        }

        if (!IsSafeFilename(filename)) {
            LogFatalAndExit(ErrStrUnsafeFilename(filename));
            return null;
        }

        InputStream stream = OpenFromFilesystem(subdir, filename);

        if (stream == null) {
            stream = OpenFromClasspath(subdir, filename);
        }

        return stream;
    }

    // -----------------------------------------------------------------
    //  Resolution strategies
    // -----------------------------------------------------------------

    private static InputStream OpenFromFilesystem(String subdir,
                                                  String filename) {
        if (externalBaseDir == null) {
            return null;
        }

        File dir  = new File(externalBaseDir, subdir);
        File file = new File(dir, filename);

        if (!file.isFile() || !file.canRead()) {
            return null;
        }

        /*
         * Canonical-path check: after resolving symlinks and relative
         * components, the final path must still reside under the
         * external base directory. This is a safety net against any
         * traversal that slips past the filename validation.
         */
        try {
            String canonical = file.getCanonicalPath();
            String basePath  = externalBaseDir.getCanonicalPath()
                    + File.separator;
            if (!canonical.startsWith(basePath)) {
                LogFatalAndExit(ErrStrTraversal(filename, canonical));
                return null;
            }
        } catch (IOException e) {
            return null;
        }

        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            /* Race between exists-check and open; treat as absent. */
            return null;
        }
    }

    private static InputStream OpenFromClasspath(String subdir,
                                                 String filename) {
        String path = "/" + subdir + "/" + filename;
        return AssetStreamResolver.class.getResourceAsStream(path);
    }

    // -----------------------------------------------------------------
    //  Filename safety
    // -----------------------------------------------------------------

    /**
     * Returns {@code true} if the filename is safe to use in a path.
     * Rejects any name that contains a forward slash, backslash, or
     * the exact components {@code "."} or {@code ".."}.
     */
    private static boolean IsSafeFilename(String filename) {
        if (filename.indexOf('/') >= 0 || filename.indexOf('\\') >= 0) {
            return false;
        }
        if (filename.equals(".") || filename.equals("..")) {
            return false;
        }
        return true;
    }

    // -----------------------------------------------------------------
    //  Error messages
    // -----------------------------------------------------------------

    private static final String CLASS =
            AssetStreamResolver.class.getSimpleName();

    private static String ErrStrAlreadyInit() {
        return String.format("%s has already been initialized.\n", CLASS);
    }

    private static String ErrStrNotInit() {
        return String.format("%s has not been initialized. " +
                "Call Init() before opening assets.\n", CLASS);
    }

    private static String ErrStrNotADir(File f) {
        return String.format("%s rejected base directory [%s]. " +
                "Path is not an existing directory.\n", CLASS, f);
    }

    private static String ErrStrNotReadable(File f) {
        return String.format("%s rejected base directory [%s]. " +
                "Directory is not readable.\n", CLASS, f);
    }

    private static String ErrStrCanonFailed(File f) {
        return String.format("%s failed to resolve canonical path " +
                "for [%s].\n", CLASS, f);
    }

    private static String ErrStrBadSubdir(String subdir) {
        return String.format("%s received null or empty " +
                "subdirectory.\n", CLASS);
    }

    private static String ErrStrBadFilename(String filename) {
        return String.format("%s received null or empty " +
                "filename.\n", CLASS);
    }

    private static String ErrStrUnsafeFilename(String filename) {
        return String.format("%s rejected filename [%s]. " +
                "Filenames must not contain path separators or " +
                "relative components.\n", CLASS, filename);
    }

    private static String ErrStrTraversal(String filename,
                                          String resolved) {
        return String.format("%s blocked directory traversal. " +
                "Filename [%s] resolved to [%s], which is outside " +
                "the asset base directory.\n", CLASS, filename, resolved);
    }
}