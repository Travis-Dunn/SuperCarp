package production;

import whitetail.utility.logging.LogLevel;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.Logger.LogSession;

public class SpriteSys {
    private static boolean init;
    private static int cap;
    private static int activeCount;
    private static int highMark;

    /* per-sprite */
    private static int xArr[];
    private static int yArr[];
    private static byte layerArr[];
    /* duplicated, computed using atlasIdx at sprite creation or when atlasIdx
    is set */
    private static float sArr[];
    private static float tArr[];
    private static byte flagsArr[];
    private static boolean validArr[];
    /* should probably shift this in order to treat it as unsigned */
    private static short atlasIdxArr[];

    private static int freeCount;
    private static int freeList[];

    private static final int DEF_CAP = 0xFFFF;
    public static final int INVALID_HANDLE = -1;
    public static final byte FLAG_VISIBLE =                     0b00000001;
    public static final byte FLAG_FLIPH =                       0b00000010;
    public static final byte FLAG_FLIPV =                       0b00000100;

    private SpriteSys() {}

    public static boolean Init(int cap) {
        assert(!init);

        if (cap > 0) {
            SpriteSys.cap = cap;
        } else {
            SpriteSys.cap = DEF_CAP;
            LogSession(LogLevel.DEBUG, ErrStrCapOutOfBounds(cap, "cap",
                    DEF_CAP));
        }

        try {
            xArr = new int[SpriteSys.cap];
            yArr = new int[SpriteSys.cap];
            layerArr = new byte[SpriteSys.cap];
            sArr = new float[SpriteSys.cap];
            tArr = new float[SpriteSys.cap];
            flagsArr = new byte[SpriteSys.cap];
            atlasIdxArr = new short[SpriteSys.cap];
            validArr = new boolean[SpriteSys.cap];
            freeList = new int[SpriteSys.cap];

            freeCount = highMark = activeCount = 0;

            return init = true;
        } catch (OutOfMemoryError e) {
            LogFatalAndExit(ERR_STR_FAILED_INIT);

            return init = false;
        }
    }

    private static int AcquireHandle() {
        if (freeCount > 0) return freeList[--freeCount];
        else if (highMark < cap) return highMark++;
        return INVALID_HANDLE;
    }

    public static int Generate() {
        assert(init);

        int handle = AcquireHandle();
        if (INVALID_HANDLE == handle) return INVALID_HANDLE;

        xArr[handle] = 0;
        yArr[handle] = 0;
        layerArr[handle] = 0;
        atlasIdxArr[handle] = 0;
        sArr[handle] = 0.0f;
        tArr[handle] = 0.0f;
        flagsArr[handle] = FLAG_VISIBLE;
        validArr[handle] = true;

        activeCount++;

        return handle;
    }

    public static int Create(int x, int y, byte layer, short atlasIdx) {
        assert(init);

        int handle = AcquireHandle();
        if (INVALID_HANDLE == handle) return INVALID_HANDLE;

        if (atlasIdx < 0) {
            LogFatalAndExit(ErrStrIdxOOB(atlasIdx, "atlasIdx", 0,
                    0x3FFF));
            return INVALID_HANDLE;
        }

        xArr[handle] = x;
        yArr[handle] = y;
        layerArr[handle] = layer;
        atlasIdxArr[handle] = atlasIdx;
        /* TODO: Compute and set texture coordinates! */
        sArr[handle] = 0.0f;
        tArr[handle] = 0.0f;
        flagsArr[handle] = FLAG_VISIBLE;
        validArr[handle] = true;

        activeCount++;

        return handle;
    }

    public static void Remove(int handle) {
        assert(init);
        /* Not totally sure, but pretty sure that we don't need runtime bounds
        checking on handle. It should be final, and only come into existence
        through Generate/Create */
        assert(!(handle < 0) && handle < highMark);
        assert(validArr[handle]);

        validArr[handle] = false;
        freeList[freeCount++] = handle;
        activeCount--;
    }

    public static boolean IsValid(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert(validArr[handle]);

        return validArr[handle];
    }

    public static void SetPos(int handle, int x, int y) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert(validArr[handle]);

