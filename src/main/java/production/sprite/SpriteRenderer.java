package production.sprite;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.ErrorStrings.ERR_STR_FAILED_INIT_OOM;

public final class SpriteRenderer {
    private static boolean init;

    private static SpriteCamera cam;
    private static int fbWidth;
    private static int fbHeight;
    private static byte[] framebuffer;

    private static final int MAX_LAYERS = 8;
    private static short[][] handlesByLayerArr;
    private static int[] layerCounts;

    private static final int BYTES_PER_PIXEL = 4;  // RGBA

    /* new, public for now, won't be when refactor finished */
    public static SpritePalette paletteArr[];
    public static SpriteAtlas atlasArr[];

    private SpriteRenderer() {}

    public static boolean Init(int framebufferWidth, int framebufferHeight) {
        assert(!init);
        assert(framebufferWidth > 0);
        assert(framebufferHeight > 0);

        fbWidth = framebufferWidth;
        fbHeight = framebufferHeight;

        try {
            framebuffer = new byte[fbWidth * fbHeight * BYTES_PER_PIXEL];
            handlesByLayerArr = new short[MAX_LAYERS][SpritePool.GetCapacity()];
            layerCounts = new int[MAX_LAYERS];
            paletteArr = new SpritePalette[SpritePool.MAX_PALETTE + 1];
            atlasArr = new SpriteAtlas[SpritePool.MAX_ATLAS + 1];
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

    public static void RenderNew() {
        assert(init);
        assert(cam != null);

        long[] arr = SpritePool.GetArr();
        int hm = SpritePool.GetHighMark();
        int i, j;

        for (i = 0; i < MAX_LAYERS; ++i) {
            layerCounts[i] = 0;
        }

        long renderMask = SpritePool.VALID_VISIBLE_MASK;

        for (i = 0; i < hm; ++i) {
            long bits = arr[i];

            if ((bits & renderMask) != renderMask) continue;

            int layer = (int)((bits & SpritePool.LAYER_MASK) >>> SpritePool.LAYER_SHIFT);
            handlesByLayerArr[layer][layerCounts[layer]++] = (short)i;
        }

        for (i = MAX_LAYERS - 1; i >= 0; --i) {
            for (j = 0; j < layerCounts[i]; ++j) {
                int handle = handlesByLayerArr[i][j];

                int screenX = SpritePool.GetX(handle) - (int)cam.getX();
                int screenY = SpritePool.GetY(handle) - (int)cam.getY();

                int atlasIdx = SpritePool.GetAtlasIdx(handle);
                int atlasId = SpritePool.GetAtlasId(handle);
                int atlasX = atlasArr[atlasId].getSpriteX(atlasIdx);
                int atlasY = atlasArr[atlasId].getSpriteY(atlasIdx);

                boolean flipH = SpritePool.IsHFlipped(handle);
                boolean flipV = SpritePool.IsVFlipped(handle);

                int size = atlasArr[atlasId].spriteSize;

                byte[] pixels = atlasArr[atlasId].data;

                /* TODO: use 'size' when rewriting this */
                int atlasW = atlasArr[atlasId].spritesPerRow * size;

                int[] palette = paletteArr[SpritePool.GetPaletteIdx(handle)].colors;

                blitSprite(screenX, screenY, atlasX, atlasY, size, pixels,
                        atlasW, palette, flipH, flipV);
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
                if (texelIdx == SpritePalette.TRANSPARENT_IDX) continue;

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