package production.ui;

import whitetail.utility.logging.LogLevel;

import static whitetail.utility.logging.Logger.LogSession;

public final class FontAtlas {
    public final int lineHeight;
    public final int base;
    public final int w, h;
    final byte buf[];
    private final Glyph glyphs[];
    private static final int MAX_CHAR = 256;

    FontAtlas(int lineHeight, int base, int w, int h, byte buf[],
            Glyph glyphs[]) {
        this.lineHeight = lineHeight;
        this.base = base;
        this.w = w;
        this.h = h;
        this.buf = buf;
        this.glyphs = glyphs;
    }

    public Glyph getGlyph(char c) {
        return (c < MAX_CHAR) ? glyphs[c] : null;
    }

    public int measureWidth(String s) {
        int width = 0;
        if (s == null) {
            LogSession(LogLevel.DEBUG, ERR_STR_MEASURE_WIDTH_NULL);
            return width;
        }
        int i, len = s.length();
        for (i = 0; i < len; ++i) {
            Glyph g = getGlyph(s.charAt(i));
            if (g != null) width += g.xAdvance;
        }
        return width;
    }



    public static final String CLASS = FontAtlas.class.getSimpleName();
    private static final String ERR_STR_MEASURE_WIDTH_NULL = CLASS + " tried " +
            "to measure the width of a string, but the string was null.\n";
}
