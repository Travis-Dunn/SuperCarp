package production.sprite;

import whitetail.utility.logging.LogLevel;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.ErrorStrings.ERR_STR_FAILED_INIT_OOM;
import static whitetail.utility.logging.Logger.LogSession;

public final class SpritePool {
    private static boolean init;
    private static int cap;
    private static int activeCount;
    private static int highMark;
    private static long arr[];
    private static int freeCount;
    private static int freeList[];
    static final int DEF_CAP = 0xFFFF;
    static final int MAX_CAP = 0x000FFFFF;
    static final int MIN_CAP = 1;
    public static final int INVALID_HANDLE = -1;

    /**
     * XXXXXXXX_XXXXXXXX_YYYYYYYY_YYYYYYYY_AAAAAAAA_AAAAAAAA_LLLPPPII_IUUUVOES
     *
     * (16) Signed screen space [X] position
     * (16) Signed screen space [Y] position
     * (16) Unsigned [A]tlas index (intra-atlas sprite index)
     * (03) Unsigned [L]ayer
     * (03) Unsigned [P]alette
     * (03) Unsigned atlas [I]D (inter-atlas identifier)
     * (03) [U]nused
     * (01) [V]alid
     * (01) Flipped H[O]rizontally
     * (01) Flipped V[E]rtically
     * (01) Vi[S]ible
     */

    static final int X_SHIFT =          48;
    static final int Y_SHIFT =          32;
    static final int ATLAS_SHIFT =      16;
    static final int LAYER_SHIFT =      13;
    static final int PALETTE_SHIFT =    10;
    static final int ATLAS_ID_SHIFT =   7;
    static final int UNUSED_SHIFT =     4;
    static final int VALID_SHIFT =      3;
    static final int FLIP_H_SHIFT =     2;
    static final int FLIP_V_SHIFT =     1;

    static final long X_MASK =          0xFFFF000000000000L;
    static final long Y_MASK =          0x0000FFFF00000000L;
    static final long ATLAS_MASK =      0x00000000FFFF0000L;
    static final long LAYER_MASK =      0x000000000000E000L;
    static final long PALETTE_MASK =    0x0000000000001C00L;
    static final long ATLAS_ID_MASK =   0x0000000000000380L;
    static final long UNUSED_MASK =     0x0000000000000070L;
    static final long VALID_MASK =      0x0000000000000008L;
    static final long FLIP_H_MASK =     0x0000000000000004L;
    static final long FLIP_V_MASK =     0x0000000000000002L;
    static final long VISIBLE_MASK =    0x0000000000000001L;

    /* pre-computed for renderer */
    static final long VALID_VISIBLE_MASK = VALID_MASK | VISIBLE_MASK;

    /* inclusive */
    static final int MIN_POS =      -0x8000;
    static final int MAX_POS =       0x7FFF;
    static final int MIN_LAYER =     0x0;
    static final int MAX_LAYER =     0x7;
    static final int MIN_PALETTE =   0x0;
    static final int MAX_PALETTE =   0x7;
    static final int MIN_ATLAS =     0x0;
    static final int MAX_ATLAS =     0x7;

    private static final long LONG_16_MASK = 0xFFFFL;
    private static final long LONG_3_MASK = 0x7L;

    public static boolean Init(int cap) {
        assert(!init);
        assert(cap > 0);

        SpritePool.cap = cap;

        LogSession(LogLevel.DEBUG, CLASS + " initializing...\n");

        try {
            arr = new long[SpritePool.cap];
            freeList = new int[SpritePool.cap];
        } catch (OutOfMemoryError e) {
            LogFatalAndExit(CLASS + ERR_STR_FAILED_INIT_OOM);
            return init = false;
        }

        freeCount = highMark = activeCount = 0;

        LogSession(LogLevel.DEBUG, CLASS + " initialized with ["
                + SpritePool.cap + "] capacity.\n");

        return init = true;
    }

    private static int Alloc() {
        assert(init);

        if (freeCount > 0) return freeList[--freeCount];
        else if (highMark < cap) return highMark++;
        return INVALID_HANDLE;
    }

    public static int Generate() {
        assert(init);

        int handle = Alloc();
        if (INVALID_HANDLE == handle) return INVALID_HANDLE;

        arr[handle] = VALID_MASK;

        activeCount++;

        return handle;
    }

