package production.ui;

import production.display.DisplayConfig;
import production.sprite.SpritePalette;
import production.sprite.SpriteSys;
import whitetail.utility.logging.LogLevel;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.Logger.LogSession;

public final class TextRenderer {
    private static boolean init;

    private static int bpp;
    private static int buf[];
    private static int fbW, fbH;

    public static boolean Init(int buf[], int fbW, int fbH, int bpp) {
        assert(!init);

        /* TODO input validation */
        TextRenderer.buf = buf;
        TextRenderer.fbW = fbW;
        TextRenderer.fbH = fbH;
        TextRenderer.bpp = bpp;

        return init = true;
    }

    /***
     * Draws a line of text using the font atlas with top left (x, y).
     * Renders partially off-screen lines.
     * Wrapping, if desired, is caller's responsibility.
     *
     * @param atlas
     * @param s
     * @param x
     * @param y
     * @param argb
     * @return width of the drawn line in pixels, or 0 on error
     */
    public static int DrawLineLeft(FontAtlas atlas, String s, int x, int y,
                                   int argb) {
        assert(init);

        if (atlas == null) {
            LogFatalAndExit(ErrStrFailedDrawAtlasNull(s));
            return 0;
        }
        if (s == null) {
            LogFatalAndExit(ERR_STR_FAILED_DRAW_S_NULL);
            return 0;
        }
        if (s.isEmpty()) {
            LogSession(LogLevel.WARNING, ERR_STR_DRAW_EMPTY_STR);
            return 0;
        }

        int width = atlas.measureWidth(s);

        if (y >= fbH || y + atlas.lineHeight < 0) {
            return width;
        }
        if (x >= fbW) {
            return width;
        }

        byte r = (byte)((argb >> 16) & 0xFF);
        byte g = (byte)((argb >> 8) & 0xFF);
        byte b = (byte)(argb & 0xFF);
        byte a = (byte)((argb >> 24) & 0xFF);

        int i, l = s.length();
        Glyph glyph;

        for (i = 0; i < l; ++i, x += (glyph != null) ? glyph.xAdvance : 0) {
            if ((glyph = atlas.getGlyph(s.charAt(i))) == null) continue;

            BlitGlyph(atlas, x + glyph.xOffset, y + glyph.yOffset, glyph,
                    r, g, b, a);
        }

        return width;
    }

    /***
     * Draws a line of text using the font atlas with top center (x, y).
     * Renders partially off-screen lines.
     * Wrapping, if desired, is caller's responsibility.
     *
     * @param atlas
     * @param s
     * @param x
     * @param y
     * @param argb
     * @return width of the drawn line in pixels, or 0 on error
     */
    public static int DrawLineCenter(FontAtlas atlas, String s, int x, int y,
                                     int argb) {
        assert(init);

        if (atlas == null) {
            LogFatalAndExit(ErrStrFailedDrawAtlasNull(s));
            return 0;
        }
        if (s == null) {
            LogFatalAndExit(ERR_STR_FAILED_DRAW_S_NULL);
            return 0;
        }
        if (s.isEmpty()) {
            LogSession(LogLevel.WARNING, ERR_STR_DRAW_EMPTY_STR);
            return 0;
        }

        int width = atlas.measureWidth(s);

        if (y >= fbH || y + atlas.lineHeight < 0) {
            return width;
        }

        byte r = (byte)((argb >> 16) & 0xFF);
        byte g = (byte)((argb >> 8) & 0xFF);
        byte b = (byte)(argb & 0xFF);
        byte a = (byte)((argb >> 24) & 0xFF);

        int i, l = s.length();
        int cursorX = x - width / 2;
        Glyph glyph;

        for (i = 0; i < l; ++i, cursorX += (glyph != null) ? glyph.xAdvance : 0) {
            if ((glyph = atlas.getGlyph(s.charAt(i))) == null) continue;

            BlitGlyph(atlas, cursorX + glyph.xOffset, y + glyph.yOffset, glyph,
                    r, g, b, a);
        }

        return width;
    }

