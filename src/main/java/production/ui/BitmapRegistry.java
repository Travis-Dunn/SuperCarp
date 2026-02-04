package production.ui;

import production.sprite.SpritePalette;
import whitetail.utility.logging.LogLevel;

import static whitetail.utility.logging.Logger.LogSession;

/**
 * Static registry for UI bitmaps.
 * All bitmaps are loaded during Init() and accessed via public fields.
 */
public final class BitmapRegistry {
    private static boolean init;

    /* --- bitmap fields --- */
    public static Bitmap BILBO_PORTRAIT;
    // public static Bitmap CURSOR;
    // etc.

    private BitmapRegistry() {}

    public static boolean Init(SpritePalette palette) {
        assert(!init);

        LogSession(LogLevel.DEBUG, CLASS + " initializing...\n");

        /* load bitmaps */
        BILBO_PORTRAIT = BitmapFileParser.FromFile("bilbo.png", palette);
        // CURSOR = BitmapFileParser.FromFile("cursor.png", palette);

        LogSession(LogLevel.DEBUG, CLASS + " initialized.\n");

        return init = true;
    }

    public static void Shutdown() {
        assert(init);

        LogSession(LogLevel.DEBUG, CLASS + " shutting down...\n");

        /* null out references */
        BILBO_PORTRAIT = null;
        // CURSOR = null;

        init = false;

        LogSession(LogLevel.DEBUG, CLASS + " shutdown complete.\n");
    }

    public static boolean IsInitialized() {
        return init;
    }

    public static final String CLASS = BitmapRegistry.class.getSimpleName();
}