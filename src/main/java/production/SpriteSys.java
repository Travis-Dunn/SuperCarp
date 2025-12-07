package production;

import whitetail.utility.logging.LogLevel;

import static production.SpriteAtlas.GetST;
import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.Logger.LogSession;

public class SpriteSys {
    private static boolean init;
    private static int cap;
    private static int activeCount;
    private static int highMark;

    /* per-sprite */
    private static int xyArr[];
    /* duplicated, computed using atlasIdx at sprite creation or when atlasIdx
    is set */
    private static float stArr[];
    /* packed: bits 0-15 atlasIdx, bits 16-23 layer, bits 24-31 flags */
    private static int bitfieldArr[];

    private static int freeCount;
    private static int freeList[];

    private static final int DEF_CAP = 0xFFFF;
    public static final int INVALID_HANDLE = -1;

    /* flags occupy bits 24-31 of packedArr */
    public static final byte FLAG_VISIBLE =                     0b00000001;
    public static final byte FLAG_FLIPH =                       0b00000010;
    public static final byte FLAG_FLIPV =                       0b00000100;
    private static final byte FLAG_VALID =                      0b00001000;

    /* bit layout constants */
    private static final int ATLAS_MASK =       0x0000FFFF;
    private static final int LAYER_SHIFT =      16;
    private static final int LAYER_MASK =       0x00FF0000;
    private static final int FLAGS_SHIFT =      24;
    private static final int FLAGS_MASK =       0xFF000000;

    private SpriteSys() {}

    public static boolean Init(int cap) {
        assert(!init);

        if (cap > 0 && cap <= 0x3FFFFFFF) {
            SpriteSys.cap = cap;
        } else {
            SpriteSys.cap = DEF_CAP;
            LogSession(LogLevel.DEBUG, ErrStrCapOutOfBounds(cap, "cap",
                    DEF_CAP));
        }

        try {
            xyArr = new int[SpriteSys.cap * 2];
            stArr = new float[SpriteSys.cap * 2];
            bitfieldArr = new int[SpriteSys.cap];
            freeList = new int[SpriteSys.cap];

            freeCount = highMark = activeCount = 0;

            return init = true;
        } catch (OutOfMemoryError e) {
            LogFatalAndExit(ERR_STR_FAILED_INIT);

            return init = false;
        }
    }

    private static int Alloc() {
        assert(init);

        if (freeCount > 0) return freeList[--freeCount];
        else if (highMark < cap) return highMark++;
        return INVALID_HANDLE;
    }

    public static int Generate() {
        assert(init);

        int idx;
        int handle = Alloc();
        if (INVALID_HANDLE == handle) return INVALID_HANDLE;
        idx = handle << 1;

        xyArr[idx] = 0;
        xyArr[idx + 1] = 0;
        stArr[idx] = 0.0f;
        stArr[idx + 1] = 0.0f;
        bitfieldArr[handle] = (FLAG_VISIBLE | FLAG_VALID) << FLAGS_SHIFT;

        activeCount++;

        return handle;
    }

    public static int Create(int x, int y, byte layer, short atlasIdx) {
        assert(init);

        int idx;
        int handle;
        long st;

        if (atlasIdx < 0 || atlasIdx > 0x3FFF) {
            LogFatalAndExit(ErrStrIdxOOB(atlasIdx, "atlasIdx", 0,
                    0x3FFF));
            return INVALID_HANDLE;
        }

        handle = Alloc();
        if (INVALID_HANDLE == handle) return INVALID_HANDLE;
        idx = handle << 1;

        st = GetST(atlasIdx);

        xyArr[idx] = x;
        xyArr[idx + 1] = y;
        stArr[idx] = Float.intBitsToFloat((int)(st >> 32));
        stArr[idx + 1] = Float.intBitsToFloat((int)st);
        bitfieldArr[handle] = (atlasIdx & ATLAS_MASK)
                | ((layer & 0xFF) << LAYER_SHIFT)
                | ((FLAG_VISIBLE | FLAG_VALID) << FLAGS_SHIFT);

        activeCount++;

        return handle;
    }