    /***
     * Draws a line of text using the font atlas with top right (x, y).
     * center.
     * Renders partially off-screen lines.
     * Wrapping, if desired, is caller's responsibility.
     *
     * @param atlas
     * @param s
     * @param x
     * @param y
     * @param argb
     * @return width of the drawn line in pixels, or 0 on error
     */
    public static int DrawLineRight(FontAtlas atlas, String s, int x, int y,
                                    int argb) {
        assert(init);

        if (atlas == null) {
            LogFatalAndExit(ErrStrFailedDrawAtlasNull(s));
            return 0;
        }
        if (s == null) {
            LogFatalAndExit(ERR_STR_FAILED_DRAW_S_NULL);
            return 0;
        }
        if (s.isEmpty()) {
            LogSession(LogLevel.WARNING, ERR_STR_DRAW_EMPTY_STR);
            return 0;
        }

        int width = atlas.measureWidth(s);

        if (y >= fbH || y + atlas.lineHeight < 0) {
            return width;
        }
        if (x < 0) {
            return width;
        }

        byte r = (byte)((argb >> 16) & 0xFF);
        byte g = (byte)((argb >> 8) & 0xFF);
        byte b = (byte)(argb & 0xFF);
        byte a = (byte)((argb >> 24) & 0xFF);

        int i, l = s.length();
        int cursorX = x - width;
        Glyph glyph;

        for (i = 0; i < l; ++i, cursorX += (glyph != null) ? glyph.xAdvance : 0) {
            if ((glyph = atlas.getGlyph(s.charAt(i))) == null) continue;

            BlitGlyph(atlas, cursorX + glyph.xOffset, y + glyph.yOffset, glyph,
                    r, g, b, a);
        }

        return width;
    }

    private static void BlitGlyph(FontAtlas atlas, int x, int y, Glyph glyph,
                                  byte r, byte g, byte b, byte a) {
        if (x + glyph.w <= 0 || x >= fbW || y + glyph.h <= 0 || y >= fbH)
            return;

        int x0 = Math.max(x , 0);
        int y0 = Math.max(y , 0);
        int x1 = Math.min(x + glyph.w, fbW);
        int y1 = Math.min(y + glyph.h, fbH);

        byte buf[] = atlas.buf;
        int atlasW = atlas.w;
        int px, py, srcy, srcx, atlasRowOffset, fbRowOffset, atlasIdx, fbIdx;

        for (py = y0; py < y1; ++py) {
            srcy = py - y;
            atlasRowOffset = (glyph.y + srcy) * atlasW;
            fbRowOffset = py * fbW;

            for (px = x0; px < x1; ++px) {
                srcx = px - x;
                atlasIdx = atlasRowOffset + glyph.x + srcx;

                if (buf[atlasIdx] == SpritePalette.TRANSPARENT_IDX) continue;

                /* TODO: stop unpacking and repacking the color int.
                 * Make it so that bits per channel and order are configurable
                 * in the palette, then set it up so they match the buffer */
                TextRenderer.buf[fbRowOffset + px] = (r & 0xFF) << 24
                        | (g & 0xFF) << 16
                        | (b & 0xFF) <<  8
                        | (a & 0xFF);
            }
        }

    }

    public static final String CLASS = TextRenderer.class.getSimpleName();
    private static String ErrStrFailedDrawAtlasNull(String s) {
        return String.format("%s failed to draw string [%s] because the " +
                FontAtlas.CLASS + " was null.\n", CLASS, s);
    }
    private static final String ERR_STR_FAILED_DRAW_S_NULL = CLASS + " failed" +
            " to a draw string because it was null.\n";
    private static final String ERR_STR_DRAW_EMPTY_STR = CLASS + " tried to " +
            "draw an empty string.\n";
}