package production.ui;

public final class Glyph {
    final int x, y;
    final int w, h;
    final int xOffset, yOffset;
    final int xAdvance;

    Glyph(int x, int y, int w, int h, int xOffset, int yOffset, int xAdvance) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.xAdvance = xAdvance;
    }
}