    public static void Remove(int handle) {
        assert(init);
        /* Not totally sure, but pretty sure that we don't need runtime bounds
        checking on handle. It should be final, and only come into existence
        through Generate/Create */
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & (FLAG_VALID << FLAGS_SHIFT)) != 0);

        bitfieldArr[handle] &= ~(FLAG_VALID << FLAGS_SHIFT);
        freeList[freeCount++] = handle;
        activeCount--;
    }

    public static boolean IsValid(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);

        return (bitfieldArr[handle] & (FLAG_VALID << FLAGS_SHIFT)) != 0;
    }

    public static void SetPos(int handle, int x, int y) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & (FLAG_VALID << FLAGS_SHIFT)) != 0);

        int idx = handle << 1;

        xyArr[idx] = x;
        xyArr[idx + 1] = y;
    }

    public static void SetX(int handle, int x) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & (FLAG_VALID << FLAGS_SHIFT)) != 0);

        xyArr[handle << 1] = x;
    }

    public static void SetY(int handle, int y) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & (FLAG_VALID << FLAGS_SHIFT)) != 0);

        xyArr[(handle << 1) + 1] = y;
    }

    public static void SetLayer(int handle, byte layer) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & (FLAG_VALID << FLAGS_SHIFT)) != 0);

        bitfieldArr[handle] = (bitfieldArr[handle] & ~LAYER_MASK)
                | ((layer & 0xFF) << LAYER_SHIFT);
    }

    public static void SetAtlasIdx(int handle, short atlasIdx) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & (FLAG_VALID << FLAGS_SHIFT)) != 0);

        int idx = handle << 1;
        long st;

        if (atlasIdx < 0 || atlasIdx > 0x3FFF) {
            LogFatalAndExit(ErrStrIdxOOB(atlasIdx, "atlasIdx", 0,
                    0x3FFF));
            return;
        }

        st = GetST(atlasIdx);

        stArr[idx] = Float.intBitsToFloat((int)(st >> 32));
        stArr[idx + 1] = Float.intBitsToFloat((int)st);

        bitfieldArr[handle] = (bitfieldArr[handle] & ~ATLAS_MASK)
                | (atlasIdx & ATLAS_MASK);
    }

    public static void SetFlags(int handle, byte flags) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & (FLAG_VALID << FLAGS_SHIFT)) != 0);

        /* preserve FLAG_VALID, apply user flags */
        bitfieldArr[handle] = (bitfieldArr[handle] & ~((FLAGS_MASK) ^ (FLAG_VALID << FLAGS_SHIFT)))
                | ((flags & ~FLAG_VALID) << FLAGS_SHIFT);
    }

    public static void SetVisible(int handle, boolean visible) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & (FLAG_VALID << FLAGS_SHIFT)) != 0);

        if (visible) bitfieldArr[handle] |= FLAG_VISIBLE << FLAGS_SHIFT;
        else bitfieldArr[handle] &= ~(FLAG_VISIBLE << FLAGS_SHIFT);
    }

    public static void SetFlipH(int handle, boolean flipH) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & (FLAG_VALID << FLAGS_SHIFT)) != 0);

        if (flipH) bitfieldArr[handle] |= FLAG_FLIPH << FLAGS_SHIFT;
        else bitfieldArr[handle] &= ~(FLAG_FLIPH << FLAGS_SHIFT);
    }

    public static void SetFlipV(int handle, boolean flipV) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & (FLAG_VALID << FLAGS_SHIFT)) != 0);

        if (flipV) bitfieldArr[handle] |= FLAG_FLIPV << FLAGS_SHIFT;
        else bitfieldArr[handle] &= ~(FLAG_FLIPV << FLAGS_SHIFT);
    }

    public static int GetX(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & (FLAG_VALID << FLAGS_SHIFT)) != 0);

        return xyArr[handle << 1];
    }

    public static int GetY(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & (FLAG_VALID << FLAGS_SHIFT)) != 0);

        return xyArr[(handle << 1) + 1];
    }

    public static byte GetLayer(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & (FLAG_VALID << FLAGS_SHIFT)) != 0);

        return (byte)((bitfieldArr[handle] & LAYER_MASK) >>> LAYER_SHIFT);
    }

    public static short GetAtlasIdx(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & (FLAG_VALID << FLAGS_SHIFT)) != 0);

        return (short)(bitfieldArr[handle] & ATLAS_MASK);
    }

    public static byte GetFlags(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & (FLAG_VALID << FLAGS_SHIFT)) != 0);

        /* return user-visible flags only, mask out FLAG_VALID */
        return (byte)(((bitfieldArr[handle] & FLAGS_MASK) >>> FLAGS_SHIFT) & ~FLAG_VALID);
    }

    public static boolean IsVisible(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & (FLAG_VALID << FLAGS_SHIFT)) != 0);

        return (bitfieldArr[handle] & (FLAG_VISIBLE << FLAGS_SHIFT)) != 0;
    }

    public static boolean IsFlippedH(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & (FLAG_VALID << FLAGS_SHIFT)) != 0);

        return (bitfieldArr[handle] & (FLAG_FLIPH << FLAGS_SHIFT)) != 0;
    }

    public static boolean IsFlippedV(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & (FLAG_VALID << FLAGS_SHIFT)) != 0);

        return (bitfieldArr[handle] & (FLAG_FLIPV << FLAGS_SHIFT)) != 0;
    }

    public static void FlipH(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & (FLAG_VALID << FLAGS_SHIFT)) != 0);

        bitfieldArr[handle] ^= FLAG_FLIPH << FLAGS_SHIFT;
    }

    public static void FlipV(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & (FLAG_VALID << FLAGS_SHIFT)) != 0);

        bitfieldArr[handle] ^= FLAG_FLIPV << FLAGS_SHIFT;
    }

    static int[] GetXYArr() { return xyArr; }
    static float[] GetSTArr() { return stArr; }
    static int[] GetPackedArr() { return bitfieldArr; }
    static int GetHighMark() { return highMark; }
    static int GetActiveCount() { return activeCount; }
    static int GetCap() { return cap; }
    static int GetFreeCount() { return freeCount; }

    public static void Shutdown() {
        assert(init);

        xyArr = null;
        stArr = null;
        bitfieldArr = null;
        freeList = null;

        init = false;
    }

    public static final String CLASS = SpriteSys.class.getSimpleName();
    private static final String ERR_STR_FAILED_INIT = CLASS + " failed to " +
            "initialize because an OutOfMemoryError was encountered.\n";
    private static String ErrStrCapOutOfBounds(int c, String s, int def) {
        return String.format("%s defaulted to [%s] capacity [%d] because an " +
                        "invalid [%s] capacity [%d] was requested. Valid " +
                "range is [%d - %d] inclusive.\n", CLASS, s,
                def, s, c, 1, 0x3FFFFFFF);
    }
    private static String ErrStrIdxOOB(int idx, String s, int lo, int hi) {
        return String.format("%s attempted to use an out of bounds index," +
                        " [%d] for [%s]. Valid range is [%d - %d] inclusive.\n", CLASS, idx, s,
                lo, hi);
    }
}