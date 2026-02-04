package production.ui;

import production.sprite.SpritePalette;
import whitetail.utility.logging.LogLevel;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.Logger.LogSession;

/**
 * Immediate-mode UI rendering utilities.
 * All methods draw directly to the framebuffer - nothing persists between frames.
 */
public final class Renderer {
    private static boolean init;

    private static int bpp;
    private static byte buf[];
    private static int fbW, fbH;

    private Renderer() {}

    public static boolean Init(int bpp, byte buf[], int fbW, int fbH) {
        assert(!init);

        LogSession(LogLevel.DEBUG, CLASS + " initializing...\n");

        if (bpp < 1) {
            LogFatalAndExit(ErrStrFailedInitValTooSmall(
                    "bytes per pixel", bpp));
            return init = false;
        } else {
            Renderer.bpp = bpp;
        }

        if (fbW < 1) {
            LogFatalAndExit(ErrStrFailedInitValTooSmall(
                    "framebuffer width", fbW));
            return init = false;
        } else {
            Renderer.fbW = fbW;
        }

        if (fbH < 1) {
            LogFatalAndExit(ErrStrFailedInitValTooSmall(
                    "framebuffer height", fbH));
            return init = false;
        } else {
            Renderer.fbH = fbH;
        }

        if (buf == null) {
            LogFatalAndExit(ErrStrFailedInitBufNull());
            return init = false;
        } else {
            Renderer.buf = buf;
        }

        LogSession(LogLevel.DEBUG, ErrStrInit());

        return init = true;
    }

    /**
     * Draw a filled rectangle.
     *
     * @param x left edge
     * @param y top edge
     * @param w width
     * @param h height
     * @param paletteIndex color index (0-15)
     * @param palette the palette to use
     */
    public static void DrawRect(int x, int y, int w, int h,
                                int paletteIndex, SpritePalette palette) {
        /* bounds check palette index */
        if (paletteIndex < 0 || paletteIndex > palette.maxIdx) return;

        int color = palette.colors[paletteIndex];
        _DrawRect(x, y, w, h, color);
    }

    /**
     * Draw a filled rectangle with raw ARGB color.
     */
    private static void _DrawRect(int x, int y, int w, int h,
                                  int argbColor) {
        /* early rejection */
        if (w <= 0 || h <= 0) return;
        if (x + w <= 0 || x >= Renderer.fbW) return;
        if (y + h <= 0 || y >= Renderer.fbH) return;

        /* clip to screen */
        int x0 = Math.max(x, 0);
        int y0 = Math.max(y, 0);
        int x1 = Math.min(x + w, Renderer.fbW);
        int y1 = Math.min(y + h, Renderer.fbH);

        byte r = (byte)((argbColor >> 16) & 0xFF);
        byte g = (byte)((argbColor >> 8) & 0xFF);
        byte b = (byte)(argbColor & 0xFF);
        byte a = (byte)((argbColor >> 24) & 0xFF);

        for (int py = y0; py < y1; py++) {
            int rowOffset = py * Renderer.fbW * bpp;
            for (int px = x0; px < x1; px++) {
                int idx = rowOffset + px * bpp;
                buf[idx]     = r;
                buf[idx + 1] = g;
                buf[idx + 2] = b;
                buf[idx + 3] = a;
            }
        }
    }

    /**
     * Draw a rectangle outline (1px border).
     */
    public static void DrawRectOutline(int x, int y, int w, int h,
                                       int paletteIndex, SpritePalette palette) {
        if (paletteIndex < 0 || paletteIndex > palette.maxIdx) return;

        int color = palette.colors[paletteIndex];
        _DrawRectOutline(x, y, w, h, color);
    }

    /**
     * Draw a rectangle outline with raw ARGB color.
     */
    private static void _DrawRectOutline(int x, int y, int w, int h,
                                        int argbColor) {
        if (w <= 0 || h <= 0) return;

        /* top edge */
        _DrawRect(x, y, w, 1, argbColor);
        /* bottom edge */
        _DrawRect(x, y + h - 1, w, 1, argbColor);
        /* left edge */
        _DrawRect(x, y + 1, 1, h - 2, argbColor);
        /* right edge */
        _DrawRect(x + w - 1, y + 1, 1, h - 2, argbColor);
    }

    /**
     * Draw a filled rectangle with a 1px border.
     */
    public static void DrawRectWithBorder(int x, int y, int w, int h,
                                          int fillIndex, int borderIndex,
                                          SpritePalette palette) {
        DrawRect(x, y, w, h, fillIndex, palette);
        DrawRectOutline(x, y, w, h, borderIndex, palette);
    }

    /**
     * Check if a point is inside a rectangle.
     * Useful for mouse hit testing.
     */
    public static boolean pointInRect(int px, int py,
                                      int rx, int ry, int rw, int rh) {
        return px >= rx && px < rx + rw && py >= ry && py < ry + rh;
    }

    /**
     * Draw a Bitmap at the specified position.
     * Transparent pixels (index 0) are skipped.
     *
     * @param bitmap the bitmap to draw
     * @param x left edge
     * @param y top edge
     */
    public static void DrawBitmap(Bitmap bitmap, int x, int y) {
        assert(init);
        assert(bitmap != null);

        int w = bitmap.width;
        int h = bitmap.height;

        /* early rejection */
        if (x + w <= 0 || x >= fbW) return;
        if (y + h <= 0 || y >= fbH) return;

        /* clip to screen */
        int x0 = Math.max(x, 0);
        int y0 = Math.max(y, 0);
        int x1 = Math.min(x + w, fbW);
        int y1 = Math.min(y + h, fbH);

        byte[] pixels = bitmap.data;
        int[] palette = bitmap.getPalette().colors;

        int srcX, texelIdx, color, fbIdx;
        int fbRowOffset, srcRowOffset;

        for (int py = y0; py < y1; ++py) {
            fbRowOffset = py * fbW * bpp;
            srcRowOffset = (py - y) * w;

            for (int px = x0; px < x1; ++px) {
                srcX = px - x;
                texelIdx = pixels[srcRowOffset + srcX] & 0xFF;

                /* transparency check */
                if (texelIdx == 0) continue;

                color = palette[texelIdx];
                fbIdx = fbRowOffset + px * bpp;

                buf[fbIdx]     = (byte)((color >> 16) & 0xFF);  /* R */
                buf[fbIdx + 1] = (byte)((color >> 8) & 0xFF);   /* G */
                buf[fbIdx + 2] = (byte)(color & 0xFF);          /* B */
                buf[fbIdx + 3] = (byte)((color >> 24) & 0xFF);  /* A */
            }
        }
    }

    public static final String CLASS = Renderer.class.getSimpleName();
    private static String ErrStrFailedInitValTooSmall(String s, int i) {
        return String.format("%s failed to initialize because [%s] [%d] must " +
                "be at least 1", CLASS, s, i);
    }
    private static String ErrStrInit() {
        return String.format("%s initialized with [%d] bytes per pixel.\n",
                CLASS, bpp);
    }
    private static String ErrStrFailedInitBufNull() {
        return String.format("%s failed to initialize because buf was null.\n",
                CLASS);
    }
}