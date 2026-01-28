package production.ui;

import production.sprite.SpritePalette;

/**
 * Renders text to the framebuffer using a FontAtlas.
 * Supports word wrap and left/center/right alignment (left implemented).
 */
public final class TextRenderer {

    public static final int ALIGN_LEFT = 0;
    public static final int ALIGN_CENTER = 1;
    public static final int ALIGN_RIGHT = 2;

    private static final int BYTES_PER_PIXEL = 4;  // RGBA

    private TextRenderer() {}

    /**
     * Draw a string to the framebuffer.
     *
     * @param fb framebuffer (RGBA byte array)
     * @param fbWidth framebuffer width in pixels
     * @param fbHeight framebuffer height in pixels
     * @param font the font atlas to use
     * @param text the string to render
     * @param x left edge of text area
     * @param y top edge of text area
     * @param color palette color (ARGB packed int)
     * @param maxWidth max width for word wrap (0 = no wrap)
     * @param align alignment (ALIGN_LEFT, ALIGN_CENTER, ALIGN_RIGHT)
     * @return the Y position after the last line (for chaining)
     */
    public static int draw(byte[] fb, int fbWidth, int fbHeight,
                           FontAtlas font, String text,
                           int x, int y, int color,
                           int maxWidth, int align) {
        if (text == null || text.isEmpty()) return y;

        byte r = (byte)((color >> 16) & 0xFF);
        byte g = (byte)((color >> 8) & 0xFF);
        byte b = (byte)(color & 0xFF);
        byte a = (byte)((color >> 24) & 0xFF);

        int cursorX = x;
        int cursorY = y;
        int lineStart = 0;

        if (maxWidth <= 0) {
            /* no wrapping - single line */
            drawLine(fb, fbWidth, fbHeight, font, text, 0, text.length(),
                    x, y, r, g, b, a, align, maxWidth);
            return y + font.lineHeight;
        }

        /* word wrap */
        int i = 0;
        int lastSpace = -1;
        int lineWidth = 0;

        while (i < text.length()) {
            char c = text.charAt(i);

            /* handle explicit newlines */
            if (c == '\n') {
                drawLine(fb, fbWidth, fbHeight, font, text, lineStart, i,
                        x, cursorY, r, g, b, a, align, maxWidth);
                cursorY += font.lineHeight;
                lineStart = i + 1;
                lastSpace = -1;
                lineWidth = 0;
                i++;
                continue;
            }

            FontAtlas.Glyph glyph = font.getGlyph(c);
            int glyphAdvance = (glyph != null) ? glyph.xAdvance : 0;

            if (c == ' ') {
                lastSpace = i;
            }

            if (lineWidth + glyphAdvance > maxWidth && lineStart < i) {
                /* need to wrap */
                int lineEnd;
                int nextStart;

                if (lastSpace > lineStart) {
                    /* wrap at last space */
                    lineEnd = lastSpace;
                    nextStart = lastSpace + 1;
                } else {
                    /* no space - break at current position */
                    lineEnd = i;
                    nextStart = i;
                }

                drawLine(fb, fbWidth, fbHeight, font, text, lineStart, lineEnd,
                        x, cursorY, r, g, b, a, align, maxWidth);
                cursorY += font.lineHeight;
                lineStart = nextStart;
                lastSpace = -1;

                /* recalculate width from new line start */
                lineWidth = 0;
                for (int j = lineStart; j <= i; j++) {
                    if (j < text.length()) {
                        FontAtlas.Glyph g2 = font.getGlyph(text.charAt(j));
                        if (g2 != null) lineWidth += g2.xAdvance;
                    }
                }
            } else {
                lineWidth += glyphAdvance;
            }

            i++;
        }

        /* draw remaining text */
        if (lineStart < text.length()) {
            drawLine(fb, fbWidth, fbHeight, font, text, lineStart, text.length(),
                    x, cursorY, r, g, b, a, align, maxWidth);
            cursorY += font.lineHeight;
        }

        return cursorY;
    }

    /**
     * Draw a single line (or substring) of text.
     */
    private static void drawLine(byte[] fb, int fbWidth, int fbHeight,
                                 FontAtlas font, String text,
                                 int start, int end,
                                 int x, int y,
                                 byte r, byte g, byte b, byte a,
                                 int align, int maxWidth) {
        if (start >= end) return;

        int cursorX = x;

        /* alignment offset */
        if (align != ALIGN_LEFT && maxWidth > 0) {
            int lineWidth = font.measureWidth(text, start, end);
            if (align == ALIGN_CENTER) {
                cursorX = x + (maxWidth - lineWidth) / 2;
            } else if (align == ALIGN_RIGHT) {
                cursorX = x + maxWidth - lineWidth;
            }
        }

        for (int i = start; i < end; i++) {
            char c = text.charAt(i);
            FontAtlas.Glyph glyph = font.getGlyph(c);

            if (glyph == null) continue;

            int drawX = cursorX + glyph.xOffset;
            int drawY = y + glyph.yOffset;

            blitGlyph(fb, fbWidth, fbHeight, font, glyph,
                    drawX, drawY, r, g, b, a);

            cursorX += glyph.xAdvance;
        }
    }

    /**
     * Blit a single glyph to the framebuffer.
     */
    private static void blitGlyph(byte[] fb, int fbWidth, int fbHeight,
                                  FontAtlas font, FontAtlas.Glyph glyph,
                                  int screenX, int screenY,
                                  byte r, byte g, byte b, byte a) {
        /* early rejection */
        if (screenX + glyph.width <= 0 || screenX >= fbWidth ||
                screenY + glyph.height <= 0 || screenY >= fbHeight) {
            return;
        }

        /* clip to screen */
        int x0 = Math.max(screenX, 0);
        int y0 = Math.max(screenY, 0);
        int x1 = Math.min(screenX + glyph.width, fbWidth);
        int y1 = Math.min(screenY + glyph.height, fbHeight);

        byte[] atlasPixels = font.pixels;
        int atlasWidth = font.atlasWidth;

        for (int py = y0; py < y1; py++) {
            int srcY = py - screenY;
            int atlasRowOffset = (glyph.y + srcY) * atlasWidth;
            int fbRowOffset = py * fbWidth * BYTES_PER_PIXEL;

            for (int px = x0; px < x1; px++) {
                int srcX = px - screenX;
                int atlasIdx = atlasRowOffset + glyph.x + srcX;

                /* transparency check */
                if (atlasPixels[atlasIdx] == SpritePalette.TRANSPARENT_IDX) {
                    continue;
                }

                int fbIdx = fbRowOffset + px * BYTES_PER_PIXEL;
                fb[fbIdx]     = r;
                fb[fbIdx + 1] = g;
                fb[fbIdx + 2] = b;
                fb[fbIdx + 3] = a;
            }
        }
    }

    /**
     * Convenience overload: left-aligned, no wrap.
     */
    public static int draw(byte[] fb, int fbWidth, int fbHeight,
                           FontAtlas font, String text,
                           int x, int y, int color) {
        return draw(fb, fbWidth, fbHeight, font, text, x, y, color, 0, ALIGN_LEFT);
    }

    /**
     * Convenience overload: left-aligned with wrap.
     */
    public static int draw(byte[] fb, int fbWidth, int fbHeight,
                           FontAtlas font, String text,
                           int x, int y, int color, int maxWidth) {
        return draw(fb, fbWidth, fbHeight, font, text, x, y, color,
                maxWidth, ALIGN_LEFT);
    }

    public static final String CLASS = TextRenderer.class.getSimpleName();
}