package production.sprite;

public final class SpriteAtlas {
    private SpritePalette palette;
    public final int spriteSize;                     /* pixels */
    public final int spritesPerRow;                  /* unitless */
    public final int spriteCount;                    /* unitless */
    public final byte data[];

    public static final int MIN_IDX = 0x0;
    public static final int MAX_IDX = 0xFFFF;
    public static final int MAX_SIZE = 0x7FFF;

    SpriteAtlas(SpritePalette palette, int spriteSize, byte data[],
                int spritesPerRow, int spriteCount) {
        this.palette = palette;
        this.spriteSize = spriteSize;
        this.data = data;
        this.spritesPerRow = spritesPerRow;
        this.spriteCount = spriteCount;
    }

    public int getPixelOffset(int atlasIdx) {
        assert (atlasIdx >= 0 && atlasIdx < spriteCount);

        int sx = (atlasIdx % spritesPerRow) * spriteSize;
        int sy = (atlasIdx / spritesPerRow) * spriteSize;

        return (sx << 16) | sy;
    }

    public int getSpriteX(int atlasIdx) {
        assert (atlasIdx >= 0 && atlasIdx < spriteCount);

        return (atlasIdx % spritesPerRow) * spriteSize;
    }

    public int getSpriteY(int atlasIdx) {
        assert (atlasIdx >= 0 && atlasIdx < spriteCount);

        return (atlasIdx / spritesPerRow) * spriteSize;
    }

    public SpritePalette getPalette() {
        return palette;
    }

    public void setPalette(SpritePalette palette) {
        this.palette = palette;
    }

    public static final String CLASS = SpriteAtlas.class.getSimpleName();
}
