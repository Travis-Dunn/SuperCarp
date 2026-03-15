package production.ui;

import production.character.CharRegistry;
import production.sprite.SpritePalette;
import whitetail.utility.logging.LogLevel;

import static whitetail.utility.logging.Logger.LogSession;

/**
 * Static registry for UI bitmaps.
 * All bitmaps are loaded during Init() and accessed via public fields.
 */
public final class BitmapRegistry {
    private static boolean init;

    public static Bitmap MISSING_PORTRAIT;
    public static Bitmap BILBO_PORTRAIT;

    public static Bitmap FRAME_0;

    private BitmapRegistry() {}

    public static boolean Init(SpritePalette palette) {
        assert(!init);

        LogSession(LogLevel.DEBUG, CLASS + " initializing...\n");

        BILBO_PORTRAIT = BitmapFileParser.FromFile("bilbo.png", palette);
        CharRegistry.BILBO.setPortrait(BILBO_PORTRAIT);

        MISSING_PORTRAIT = BitmapFileParser.FromFile(
                "missing_portrait.png", palette);
        CharRegistry.MISSING_CHAR.setPortrait(MISSING_PORTRAIT);

        FRAME_0 = BitmapFileParser.FromFile("frame_0.png", palette);

        LogSession(LogLevel.DEBUG, CLASS + " initialized.\n");

        return init = true;
    }

    public static void Shutdown() {
        assert(init);

        LogSession(LogLevel.DEBUG, CLASS + " shutting down...\n");

        BILBO_PORTRAIT = null;

        FRAME_0 = null;

        init = false;

        LogSession(LogLevel.DEBUG, CLASS + " shutdown complete.\n");
    }

    public static boolean IsInitialized() {
        return init;
    }

    public static final String CLASS = BitmapRegistry.class.getSimpleName();
}