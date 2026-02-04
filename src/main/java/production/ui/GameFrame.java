package production.ui;

import production.DisplayConfig;
import production.sprite.SpritePalette;
import whitetail.utility.logging.LogLevel;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.ErrorStrings.ERR_STR_FAILED_INIT_OOM;
import static whitetail.utility.logging.Logger.LogSession;

/**
 * Builds and draws the static frame around the game window.
 * Call Init() once during startup, then Draw() each frame.
 */
public final class GameFrame {
    private static boolean init;

    private static final int CORNER_SIZE = 32;

    /* positions computed at init */
    private static int screenW;
    private static int screenH;
    private static int edgeW;   /* top/bottom edge width */
    private static int edgeH;   /* left/right edge height */

    private GameFrame() {}

    public static boolean Init() {
        assert(!init);

        LogSession(LogLevel.DEBUG, CLASS + " initializing...\n");

        screenW = DisplayConfig.GetEmulatedW();
        screenH = DisplayConfig.GetEmulatedH();
        edgeW = screenW - (CORNER_SIZE * 2);
        edgeH = screenH - (CORNER_SIZE * 2);

        if (edgeW <= 0 || edgeH <= 0) {
            LogFatalAndExit(ErrStrScreenTooSmall(screenW, screenH));
            return init = false;
        }

        /* expand horizontal edges (top and bottom) */
        BitmapRegistry.FRAME_T = expandHorizontal(BitmapRegistry.FRAME_T, edgeW);
        if (BitmapRegistry.FRAME_T == null) return init = false;

        BitmapRegistry.FRAME_B = expandHorizontal(BitmapRegistry.FRAME_B, edgeW);
        if (BitmapRegistry.FRAME_B == null) return init = false;

        /* expand vertical edges (left and right) */
        BitmapRegistry.FRAME_L = expandVertical(BitmapRegistry.FRAME_L, edgeH);
        if (BitmapRegistry.FRAME_L == null) return init = false;

        BitmapRegistry.FRAME_R = expandVertical(BitmapRegistry.FRAME_R, edgeH);
        if (BitmapRegistry.FRAME_R == null) return init = false;

        LogSession(LogLevel.DEBUG, CLASS + " initialized with screen [" +
                screenW + "x" + screenH + "], edges [" + edgeW + "x" + edgeH +
                "].\n");

        return init = true;
    }

    /**
     * Expand a 1-wide bitmap horizontally by repeating it.
     */
    private static Bitmap expandHorizontal(Bitmap src, int targetW) {
        assert(src.width == 1);

        int h = src.height;
        byte[] srcData = src.data;
        byte[] newData;

        try {
            newData = new byte[targetW * h];
        } catch (OutOfMemoryError e) {
            LogFatalAndExit(CLASS + ERR_STR_FAILED_INIT_OOM);
            return null;
        }

        for (int y = 0; y < h; ++y) {
            byte pixel = srcData[y];
            int rowOffset = y * targetW;
            for (int x = 0; x < targetW; ++x) {
                newData[rowOffset + x] = pixel;
            }
        }

        return new Bitmap(targetW, h, newData, src.getPalette());
    }

    /**
     * Expand a 1-tall bitmap vertically by repeating it.
     */
    private static Bitmap expandVertical(Bitmap src, int targetH) {
        assert(src.height == 1);

        int w = src.width;
        byte[] srcData = src.data;
        byte[] newData;

        try {
            newData = new byte[w * targetH];
        } catch (OutOfMemoryError e) {
            LogFatalAndExit(CLASS + ERR_STR_FAILED_INIT_OOM);
            return null;
        }

        for (int y = 0; y < targetH; ++y) {
            int rowOffset = y * w;
            System.arraycopy(srcData, 0, newData, rowOffset, w);
        }

        return new Bitmap(w, targetH, newData, src.getPalette());
    }

    /**
     * Draw the frame. Call during render, after sprites.
     */
    public static void Draw() {
        assert(init);

        /* corners */
        Renderer.DrawBitmap(BitmapRegistry.FRAME_TL, 0, 0);
        Renderer.DrawBitmap(BitmapRegistry.FRAME_TR, screenW - CORNER_SIZE, 0);
        Renderer.DrawBitmap(BitmapRegistry.FRAME_BL, 0, screenH - CORNER_SIZE);
        Renderer.DrawBitmap(BitmapRegistry.FRAME_BR,
                screenW - CORNER_SIZE, screenH - CORNER_SIZE);

        /* edges */
        Renderer.DrawBitmap(BitmapRegistry.FRAME_T, CORNER_SIZE, 0);
        Renderer.DrawBitmap(BitmapRegistry.FRAME_B, CORNER_SIZE,
                screenH - CORNER_SIZE);
        Renderer.DrawBitmap(BitmapRegistry.FRAME_L, 0, CORNER_SIZE);
        Renderer.DrawBitmap(BitmapRegistry.FRAME_R, screenW - CORNER_SIZE,
                CORNER_SIZE);
    }

    public static void Shutdown() {
        assert(init);

        LogSession(LogLevel.DEBUG, CLASS + " shutting down...\n");

        /* note: expanded bitmaps replaced registry refs, so clearing
           the registry handles cleanup */

        init = false;

        LogSession(LogLevel.DEBUG, CLASS + " shutdown complete.\n");
    }

    public static boolean IsInitialized() {
        return init;
    }

    public static final String CLASS = GameFrame.class.getSimpleName();

    private static String ErrStrScreenTooSmall(int w, int h) {
        return String.format("%s failed to initialize. Screen [%dx%d] is too " +
                        "small for frame corners [%dx%d].\n", CLASS, w, h,
                CORNER_SIZE * 2, CORNER_SIZE * 2);
    }
}