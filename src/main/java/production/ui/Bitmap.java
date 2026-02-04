package production.ui;

import production.sprite.SpritePalette;

/**
 * A variable-size indexed-color image for UI rendering.
 * Unlike sprites, these are not constrained to square or uniform sizes.
 */
public final class Bitmap {
    public final int width;
    public final int height;
    public final byte[] data;       // palette-indexed pixels, row-major
    private SpritePalette palette;

    Bitmap(int width, int height, byte[] data, SpritePalette palette) {
        this.width = width;
        this.height = height;
        this.data = data;
        this.palette = palette;
    }

    public SpritePalette getPalette() {
        return palette;
    }

    public void setPalette(SpritePalette palette) {
        this.palette = palette;
    }

    public static final String CLASS = Bitmap.class.getSimpleName();
}