    public static int Create(int x, int y, int atlasId, int atlasIdx, int layer,
                             int paletteIdx, boolean hFlip, boolean vFlip, boolean visible) {
        assert(init);

        int handle;
        long val;

        if (x < MIN_POS || x > MAX_POS) {
            LogFatalAndExit(ErrStrValOOB(x, "x", MIN_POS, MAX_POS));
            return INVALID_HANDLE;
        }

        if (y < MIN_POS || y > MAX_POS) {
            LogFatalAndExit(ErrStrValOOB(y, "y", MIN_POS, MAX_POS));
            return INVALID_HANDLE;
        }

        if (atlasId < MIN_ATLAS || atlasId > MAX_ATLAS) {
            LogFatalAndExit(ErrStrValOOB(atlasId, "atlasId",
                    MIN_ATLAS, MAX_ATLAS));
            return INVALID_HANDLE;
        }

        if (atlasIdx < SpriteAtlas.MIN_IDX || atlasIdx > SpriteAtlas.MAX_IDX) {
            LogFatalAndExit(ErrStrValOOB(atlasIdx, "atlasIdx",
                    SpriteAtlas.MIN_IDX, SpriteAtlas.MAX_IDX));
            return INVALID_HANDLE;
        }

        if (layer < MIN_LAYER || layer > MAX_LAYER) {
            LogFatalAndExit(ErrStrValOOB(layer, "layer", MIN_LAYER,
                    MAX_LAYER));
            return INVALID_HANDLE;
        }

        if (paletteIdx < MIN_PALETTE || paletteIdx > MAX_PALETTE) {
            LogFatalAndExit(ErrStrValOOB(paletteIdx, "paletteIdx",
                    MIN_PALETTE, MAX_PALETTE));
            return INVALID_HANDLE;
        }

        handle = Alloc();
        if (INVALID_HANDLE == handle) return INVALID_HANDLE;

        val = VALID_MASK
                | ((long)x & LONG_16_MASK) << X_SHIFT
                | ((long)y & LONG_16_MASK) << Y_SHIFT
                | ((long)atlasIdx & LONG_16_MASK) << ATLAS_SHIFT
                | ((long)layer & LONG_3_MASK) << LAYER_SHIFT
                | ((long)paletteIdx & LONG_3_MASK) << PALETTE_SHIFT
                | ((long)atlasId & LONG_3_MASK) << ATLAS_ID_SHIFT
                | (hFlip   ? FLIP_H_MASK   : 0)
                | (vFlip   ? FLIP_V_MASK   : 0)
                | (visible ? VISIBLE_MASK  : 0);

        arr[handle] = val;

        activeCount++;

        return handle;
    }

    /* Current thinking is that we trust user to use the API correctly.
    Handles are meant to come into existence only through Generate/Create, so
    debug-time bounds checking should be sufficient. */
    public static void Remove(int handle) {
        assert(init);
        assert(IsValid(handle));

        arr[handle] &= ~VALID_MASK;
        freeList[freeCount++] = handle;
        activeCount--;
    }

    public static boolean IsValid(int handle) {
        assert(init);

        if (!(handle >= 0 && handle < highMark)) {
            LogFatalAndExit(ErrStrHandleOOB(handle));
            return false;
        }

        return (arr[handle] & VALID_MASK) != 0;
    }

    public static int GetX(int handle) {
        assert(init);
        assert(IsValid(handle));

        return (short)((arr[handle] & X_MASK) >>> X_SHIFT);
    }

    public static int GetY(int handle) {
        assert(init);
        assert(IsValid(handle));

        return (short)((arr[handle] & Y_MASK) >>> Y_SHIFT);
    }

    public static int GetAtlasId(int handle) {
        assert(init);
        assert(IsValid(handle));

        return (int)((arr[handle] & ATLAS_ID_MASK) >>> ATLAS_ID_SHIFT);
    }

    public static int GetAtlasIdx(int handle) {
        assert(init);
        assert(IsValid(handle));

        return (int)((arr[handle] & ATLAS_MASK) >>> ATLAS_SHIFT);
    }

    public static int GetLayer(int handle) {
        assert(init);
        assert(IsValid(handle));

        return (int)((arr[handle] & LAYER_MASK) >>> LAYER_SHIFT);
    }

    public static int GetPaletteIdx(int handle) {
        assert(init);
        assert(IsValid(handle));

        return (int)((arr[handle] & PALETTE_MASK) >>> PALETTE_SHIFT);
    }

