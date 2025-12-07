package production;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;

public class SpriteAtlas {
    private static boolean init;
    private static int textureSize;
    private static int spriteSize;
    private static float spriteSizeInTextures;
    private static int spriteCount;
    private static int rowSize;

    private SpriteAtlas() {}

    public static boolean Init(int textureSize, int spriteSize) {
        assert(!init);
        /* hand waving input validation because it will soon be re-written */
        assert(textureSize > 0 && spriteSize > 0);

        SpriteAtlas.textureSize = textureSize;
        SpriteAtlas.spriteSize = spriteSize;
        rowSize = SpriteAtlas.textureSize / SpriteAtlas.spriteSize;
        spriteCount = rowSize * rowSize;
        spriteSizeInTextures =
                (float)SpriteAtlas.spriteSize / SpriteAtlas.textureSize;

        return init = true;
    }

   public static long GetST(int atlasIdx) {
        assert(init);
        assert(atlasIdx < spriteCount && atlasIdx >= 0);

        int sBits = Float.floatToRawIntBits
                ((atlasIdx % rowSize) * spriteSizeInTextures);
        int tBits = Float.floatToRawIntBits
                ((atlasIdx / rowSize) * spriteSizeInTextures);

        return (long)sBits << 32 | tBits & 0xFFFFFFFFL;
    }

    public static final String CLASS = SpriteAtlas.class.getSimpleName();
    private static final String ERR_STR_FAILED_INIT = CLASS + " failed to " +
            "initialize because an OutOfMemoryError was encountered.\n";
}
