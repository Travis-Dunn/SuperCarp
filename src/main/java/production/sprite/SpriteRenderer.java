package production.sprite;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.ErrorStrings.ERR_STR_FAILED_INIT_OOM;

public class SpriteRenderer {
    private static boolean init;

    private static SpriteCamera cam;
    private static int fbWidth;
    private static int fbHeight;
    private static byte[] framebuffer;

    private static final int MAX_LAYERS = 8;
    private static short[][] handlesByLayerArr;
    private static int[] layerCounts;

    private static final int BYTES_PER_PIXEL = 4;  // RGBA

    /* FLAG_VALID is package-private in SpriteSys, so we duplicate it here */
    private static final int FLAG_VALID = 0x08;

    private SpriteRenderer() {}

    public static boolean Init(int framebufferWidth, int framebufferHeight) {
        assert(!init);
        assert(framebufferWidth > 0);
        assert(framebufferHeight > 0);

        fbWidth = framebufferWidth;
        fbHeight = framebufferHeight;

        try {
            framebuffer = new byte[fbWidth * fbHeight * BYTES_PER_PIXEL];
            handlesByLayerArr = new short[MAX_LAYERS][SpriteSys.GetCap()];
            layerCounts = new int[MAX_LAYERS];
        } catch (OutOfMemoryError e) {
            LogFatalAndExit(CLASS + ERR_STR_FAILED_INIT_OOM);
            return init = false;
        }

        return init = true;
    }

    /**
     * Clear framebuffer to a solid color.
     * Color format is ARGB (same as palette).
     */
    public static void Clear(int color) {
        assert(init);

        byte r = (byte)((color >> 16) & 0xFF);
        byte g = (byte)((color >> 8) & 0xFF);
        byte b = (byte)(color & 0xFF);
        byte a = (byte)((color >> 24) & 0xFF);

        for (int i = 0; i < framebuffer.length; i += BYTES_PER_PIXEL) {
            framebuffer[i]     = r;
            framebuffer[i + 1] = g;
            framebuffer[i + 2] = b;
            framebuffer[i + 3] = a;
        }
    }

    public static void Render() {
        assert(init);
        assert(cam != null);

        int[] bitfieldArr = SpriteSys.GetBitfieldArr();
        int highMark = SpriteSys.GetHighMark();
        int i, j;

        /* reset layer counts */
        for (i = 0; i < MAX_LAYERS; ++i) {
            layerCounts[i] = 0;
        }

        /* bucket sprites by layer, skipping invalid/invisible */
        int validVisibleMask = (SpriteSys.FLAG_VISIBLE | FLAG_VALID) << 24;

        for (i = 0; i < highMark; ++i) {
            int bits = bitfieldArr[i];
            if ((bits & validVisibleMask) != validVisibleMask) {
                continue;
            }
            byte layer = SpriteSys.GetLayer(i);
            handlesByLayerArr[layer][layerCounts[layer]++] = (short)i;
        }

        /* cache frequently accessed data */
        int[] xyArr = SpriteSys.GetXYArr();
        byte[] atlasPixels = SpriteAtlas.GetPixels();
        int[] palette = SpriteAtlas.GetPalette();
        int atlasWidth = SpriteAtlas.GetAtlasWidth();
        int spriteSize = SpriteAtlas.GetSpriteSize();

        int camX = (int)cam.GetX();
        int camY = (int)cam.GetY();

        /* render back-to-front: layer 7 (back) to layer 0 (front) */
        for (i = MAX_LAYERS - 1; i >= 0; --i) {
            for (j = 0; j < layerCounts[i]; ++j) {
                int handle = handlesByLayerArr[i][j];
                int idx = handle << 1;

                int screenX = xyArr[idx] - camX;
                int screenY = xyArr[idx + 1] - camY;

                short atlasIdx = SpriteSys.GetAtlasIdx(handle);
                int atlasX = SpriteAtlas.GetSpriteX(atlasIdx);
                int atlasY = SpriteAtlas.GetSpriteY(atlasIdx);

                boolean flipH = SpriteSys.IsFlippedH(handle);
                boolean flipV = SpriteSys.IsFlippedV(handle);

                blitSprite(screenX, screenY, atlasX, atlasY, spriteSize,
                        atlasPixels, atlasWidth, palette, flipH, flipV);
            }
        }
    }

    private static void blitSprite(int screenX, int screenY,
                                   int atlasX, int atlasY, int size,
                                   byte[] atlasPixels, int atlasWidth,
                                   int[] palette,
                                   boolean flipH, boolean flipV) {
        /* early rejection: entirely off-screen */
        if (screenX + size <= 0 || screenX >= fbWidth ||
                screenY + size <= 0 || screenY >= fbHeight) {
            return;
        }

        /* clip to screen bounds */
        int x0 = Math.max(screenX, 0);
        int y0 = Math.max(screenY, 0);
        int x1 = Math.min(screenX + size, fbWidth);
        int y1 = Math.min(screenY + size, fbHeight);

        int srcX, srcY, texelIdx, color, fbIdx;
        int fbRowOffset, atlasRowOffset;

        for (int y = y0; y < y1; ++y) {
            fbRowOffset = y * fbWidth * BYTES_PER_PIXEL;

            srcY = y - screenY;
            if (flipV) srcY = (size - 1) - srcY;
            atlasRowOffset = (atlasY + srcY) * atlasWidth;

            for (int x = x0; x < x1; ++x) {
                srcX = x - screenX;
                if (flipH) srcX = (size - 1) - srcX;

                texelIdx = atlasPixels[atlasRowOffset + atlasX + srcX] & 0xFF;

                /* alpha test: index 0 = transparent */
                if (texelIdx == 0) continue;

                color = palette[texelIdx];
                fbIdx = fbRowOffset + x * BYTES_PER_PIXEL;

                framebuffer[fbIdx]     = (byte)((color >> 16) & 0xFF);  /* R */
                framebuffer[fbIdx + 1] = (byte)((color >> 8) & 0xFF);   /* G */
                framebuffer[fbIdx + 2] = (byte)(color & 0xFF);          /* B */
                framebuffer[fbIdx + 3] = (byte)0xFF;                    /* A */
            }
        }
    }

    public static byte[] GetFramebuffer() {
        assert(init);

        return framebuffer;
    }

    public static int GetWidth() {
        assert(init);

        return fbWidth;
    }

    public static int GetHeight() {
        assert(init);

        return fbHeight;
    }

    public static void SetCamera(SpriteCamera camera) {
        assert(camera != null);

        cam = camera;
    }

    public static void Shutdown() {
        assert(init);

        framebuffer = null;
        handlesByLayerArr = null;
        layerCounts = null;
        cam = null;

        init = false;
    }

    public static final String CLASS = SpriteRenderer.class.getSimpleName();
}