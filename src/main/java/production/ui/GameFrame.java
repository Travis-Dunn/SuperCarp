package production.ui;

import production.display.DisplayConfig;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;

public class GameFrame {
    private static boolean init;
    private static Bitmap frame;

    private static int portraitXNpc;
    private static int portraitXPlayer;
    private static int portraitY;
    private static int centerXNpc;
    private static int headerY;
    private static int footerY;
    private static int textBoxMinY, textBoxMaxY;
    private static int textBoxMinX, textBoxMaxX;
    private static int op30Y, op31Y, op32Y;
    private static int op20Y, op21Y;
    private static int op1Y;

    public static boolean Init() {
        assert(!init);
        assert(BitmapRegistry.IsInitialized());

        int w = DisplayConfig.GetEmulatedW();
        int h = DisplayConfig.GetEmulatedH();

        /**
         * This is good enough for now, probably.
         * I am ok with just setting a bunch of values to constants here, but
         * the constants depend on font. If we change fonts, these will need to
         * be updated.
         */
        if (w == 384 && h == 216) {
            frame = BitmapRegistry.FRAME_0;
            portraitXNpc = 12;
            portraitXPlayer = 268;
            portraitY = 169;
            centerXNpc = 175;
            headerY = 159;
            footerY = 199;
            textBoxMinY = 161;
            textBoxMaxY = 211;
            textBoxMinX = 4;
            textBoxMaxX = 308;
            op30Y = 169;
            op31Y = 179;
            op32Y = 189;
            op20Y = 172;
            op21Y = 185;
            op1Y = op31Y;
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

    public static boolean InChatBox(int x, int y) {
        return x >= textBoxMinX && x <= textBoxMaxX &&
                y >= textBoxMinY && y < textBoxMaxY;
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

    public static int GetTextBoxMinY() { assert(init); return textBoxMinY; }
    public static int GetTextBoxMaxY() { assert(init); return textBoxMaxY; }
    public static int GetTextBoxMinX() { assert(init); return textBoxMinX; }
    public static int GetTextBoxMaxX() { assert(init); return textBoxMaxX; }

    public static int GetCenterXNpc() { assert(init); return centerXNpc; }
    public static int GetHeaderY() { assert(init); return headerY; }
    public static int GetFooterY() { assert(init); return footerY; }
    public static int GetOp30Y() { assert(init); return op30Y; }
    public static int GetOp31Y() { assert(init); return op31Y; }
    public static int GetOp32Y() { assert(init); return op32Y; }
    public static int GetOp20Y() { assert(init); return op20Y; }
    public static int GetOp21Y() { assert(init); return op21Y; }
    public static int GetOp1Y() { assert(init); return op1Y; }


    public static final String CLASS = GameFrame.class.getSimpleName();
    private static String ErrStrUnsupportedRes(int w, int h) {
        return String.format("%s failed to initialize because the emulated " +
                "width [%d] and emulated height [%d] were unrecognized.\n",
                CLASS, w, h);
    }
}
