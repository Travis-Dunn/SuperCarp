package production.ui;

import production.sprite.SpritePalette;
import whitetail.utility.logging.LogLevel;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.Logger.LogSession;

public final class TextRenderer {
    private static boolean init;

    private static int bpp;
    private static int buf[];
    private static int fbW, fbH;

    /**
     * Bounds packed as four unsigned 16-bit values in a long:
     *   bits 48-63 = x0  (left)
     *   bits 32-47 = y0  (top)
     *   bits 16-31 = x1  (right,  exclusive)
     *   bits  0-15 = y1  (bottom, exclusive)
     *
     * BOUNDS_EMPTY uses x0/y0 = 0xFFFF, x1/y1 = 0 so that any merge
     * with real bounds naturally produces the real bounds.
     */
    public static final long BOUNDS_EMPTY =
            ((long) 0xFFFF << 48)
                    | ((long) 0xFFFF << 32);

    public static long packBounds(int x0, int y0, int x1, int y1) {
        return ((long)(x0 & 0xFFFF) << 48)
                | ((long)(y0 & 0xFFFF) << 32)
                | ((long)(x1 & 0xFFFF) << 16)
                | ((long)(y1 & 0xFFFF));
    }

    public static int TLX(long b) { return (int)((b >>> 48) & 0xFFFF); }
    public static int TLY(long b) { return (int)((b >>> 32) & 0xFFFF); }
    public static int BRX(long b) { return (int)((b >>> 16) & 0xFFFF); }
    public static int BRY(long b) { return (int)(b & 0xFFFF);          }

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
     * @return tight bounding box of drawn pixels packed as a long
     *         (x0, y0, x1, y1 each 16 unsigned bits), or BOUNDS_EMPTY
     */
    public static long DrawLineLeft(FontAtlas atlas, String s, int x, int y,
                                    int argb) {
        assert(init);

        if (atlas == null) {
            LogFatalAndExit(ErrStrFailedDrawAtlasNull(s));
            return BOUNDS_EMPTY;
        }
        if (s == null) {
            LogFatalAndExit(ERR_STR_FAILED_DRAW_S_NULL);
            return BOUNDS_EMPTY;
        }
        if (s.isEmpty()) {
            LogSession(LogLevel.WARNING, ERR_STR_DRAW_EMPTY_STR);
            return BOUNDS_EMPTY;
        }

        if (y >= fbH || y + atlas.lineHeight < 0) {
            return BOUNDS_EMPTY;
        }
        if (x >= fbW) {
            return BOUNDS_EMPTY;
        }

        byte r = (byte)((argb >> 16) & 0xFF);
        byte g = (byte)((argb >> 8) & 0xFF);
        byte b = (byte)(argb & 0xFF);
        byte a = (byte)((argb >> 24) & 0xFF);

        int i, l = s.length();
        Glyph glyph;

        int bx0 = 0xFFFF, by0 = 0xFFFF, bx1 = 0, by1 = 0;

        for (i = 0; i < l; ++i, x += (glyph != null) ? glyph.xAdvance : 0) {
            if ((glyph = atlas.getGlyph(s.charAt(i))) == null) continue;

            long gb = BlitGlyph(atlas, x + glyph.xOffset, y + glyph.yOffset,
                    glyph, r, g, b, a);

            if (gb != BOUNDS_EMPTY) {
                int gx0 = TLX(gb), gy0 = TLY(gb);
                int gx1 = BRX(gb), gy1 = BRY(gb);
                if (gx0 < bx0) bx0 = gx0;
                if (gy0 < by0) by0 = gy0;
                if (gx1 > bx1) bx1 = gx1;
                if (gy1 > by1) by1 = gy1;
            }
        }

        if (bx0 > bx1) return BOUNDS_EMPTY;
        return packBounds(bx0, by0, bx1, by1);
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
     * @return tight bounding box of drawn pixels packed as a long
     *         (x0, y0, x1, y1 each 16 unsigned bits), or BOUNDS_EMPTY
     */
    public static long DrawLineCenter(FontAtlas atlas, String s, int x, int y,
                                      int argb) {
        assert(init);

        if (atlas == null) {
            LogFatalAndExit(ErrStrFailedDrawAtlasNull(s));
            return BOUNDS_EMPTY;
        }
        if (s == null) {
            LogFatalAndExit(ERR_STR_FAILED_DRAW_S_NULL);
            return BOUNDS_EMPTY;
        }
        if (s.isEmpty()) {
            LogSession(LogLevel.WARNING, ERR_STR_DRAW_EMPTY_STR);
            return BOUNDS_EMPTY;
        }

        if (y >= fbH || y + atlas.lineHeight < 0) {
            return BOUNDS_EMPTY;
        }

        byte r = (byte)((argb >> 16) & 0xFF);
        byte g = (byte)((argb >> 8) & 0xFF);
        byte b = (byte)(argb & 0xFF);
        byte a = (byte)((argb >> 24) & 0xFF);

        int i, l = s.length();
        Glyph glyph;

        /* We still need the total advance to compute the centering offset.
         * Walk the glyphs once to sum xAdvance — cheaper than measureWidth
         * since it avoids any extra string/atlas overhead in that method. */
        int totalAdvance = 0;
        for (i = 0; i < l; ++i) {
            glyph = atlas.getGlyph(s.charAt(i));
            if (glyph != null) totalAdvance += glyph.xAdvance;
        }

        int cursorX = x - totalAdvance / 2;

        int bx0 = 0xFFFF, by0 = 0xFFFF, bx1 = 0, by1 = 0;

        for (i = 0; i < l; ++i, cursorX += (glyph != null) ? glyph.xAdvance : 0) {
            if ((glyph = atlas.getGlyph(s.charAt(i))) == null) continue;

            long gb = BlitGlyph(atlas, cursorX + glyph.xOffset,
                    y + glyph.yOffset, glyph, r, g, b, a);

            if (gb != BOUNDS_EMPTY) {
                int gx0 = TLX(gb), gy0 = TLY(gb);
                int gx1 = BRX(gb), gy1 = BRY(gb);
                if (gx0 < bx0) bx0 = gx0;
                if (gy0 < by0) by0 = gy0;
                if (gx1 > bx1) bx1 = gx1;
                if (gy1 > by1) by1 = gy1;
            }
        }

        if (bx0 > bx1) return BOUNDS_EMPTY;
        return packBounds(bx0, by0, bx1, by1);
    }

    /***
     * Draws a line of text using the font atlas with top right (x, y).
     * Renders partially off-screen lines.
     * Wrapping, if desired, is caller's responsibility.
     *
     * @param atlas
     * @param s
     * @param x
     * @param y
     * @param argb
     * @return tight bounding box of drawn pixels packed as a long
     *         (x0, y0, x1, y1 each 16 unsigned bits), or BOUNDS_EMPTY
     */
    public static long DrawLineRight(FontAtlas atlas, String s, int x, int y,
                                     int argb) {
        assert(init);

        if (atlas == null) {
            LogFatalAndExit(ErrStrFailedDrawAtlasNull(s));
            return BOUNDS_EMPTY;
        }
        if (s == null) {
            LogFatalAndExit(ERR_STR_FAILED_DRAW_S_NULL);
            return BOUNDS_EMPTY;
        }
        if (s.isEmpty()) {
            LogSession(LogLevel.WARNING, ERR_STR_DRAW_EMPTY_STR);
            return BOUNDS_EMPTY;
        }

        if (y >= fbH || y + atlas.lineHeight < 0) {
            return BOUNDS_EMPTY;
        }
        if (x < 0) {
            return BOUNDS_EMPTY;
        }

        byte r = (byte)((argb >> 16) & 0xFF);
        byte g = (byte)((argb >> 8) & 0xFF);
        byte b = (byte)(argb & 0xFF);
        byte a = (byte)((argb >> 24) & 0xFF);

        int i, l = s.length();
        Glyph glyph;

        /* Sum xAdvance to compute right-alignment offset. */
        int totalAdvance = 0;
        for (i = 0; i < l; ++i) {
            glyph = atlas.getGlyph(s.charAt(i));
            if (glyph != null) totalAdvance += glyph.xAdvance;
        }

        int cursorX = x - totalAdvance;

        int bx0 = 0xFFFF, by0 = 0xFFFF, bx1 = 0, by1 = 0;

        for (i = 0; i < l; ++i, cursorX += (glyph != null) ? glyph.xAdvance : 0) {
            if ((glyph = atlas.getGlyph(s.charAt(i))) == null) continue;

            long gb = BlitGlyph(atlas, cursorX + glyph.xOffset,
                    y + glyph.yOffset, glyph, r, g, b, a);

            if (gb != BOUNDS_EMPTY) {
                int gx0 = TLX(gb), gy0 = TLY(gb);
                int gx1 = BRX(gb), gy1 = BRY(gb);
                if (gx0 < bx0) bx0 = gx0;
                if (gy0 < by0) by0 = gy0;
                if (gx1 > bx1) bx1 = gx1;
                if (gy1 > by1) by1 = gy1;
            }
        }

        if (bx0 > bx1) return BOUNDS_EMPTY;
        return packBounds(bx0, by0, bx1, by1);
    }

    private static long BlitGlyph(FontAtlas atlas, int x, int y, Glyph glyph,
                                  byte r, byte g, byte b, byte a) {
        if (x + glyph.w <= 0 || x >= fbW || y + glyph.h <= 0 || y >= fbH)
            return BOUNDS_EMPTY;

        int x0 = Math.max(x , 0);
        int y0 = Math.max(y , 0);
        int x1 = Math.min(x + glyph.w, fbW);
        int y1 = Math.min(y + glyph.h, fbH);

        byte buf[] = atlas.buf;
        int atlasW = atlas.w;
        int px, py, srcy, srcx, atlasRowOffset, fbRowOffset, atlasIdx, fbIdx;

        int bx0 = 0xFFFF, by0 = 0xFFFF, bx1 = 0, by1 = 0;

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

                if (px < bx0) bx0 = px;
                if (py < by0) by0 = py;
                if (px + 1 > bx1) bx1 = px + 1;
                if (py + 1 > by1) by1 = py + 1;
            }
        }

        if (bx0 > bx1) return BOUNDS_EMPTY;
        return packBounds(bx0, by0, bx1, by1);
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