        xArr[handle] = x;
        yArr[handle] = y;
    }

    public static void SetX(int handle, int x) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert(validArr[handle]);

        xArr[handle] = x;
    }

    public static void SetY(int handle, int y) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert(validArr[handle]);

        yArr[handle] = y;
    }

    public static void SetLayer(int handle, byte layer) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert(validArr[handle]);

        layerArr[handle] = layer;
    }

    public static void SetAtlasIdx(int handle, short atlasIdx) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert(validArr[handle]);

        if (atlasIdx < 0) {
            LogFatalAndExit(ErrStrIdxOOB(atlasIdx, "atlasIdx", 0,
                    0x3FFF));
            return;
        }

        /* TODO: Compute and set texture coordinates! */

        atlasIdxArr[handle] = atlasIdx;
    }

    public static void SetFlags(int handle, byte flags) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert(validArr[handle]);

        flagsArr[handle] = flags;
    }

    public static void SetVisible(int handle, boolean visible) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert(validArr[handle]);

        if (visible) flagsArr[handle] |= FLAG_VISIBLE;
        else flagsArr[handle] &= ~FLAG_VISIBLE;
    }

    public static void SetFlipH(int handle, boolean flipH) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert(validArr[handle]);

        if (flipH) flagsArr[handle] |= FLAG_FLIPH;
        else flagsArr[handle] &= ~FLAG_FLIPH;
    }

    public static void SetFlipV(int handle, boolean flipV) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert(validArr[handle]);

        if (flipV) flagsArr[handle] |= FLAG_FLIPV;
        else flagsArr[handle] &= ~FLAG_FLIPV;
    }

    public static int GetX(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert(validArr[handle]);

        return xArr[handle];
    }

    public static int GetY(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert(validArr[handle]);

        return yArr[handle];
    }

    public static byte GetLayer(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert(validArr[handle]);

        return layerArr[handle];
    }

    public static short GetAtlasIdx(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert(validArr[handle]);

        return atlasIdxArr[handle];
    }

    public static byte GetFlags(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert(validArr[handle]);

        return flagsArr[handle];
    }

    public static boolean IsVisible(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert(validArr[handle]);

        return (flagsArr[handle] & FLAG_VISIBLE) != 0;
    }

    public static boolean IsFlippedH(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert(validArr[handle]);

        return (flagsArr[handle] & FLAG_FLIPH) != 0;
    }

    public static boolean IsFlippedV(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert(validArr[handle]);

        return (flagsArr[handle] & FLAG_FLIPV) != 0;
    }

    public static void FlipH(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert(validArr[handle]);

        flagsArr[handle] = (byte) (flagsArr[handle] ^ FLAG_FLIPH);
    }

    public static void FlipV(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert(validArr[handle]);

        flagsArr[handle] = (byte) (flagsArr[handle] ^ FLAG_FLIPV);
    }

    static int[] GetXArr() { return xArr; }
    static int[] GetYArr() { return yArr; }
    static byte[] GetLayerArr() { return layerArr; }
    static float[] GetSArr() { return sArr; }
    static float[] GetTArr() { return tArr; }
    static byte[] GetFlagsArr() { return flagsArr; }
    static boolean[] GetValidArr() { return validArr; }
    static short[] GetAtlasIdxArr() { return atlasIdxArr; }
    static int GetHighMark() { return highMark; }
    static int GetActiveCount() { return activeCount; }
    static int GetCap() { return cap; }
    static int GetFreeCount() { return freeCount; }

    public static void Shutdown() {
        assert(init);

        xArr = null;
        yArr = null;
        layerArr = null;
        sArr = null;
        tArr = null;
        atlasIdxArr = null;
        flagsArr = null;
        validArr = null;
        freeList = null;

        init = false;
    }

    public static final String CLASS = SpriteSys.class.getSimpleName();
    private static final String ERR_STR_FAILED_INIT = CLASS + " failed to " +
            "initialize because an OutOfMemoryError was encountered.\n";
    private static String ErrStrCapOutOfBounds(int c, String s, int def) {
        return String.format("%s defaulted to [%s] capacity [%d] because an " +
                        "invalid [%s] capacity [%d] was requested.\n", CLASS, s,
                def, s, c);
    }
    private static String ErrStrIdxOOB(int idx, String s, int lo, int hi) {
        return String.format("%s attempted to use an out of bounds index," +
        " [%d] for [%s]. Valid range is [%d - %d] inclusive.\n", CLASS, idx, s,
                lo, hi);
    }
}