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

    private static int buf[];
    private static int fbW, fbH;

    private Renderer() {}

    public static boolean Init(int buf[], int fbW, int fbH) {
        assert(!init);

        LogSession(LogLevel.DEBUG, CLASS + " initializing...\n");

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
        if (paletteIndex < 0 || paletteIndex > palette.maxIdx) return;

        int color = palette.colors[paletteIndex];
        _DrawRect(x, y, w, h, color);
    }

    /**
     * Draw a filled rectangle with raw ARGB color.
     */
    private static void _DrawRect(int x, int y, int w, int h,
                                  int argbColor) {
        if (w <= 0 || h <= 0) return;
        if (x + w <= 0 || x >= Renderer.fbW) return;
        if (y + h <= 0 || y >= Renderer.fbH) return;

        int x0 = Math.max(x, 0);
        int y0 = Math.max(y, 0);
        int x1 = Math.min(x + w, Renderer.fbW);
        int y1 = Math.min(y + h, Renderer.fbH);

        int pixel = argbToRgba(argbColor);

        for (int py = y0; py < y1; py++) {
            int rowOffset = py * Renderer.fbW;
            for (int px = x0; px < x1; px++) {
                buf[rowOffset + px] = pixel;
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

        _DrawRect(x, y, w, 1, argbColor);
        _DrawRect(x, y + h - 1, w, 1, argbColor);
        _DrawRect(x, y + 1, 1, h - 2, argbColor);
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
     */
    public static void DrawBitmap(Bitmap bitmap, int x, int y) {
        assert(init);
        assert(bitmap != null);

        int w = bitmap.width;
        int h = bitmap.height;

        if (x + w <= 0 || x >= fbW) return;
        if (y + h <= 0 || y >= fbH) return;

        int x0 = Math.max(x, 0);
        int y0 = Math.max(y, 0);
        int x1 = Math.min(x + w, fbW);
        int y1 = Math.min(y + h, fbH);

        byte[] pixels = bitmap.data;
        int[] palette = bitmap.getPalette().colors;

        int srcX, texelIdx, fbIdx;
        int fbRowOffset, srcRowOffset;

        for (int py = y0; py < y1; ++py) {
            fbRowOffset = py * fbW;
            srcRowOffset = (py - y) * w;

            for (int px = x0; px < x1; ++px) {
                srcX = px - x;
                texelIdx = pixels[srcRowOffset + srcX] & 0xFF;

                if (texelIdx == 0) continue;

                fbIdx = fbRowOffset + px;
                buf[fbIdx] = argbToRgba(palette[texelIdx]);
            }
        }
    }

    public static void DrawBitmap1(Bitmap bitmap, int x, int y) {
        assert(init);
        assert(bitmap != null);

        int w = bitmap.width;
        int h = bitmap.height;

        if (x + w <= 0 || x >= fbW) return;
        if (y + h <= 0 || y >= fbH) return;

        int x0 = Math.max(x, 0);
        int y0 = Math.max(y, 0);
        int x1 = Math.min(x + w, fbW);
        int y1 = Math.min(y + h, fbH);

        byte[] pixels = bitmap.data;
        int[] palette = bitmap.getPalette().colors;

        int srcX, texelIdx, fbIdx;
        int fbRowOffset, srcRowOffset;

        for (int py = y0; py < y1; ++py) {
            fbRowOffset = py * fbW;
            srcRowOffset = (py - y) * w;

            for (int px = x0; px < x1; ++px) {
                srcX = px - x;
                texelIdx = pixels[srcRowOffset + srcX] & 0xFF;

                fbIdx = fbRowOffset + px;
                buf[fbIdx] = argbToRgba(palette[texelIdx]);
            }
        }
    }

    /**
     * Convert ARGB (as stored in palettes) to RGBA packed for
     * GL_UNSIGNED_INT_8_8_8_8.
     */
    private static int argbToRgba(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >>  8) & 0xFF;
        int b =  argb        & 0xFF;
        int a = (argb >> 24) & 0xFF;
        return (r << 24) | (g << 16) | (b << 8) | a;
    }

    public static final String CLASS = Renderer.class.getSimpleName();
    private static String ErrStrFailedInitValTooSmall(String s, int i) {
        return String.format("%s failed to initialize because [%s] [%d] must " +
                "be at least 1", CLASS, s, i);
    }
    private static String ErrStrInit() {
        return String.format("%s initialized.\n", CLASS);
    }
    private static String ErrStrFailedInitBufNull() {
        return String.format("%s failed to initialize because buf was null.\n",
                CLASS);
    }
}