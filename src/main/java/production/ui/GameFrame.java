package production.ui;

import production.display.DisplayConfig;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;

public class GameFrame {
    private static boolean init;
    private static Bitmap frame;

    public static int PORTRAIT_X_NPC;
    public static int PORTRAIT_X_PLAYER;
    public static int PORTRAIT_Y;

    public static boolean Init() {
        assert(!init);
        assert(BitmapRegistry.IsInitialized());

        int w = DisplayConfig.GetEmulatedW();
        int h = DisplayConfig.GetEmulatedH();

        if (w == 384 && h == 216) {
            frame = BitmapRegistry.FRAME_0;
            PORTRAIT_X_NPC = 12;
            PORTRAIT_X_PLAYER = 268;
            PORTRAIT_Y = 169;
        } else {
            LogFatalAndExit(ErrStrUnsupportedRes(w, h));
            return init = false;
        }

        return init = true;
    }

    public static void Draw() {
        assert(init);

        Renderer.DrawBitmap(frame, 0, 0);
    }

    public static final String CLASS = GameFrame.class.getSimpleName();
    private static String ErrStrUnsupportedRes(int w, int h) {
        return String.format("%s failed to initialize because the emulated " +
                "width [%d] and emulated height [%d] were unrecognized.\n",
                CLASS, w, h);
    }
}
