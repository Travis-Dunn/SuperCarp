package production.ui;

/**
 * Holds glyph metrics and atlas pixel data for a BMFont.
 * Pixel data is palette-indexed (like SpriteAtlas).
 */
public final class FontAtlas {

    /** Metrics for a single glyph. */
    public static final class Glyph {
        public final int x, y;           // position in atlas
        public final int width, height;  // size in atlas
        public final int xOffset, yOffset; // render offset from cursor
        public final int xAdvance;       // cursor advance after glyph

        Glyph(int x, int y, int width, int height,
              int xOffset, int yOffset, int xAdvance) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.xAdvance = xAdvance;
        }
    }

    public final int lineHeight;
    public final int base;          // baseline offset from top of line
    public final int atlasWidth;
    public final int atlasHeight;
    public final byte[] pixels;     // palette-indexed, like SpriteAtlas

    private final Glyph[] glyphs;   // indexed by char code (ASCII)

    private static final int MAX_CHAR = 256;

    FontAtlas(int lineHeight, int base, int atlasWidth, int atlasHeight,
              byte[] pixels, Glyph[] glyphs) {
        this.lineHeight = lineHeight;
        this.base = base;
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;
        this.pixels = pixels;
        this.glyphs = glyphs;
    }

    /**
     * Get glyph metrics for a character.
     * Returns null if character not in font.
     */
    public Glyph getGlyph(char c) {
        int code = (int) c;
        if (code < 0 || code >= MAX_CHAR) return null;
        return glyphs[code];
    }

    /**
     * Measure the width of a string in pixels.
     * Does not account for word wrap.
     */
    public int measureWidth(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            Glyph g = getGlyph(text.charAt(i));
            if (g != null) {
                width += g.xAdvance;
            }
        }
        return width;
    }

    /**
     * Measure width of a string up to a maximum character count.
     */
    public int measureWidth(String text, int start, int end) {
        int width = 0;
        int len = Math.min(end, text.length());
        for (int i = start; i < len; i++) {
            Glyph g = getGlyph(text.charAt(i));
            if (g != null) {
                width += g.xAdvance;
            }
        }
        return width;
    }

    public static final String CLASS = FontAtlas.class.getSimpleName();
}