    public static boolean IsHFlipped(int handle) {
        assert(init);
        assert(IsValid(handle));

        return (arr[handle] & FLIP_H_MASK) != 0;
    }

    public static boolean IsVFlipped(int handle) {
        assert(init);
        assert(IsValid(handle));

        return (arr[handle] & FLIP_V_MASK) != 0;
    }

    public static boolean IsVisible(int handle) {
        assert(init);
        assert(IsValid(handle));

        return (arr[handle] & VISIBLE_MASK) != 0;
    }

    public static void SetX(int handle, int x) {
        assert(init);
        assert(IsValid(handle));

        if (x < MIN_POS || x > MAX_POS) {
            LogFatalAndExit(ErrStrValOOB(x, "x", MIN_POS, MAX_POS));
            return;
        }

        arr[handle] = (arr[handle] & ~X_MASK) |
                (((long)x & LONG_16_MASK) << X_SHIFT);
    }

    public static void SetY(int handle, int y) {
        assert(init);
        assert(IsValid(handle));

        if (y < MIN_POS || y > MAX_POS) {
            LogFatalAndExit(ErrStrValOOB(y, "y", MIN_POS, MAX_POS));
            return;
        }

        arr[handle] = (arr[handle] & ~Y_MASK) |
                (((long)y & LONG_16_MASK) << Y_SHIFT);
    }

    public static void SetAtlasId(int handle, int atlasId) {
        assert(init);
        assert(IsValid(handle));

        if (atlasId < MIN_ATLAS || atlasId > MAX_ATLAS) {
            LogFatalAndExit(ErrStrValOOB(atlasId, "atlasId",
                    MIN_ATLAS, MAX_ATLAS));
            return;
        }

        arr[handle] = (arr[handle] & ~ATLAS_ID_MASK) |
                (((long)atlasId & LONG_3_MASK) << ATLAS_ID_SHIFT);
    }

    public static void SetAtlasIdx(int handle, int atlasIdx) {
        assert(init);
        assert(IsValid(handle));

        if (atlasIdx < SpriteAtlas.MIN_IDX || atlasIdx > SpriteAtlas.MAX_IDX) {
            LogFatalAndExit(ErrStrValOOB(atlasIdx, "atlasIdx",
                    SpriteAtlas.MIN_IDX, SpriteAtlas.MAX_IDX));
            return;
        }

        arr[handle] = (arr[handle] & ~ATLAS_MASK) |
                (((long)atlasIdx & LONG_16_MASK) << ATLAS_SHIFT);
    }

    public static void SetLayer(int handle, int layer) {
        assert(init);
        assert(IsValid(handle));

        if (layer < MIN_LAYER || layer > MAX_LAYER) {
            LogFatalAndExit(ErrStrValOOB(layer, "layer", MIN_LAYER, MAX_LAYER));
            return;
        }

        arr[handle] = (arr[handle] & ~LAYER_MASK) |
                (((long)layer & LONG_3_MASK) << LAYER_SHIFT);
    }

    public static void SetPaletteIdx(int handle, int paletteIdx) {
        assert(init);
        assert(IsValid(handle));

        if (paletteIdx < MIN_PALETTE || paletteIdx > MAX_PALETTE) {
            LogFatalAndExit(ErrStrValOOB(paletteIdx, "paletteIdx",
                    MIN_PALETTE, MAX_PALETTE));
            return;
        }

        arr[handle] = (arr[handle] & ~PALETTE_MASK) |
                (((long)paletteIdx & LONG_3_MASK) << PALETTE_SHIFT);
    }

    public static void SetHFlip(int handle, boolean hFlip) {
        assert(init);
        assert(IsValid(handle));

        if (hFlip) arr[handle] |= FLIP_H_MASK;
        else       arr[handle] &= ~FLIP_H_MASK;
    }

    public static void SetVFlip(int handle, boolean vFlip) {
        assert(init);
        assert(IsValid(handle));

        if (vFlip) arr[handle] |= FLIP_V_MASK;
        else       arr[handle] &= ~FLIP_V_MASK;
    }

    public static void SetVisible(int handle, boolean visible) {
        assert(init);
        assert(IsValid(handle));

        if (visible) arr[handle] |= VISIBLE_MASK;
        else         arr[handle] &= ~VISIBLE_MASK;
    }

