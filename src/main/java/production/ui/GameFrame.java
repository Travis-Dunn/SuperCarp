package production.ui;

import production.display.DisplayConfig;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;

public class GameFrame {
    private static boolean init;
    private static Bitmap frame;

    private static int portraitXNpc;
    private static int portraitXPlayer;
    private static int portraitY;

    public static boolean Init() {
        assert(!init);
        assert(BitmapRegistry.IsInitialized());

        int w = DisplayConfig.GetEmulatedW();
        int h = DisplayConfig.GetEmulatedH();

        if (w == 384 && h == 216) {
            frame = BitmapRegistry.FRAME_0;
            portraitXNpc = 12;
            portraitXPlayer = 268;
            portraitY = 169;
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

    public static int GetPortraitXNpc() {
        assert(init);

        return portraitXNpc;
    }

    public static int GetPortraitXPlayer() {
        assert(init);

        return portraitXPlayer;
    }

    public static int GetPortraitY() {
        assert(init);

        return portraitY;
    }

    public static final String CLASS = GameFrame.class.getSimpleName();
    private static String ErrStrUnsupportedRes(int w, int h) {
        return String.format("%s failed to initialize because the emulated " +
                "width [%d] and emulated height [%d] were unrecognized.\n",
                CLASS, w, h);
    }
}
