package production.ui;

import production.sprite.SpritePalette;
import whitetail.utility.logging.LogLevel;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.Logger.LogSession;

/**
 * Immediate-mode UI rendering utilities.
 * All methods draw directly to the framebuffer - nothing persists between frames.
 */
public final class UIRenderer {
    private static boolean init;

    private static int bpp;

    private UIRenderer() {}

    public static boolean Init(int bpp) {
        assert(!init);

        LogSession(LogLevel.DEBUG, CLASS + " initializing...\n");

        if (bpp < 1) {
            LogFatalAndExit(ErrStrFailedInitBppTooSmall(bpp));
            return init = false;
        } else {
            UIRenderer.bpp = bpp;
        }

        LogSession(LogLevel.DEBUG, ErrStrInit());

        return init = true;
    }

    /**
     * Draw a filled rectangle.
     *
     * @param fb framebuffer (RGBA byte array)
     * @param fbW framebuffer width
     * @param fbH framebuffer height
     * @param x left edge
     * @param y top edge
     * @param w width
     * @param h height
     * @param paletteIndex color index (0-15)
     * @param palette the palette to use
     */
    public static void DrawRect(byte[] fb, int fbW, int fbH,
                                int x, int y, int w, int h,
                                int paletteIndex, SpritePalette palette) {
        /* bounds check palette index */
        if (paletteIndex < 0 || paletteIndex > palette.maxIdx) return;

        int color = palette.colors[paletteIndex];
        _DrawRect(fb, fbW, fbH, x, y, w, h, color);
    }

    /**
     * Draw a filled rectangle with raw ARGB color.
     */
    private static void _DrawRect(byte[] fb, int fbW, int fbH,
                                  int x, int y, int w, int h,
                                  int argbColor) {
        /* early rejection */
        if (w <= 0 || h <= 0) return;
        if (x + w <= 0 || x >= fbW) return;
        if (y + h <= 0 || y >= fbH) return;

        /* clip to screen */
        int x0 = Math.max(x, 0);
        int y0 = Math.max(y, 0);
        int x1 = Math.min(x + w, fbW);
        int y1 = Math.min(y + h, fbH);

        byte r = (byte)((argbColor >> 16) & 0xFF);
        byte g = (byte)((argbColor >> 8) & 0xFF);
        byte b = (byte)(argbColor & 0xFF);
        byte a = (byte)((argbColor >> 24) & 0xFF);

        for (int py = y0; py < y1; py++) {
            int rowOffset = py * fbW * bpp;
            for (int px = x0; px < x1; px++) {
                int idx = rowOffset + px * bpp;
                fb[idx]     = r;
                fb[idx + 1] = g;
                fb[idx + 2] = b;
                fb[idx + 3] = a;
            }
        }
    }

    /**
     * Draw a rectangle outline (1px border).
     */
    public static void DrawRectOutline(byte[] fb, int fbW, int fbH,
                                       int x, int y, int w, int h,
                                       int paletteIndex, SpritePalette palette) {
        if (paletteIndex < 0 || paletteIndex > palette.maxIdx) return;

        int color = palette.colors[paletteIndex];
        _DrawRectOutline(fb, fbW, fbH, x, y, w, h, color);
    }

    /**
     * Draw a rectangle outline with raw ARGB color.
     */
    private static void _DrawRectOutline(byte[] fb, int fbW, int fbH,
                                        int x, int y, int w, int h,
                                        int argbColor) {
        if (w <= 0 || h <= 0) return;

        /* top edge */
        _DrawRect(fb, fbW, fbH, x, y, w, 1, argbColor);
        /* bottom edge */
        _DrawRect(fb, fbW, fbH, x, y + h - 1, w, 1, argbColor);
        /* left edge */
        _DrawRect(fb, fbW, fbH, x, y + 1, 1, h - 2, argbColor);
        /* right edge */
        _DrawRect(fb, fbW, fbH, x + w - 1, y + 1, 1, h - 2, argbColor);
    }

    /**
     * Draw a filled rectangle with a 1px border.
     */
    public static void DrawRectWithBorder(byte[] fb, int fbW, int fbH,
                                          int x, int y, int w, int h,
                                          int fillIndex, int borderIndex,
                                          SpritePalette palette) {
        DrawRect(fb, fbW, fbH, x, y, w, h, fillIndex, palette);
        DrawRectOutline(fb, fbW, fbH, x, y, w, h, borderIndex, palette);
    }

    /**
     * Check if a point is inside a rectangle.
     * Useful for mouse hit testing.
     */
    public static boolean pointInRect(int px, int py,
                                      int rx, int ry, int rw, int rh) {
        return px >= rx && px < rx + rw && py >= ry && py < ry + rh;
    }

    public static final String CLASS = UIRenderer.class.getSimpleName();
    private static String ErrStrFailedInitBppTooSmall(int bpp) {
        return String.format("%s failed to initialize because bpp [%d] must " +
                "be at least 1", CLASS, bpp);
    }
    private static String ErrStrInit() {
        return String.format("%s initialized with [%d] bytes per pixel.\n",
                CLASS, bpp);
    }
}