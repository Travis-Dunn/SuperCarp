package production;

/**
 * Platform detection utility.
 * Detects once at startup, caches result.
 */
public final class Platform {
    public static final int WINDOWS = 0;
    public static final int LINUX = 1;
    public static final int MAC = 2;
    public static final int UNKNOWN = -1;

    private static int detected = -2;  // -2 = not yet detected

    private Platform() {}

    public static int get() {
        if (detected == -2) {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                detected = WINDOWS;
            } else if (os.contains("nux") || os.contains("nix")) {
                detected = LINUX;
            } else if (os.contains("mac")) {
                detected = MAC;
            } else {
                detected = UNKNOWN;
            }
        }
        return detected;
    }

    public static boolean isWindows() { return get() == WINDOWS; }
    public static boolean isLinux()   { return get() == LINUX; }
    public static boolean isMac()     { return get() == MAC; }

    public static String getName() {
        switch (get()) {
            case WINDOWS: return "Windows";
            case LINUX:   return "Linux";
            case MAC:     return "macOS";
            default:      return "Unknown";
        }
    }
}