    public static void SetPosition(int handle, int x, int y) {
        assert(init);
        assert(IsValid(handle));

        if (x < MIN_POS || x > MAX_POS) {
            LogFatalAndExit(ErrStrValOOB(x, "x", MIN_POS, MAX_POS));
            return;
        }

        if (y < MIN_POS || y > MAX_POS) {
            LogFatalAndExit(ErrStrValOOB(y, "y", MIN_POS, MAX_POS));
            return;
        }

        arr[handle] = (arr[handle] & ~(X_MASK | Y_MASK)) |
                (((long)x & LONG_16_MASK) << X_SHIFT) |
                (((long)y & LONG_16_MASK) << Y_SHIFT);
    }

    public static void SetFlip(int handle, boolean hFlip, boolean vFlip) {
        assert(init);
        assert(IsValid(handle));

        arr[handle] &= ~(FLIP_H_MASK | FLIP_V_MASK);
        if (hFlip) arr[handle] |= FLIP_H_MASK;
        if (vFlip) arr[handle] |= FLIP_V_MASK;
    }

    public static void SetAtlas(int handle, int atlasId, int atlasIdx) {
        assert(init);
        assert(IsValid(handle));

        if (atlasId < MIN_ATLAS || atlasId > MAX_ATLAS) {
            LogFatalAndExit(ErrStrValOOB(atlasId, "atlasId",
                    MIN_ATLAS, MAX_ATLAS));
            return;
        }

        if (atlasIdx < SpriteAtlas.MIN_IDX || atlasIdx > SpriteAtlas.MAX_IDX) {
            LogFatalAndExit(ErrStrValOOB(atlasIdx, "atlasIdx",
                    SpriteAtlas.MIN_IDX, SpriteAtlas.MAX_IDX));
            return;
        }

        arr[handle] = (arr[handle] & ~(ATLAS_ID_MASK | ATLAS_MASK)) |
                (((long)atlasId & LONG_3_MASK) << ATLAS_ID_SHIFT) |
                (((long)atlasIdx & LONG_16_MASK) << ATLAS_SHIFT);
    }

    public static void Translate(int handle, int dx, int dy) {
        assert(init);
        assert(IsValid(handle));

        int newX = GetX(handle) + dx;
        int newY = GetY(handle) + dy;

        if (newX < MIN_POS || newX > MAX_POS) {
            LogFatalAndExit(ErrStrValOOB(newX, "x (after translate)",
                    MIN_POS, MAX_POS));
            return;
        }

        if (newY < MIN_POS || newY > MAX_POS) {
            LogFatalAndExit(ErrStrValOOB(newY, "y (after translate)",
                    MIN_POS, MAX_POS));
            return;
        }

        SetPosition(handle, newX, newY);
    }

    public static void ToggleHFlip(int handle) {
        assert(init);
        assert(IsValid(handle));

        arr[handle] ^= FLIP_H_MASK;
    }

    public static void ToggleVFlip(int handle) {
        assert(init);
        assert(IsValid(handle));

        arr[handle] ^= FLIP_V_MASK;
    }

    public static void ToggleVisible(int handle) {
        assert(init);
        assert(IsValid(handle));

        arr[handle] ^= VISIBLE_MASK;
    }

    public static boolean IsInitialized() {
        return init;
    }

    public static int GetCapacity() {
        assert(init);

        return cap;
    }

    public static int GetActiveCount() {
        assert(init);

        return activeCount;
    }

    public static int GetHighMark() {
        assert(init);

        return highMark;
    }

    public static int GetFreeCount() {
        assert(init);

        return freeCount;
    }

    public static long GetRawData(int handle) {
        assert(init);
        assert(IsValid(handle));

        return arr[handle];
    }

    static long[] GetArr() {
        assert(init);

        return arr;
    }

    public static void Shutdown() {
        assert(init);

        arr = null;
        freeList = null;

        init = false;
    }

    private static final String CLASS = SpritePool.class.getSimpleName();

    private static String ErrStrValOOB(int val, String s, int lo, int hi) {
        return String.format("%s attempted to use an out of bounds value," +
                        " [%d] for [%s]. Valid range is [%d - %d] inclusive.\n", CLASS,
                val, s, lo, hi);
    }

    private static String ErrStrHandleOOB(int handle) {
        return String.format("%s attempted to check the validity of a clearly" +
                " out of bounds handle [%d]. Active range is currently " +
                "[0 - %d].\n", CLASS, handle, highMark);
    }
}