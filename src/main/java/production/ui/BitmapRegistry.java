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

    public static Bitmap BILBO_PORTRAIT;
    public static Bitmap FRAME_TL;
    public static Bitmap FRAME_TR;
    public static Bitmap FRAME_BL;
    public static Bitmap FRAME_BR;
    public static Bitmap FRAME_L;
    public static Bitmap FRAME_R;
    public static Bitmap FRAME_T;
    public static Bitmap FRAME_B;

    private BitmapRegistry() {}

    public static boolean Init(SpritePalette palette) {
        assert(!init);

        LogSession(LogLevel.DEBUG, CLASS + " initializing...\n");

        BILBO_PORTRAIT = BitmapFileParser.FromFile("bilbo.png", palette);
        FRAME_TL = BitmapFileParser.FromFile("frame_tl.png", palette);
        FRAME_TR = BitmapFileParser.FromFile("frame_tr.png", palette);
        FRAME_BL = BitmapFileParser.FromFile("frame_bl.png", palette);
        FRAME_BR = BitmapFileParser.FromFile("frame_br.png", palette);
        FRAME_L = BitmapFileParser.FromFile("frame_l.png", palette);
        FRAME_R = BitmapFileParser.FromFile("frame_r.png", palette);
        FRAME_T = BitmapFileParser.FromFile("frame_t.png", palette);
        FRAME_B = BitmapFileParser.FromFile("frame_b.png", palette);

        LogSession(LogLevel.DEBUG, CLASS + " initialized.\n");

        return init = true;
    }

    public static void Shutdown() {
        assert(init);

        LogSession(LogLevel.DEBUG, CLASS + " shutting down...\n");

        BILBO_PORTRAIT = null;
        FRAME_TL = null;
        FRAME_TR = null;
        FRAME_BL = null;
        FRAME_BR = null;
        FRAME_L = null;
        FRAME_R = null;
        FRAME_T = null;
        FRAME_B = null;

        init = false;

        LogSession(LogLevel.DEBUG, CLASS + " shutdown complete.\n");
    }

    public static boolean IsInitialized() {
        return init;
    }

    public static final String CLASS = BitmapRegistry.class.getSimpleName();
}