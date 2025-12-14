package production.sprite;

public class SpriteAtlas {
    private static boolean init;
    private static int atlasWidth;
    private static int atlasHeight;
    private static int spriteSize;
    private static int spritesPerRow;
    private static int spriteCount;

    /* indexed color: 0 = transparent */
    private static byte[] pixels;
    /* ARGB palette, 256 entries */
    private static int[] palette;

    private SpriteAtlas() {}

    public static boolean Init(int atlasWidth, int atlasHeight, int spriteSize) {
        assert(!init);
        assert(atlasWidth > 0);
        assert(atlasHeight > 0);
        assert(spriteSize > 0);
        assert(atlasWidth % spriteSize == 0);
        assert(atlasHeight % spriteSize == 0);

        SpriteAtlas.atlasWidth = atlasWidth;
        SpriteAtlas.atlasHeight = atlasHeight;
        SpriteAtlas.spriteSize = spriteSize;
        spritesPerRow = atlasWidth / spriteSize;
        int spritesPerCol = atlasHeight / spriteSize;
        spriteCount = spritesPerRow * spritesPerCol;

        return init = true;
    }

    /**
     * Set the atlas pixel data.
     * Expects indexed color, 1 byte per pixel, row-major, top-left origin.
     * Index 0 is treated as transparent.
     */
    public static void SetPixels(byte[] pixelData) {
        assert(init);
        assert(pixelData != null);
        assert(pixelData.length == atlasWidth * atlasHeight);

        pixels = pixelData;
    }

    /**
     * Set the color palette.
     * Expects 256 ARGB entries (alpha in high byte).
     */
    public static void SetPalette(int[] paletteData) {
        assert(init);
        assert(paletteData != null);
        assert(paletteData.length == 256);

        palette = paletteData;
    }

    /**
     * Get the top-left pixel offset into the atlas for a given sprite index.
     * Returns packed (x << 16) | y.
     */
    public static int GetPixelOffset(int atlasIdx) {
        assert(init);
        assert(atlasIdx >= 0 && atlasIdx < spriteCount);

        int sx = (atlasIdx % spritesPerRow) * spriteSize;
        int sy = (atlasIdx / spritesPerRow) * spriteSize;

        return (sx << 16) | sy;
    }

    /**
     * Get the X pixel offset for a sprite index.
     */
    public static int GetSpriteX(int atlasIdx) {
        assert(init);
        assert(atlasIdx >= 0 && atlasIdx < spriteCount);

        return (atlasIdx % spritesPerRow) * spriteSize;
    }

    /**
     * Get the Y pixel offset for a sprite index.
     */
    public static int GetSpriteY(int atlasIdx) {
        assert(init);
        assert(atlasIdx >= 0 && atlasIdx < spriteCount);

        return (atlasIdx / spritesPerRow) * spriteSize;
    }

    public static byte[] GetPixels() {
        assert(init);

        return pixels;
    }

    public static int[] GetPalette() {
        assert(init);

        return palette;
    }

    public static int GetAtlasWidth() {
        assert(init);

        return atlasWidth;
    }

    public static int GetAtlasHeight() {
        assert(init);

        return atlasHeight;
    }

    public static int GetSpriteSize() {
        assert(init);

        return spriteSize;
    }

    public static int GetSpriteCount() {
        assert(init);

        return spriteCount;
    }

    public static int GetSpritesPerRow() {
        assert(init);

        return spritesPerRow;
    }

    public static void Shutdown() {
        assert(init);

        pixels = null;
        palette = null;

        init = false;
    }

    public static final String CLASS = SpriteAtlas.class.getSimpleName();
}