package production.sprite;

import whitetail.utility.logging.LogLevel;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.ErrorStrings.ERR_STR_FAILED_INIT_OOM;
import static whitetail.utility.logging.Logger.LogSession;

/**
 * Maintains sprite data.
 */
public class SpriteSys {
    private static boolean init;
    private static int cap;
    private static int activeCount;
    private static int highMark;

    /* per-sprite, packed: bits 16-31 x, bits 0-15 y */
    private static int xyArr[];

    /* packed: bits 0-15 atlasIdx, bits 16-18 layer, bits 19-23 unused, bits 24-31 flags */
    private static int bitfieldArr[];

    private static int freeCount;
    private static int freeList[];

    private static final int DEF_CAP = 0xFFFF;
    private static final int MAX_CAP = 0x000FFFFF;
    private static final int MIN_CAP = 1;
    public static final int INVALID_HANDLE = -1;

    /* coordinate constants (16-bit signed) */
    private static final int XY_X_SHIFT =       16;
    private static final int XY_X_MASK =        0xFFFF0000;
    private static final int XY_Y_MASK =        0x0000FFFF;
    private static final int MIN_COORD =        -0x8000;
    private static final int MAX_COORD =         0x7FFF;

    /* flags occupy bits 24-31 of bitfieldArr */
    public static final byte FLAG_VISIBLE =                     0b00000001;
    public static final byte FLAG_FLIPH =                       0b00000010;
    public static final byte FLAG_FLIPV =                       0b00000100;
    private static final byte FLAG_VALID =                      0b00001000;

    /* layer constants */
    private static final int LAYER_BITS =       3;
    private static final int MAX_LAYER =        (1 << LAYER_BITS) - 1;  /* 7 */
    public static final int NUM_LAYERS =        1 << LAYER_BITS;        /* 8 */

    /* unused bits constants (available for application use) */
    private static final int UNUSED_BITS =      5;
    private static final int MAX_UNUSED_VAL =   (1 << UNUSED_BITS) - 1; /* 31 */

    /* bit layout constants */
    private static final int ATLAS_MASK =       0x0000FFFF;     /* bits 0-15  */
    private static final int LAYER_SHIFT =      16;
    private static final int LAYER_MASK =       0x00070000;     /* bits 16-18 */
    private static final int UNUSED_SHIFT =     19;
    private static final int UNUSED_MASK =      0x00F80000;     /* bits 19-23 */
    private static final int FLAGS_SHIFT =      24;
    private static final int FLAGS_MASK =       0xFF000000;     /* bits 24-31 */
    private static final int VALID_FLAG_BIT =   FLAG_VALID << FLAGS_SHIFT;

    private SpriteSys() {}

    /**
     * Initializes the sprite system with the given capacity.
     * @param cap maximum number of sprites (clamped to valid range)
     * @return true if initialization succeeded
     */
    public static boolean Init(int cap) {
        assert(!init);

        if (cap >= MIN_CAP && cap <= MAX_CAP) {
            SpriteSys.cap = cap;
        } else {
            SpriteSys.cap = DEF_CAP;
            LogSession(LogLevel.DEBUG, ErrStrCapOutOfBounds(cap, "cap",
                    DEF_CAP));
        }

        try {
            xyArr = new int[SpriteSys.cap];
            bitfieldArr = new int[SpriteSys.cap];
            freeList = new int[SpriteSys.cap];

            freeCount = highMark = activeCount = 0;

            return init = true;
        } catch (OutOfMemoryError e) {
            LogFatalAndExit(CLASS + ERR_STR_FAILED_INIT_OOM);

            return init = false;
        }
    }

    private static int Alloc() {
        assert(init);

        if (freeCount > 0) return freeList[--freeCount];
        else if (highMark < cap) return highMark++;
        return INVALID_HANDLE;
    }

    /**
     * Allocates a sprite at origin with default flags.
     * @return handle, or INVALID_HANDLE if pool exhausted
     */
    public static int Generate() {
        assert(init);

        int handle = Alloc();
        if (INVALID_HANDLE == handle) return INVALID_HANDLE;

        xyArr[handle] = 0;
        bitfieldArr[handle] = (FLAG_VISIBLE | FLAG_VALID) << FLAGS_SHIFT;

        activeCount++;

        return handle;
    }

    /**
     * Allocates a sprite with specified properties.
     * @param x        x position (-0x8000 to 0x7FFF)
     * @param y        y position (-0x8000 to 0x7FFF)
     * @param layer    render layer (0-7)
     * @param atlasIdx atlas index
     * @return handle, or INVALID_HANDLE on failure
     */
    public static int Create(int x, int y, byte layer, short atlasIdx) {
        assert(init);

        int handle;

        if (x < MIN_COORD || x > MAX_COORD) {
            LogFatalAndExit(ErrStrValOOB(x, "x", MIN_COORD, MAX_COORD));
            return INVALID_HANDLE;
        }

        if (y < MIN_COORD || y > MAX_COORD) {
            LogFatalAndExit(ErrStrValOOB(y, "y", MIN_COORD, MAX_COORD));
            return INVALID_HANDLE;
        }

        if (layer < 0 || layer > MAX_LAYER) {
            LogFatalAndExit(ErrStrValOOB(layer, "layer", 0, MAX_LAYER));
            return INVALID_HANDLE;
        }

        if (atlasIdx < 0 || atlasIdx > SpriteAtlas.MAX_ATLAS_IDX) {
            LogFatalAndExit(ErrStrValOOB(atlasIdx, "atlasIdx", 0,
                    SpriteAtlas.MAX_ATLAS_IDX));
            return INVALID_HANDLE;
        }

        handle = Alloc();
        if (INVALID_HANDLE == handle) return INVALID_HANDLE;

        xyArr[handle] = (x << XY_X_SHIFT) | (y & XY_Y_MASK);
        bitfieldArr[handle] = (atlasIdx & ATLAS_MASK)
                | ((layer & MAX_LAYER) << LAYER_SHIFT)
                | ((FLAG_VISIBLE | FLAG_VALID) << FLAGS_SHIFT);

        activeCount++;

        return handle;
    }

    /**
     * Returns a sprite to the pool.
     * @param handle sprite handle
     */
    public static void Remove(int handle) {
        assert(init);
        /* Not totally sure, but pretty sure that we don't need runtime bounds
        checking on handle. It is intended to be immutable, and only come into
        existence through Generate/Create */
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        bitfieldArr[handle] &= ~VALID_FLAG_BIT;
        freeList[freeCount++] = handle;
        activeCount--;
    }

    /**
     * Checks if a handle refers to a live sprite.
     * @param handle sprite handle
     * @return true if valid
     */
    public static boolean IsValid(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);

        return (bitfieldArr[handle] & VALID_FLAG_BIT) != 0;
    }

    /**
     * Sets sprite position.
     * @param handle sprite handle
     * @param x      x position (-0x8000 to 0x7FFF)
     * @param y      y position (-0x8000 to 0x7FFF)
     */
    public static void SetPos(int handle, int x, int y) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        if (x < MIN_COORD || x > MAX_COORD) {
            LogFatalAndExit(ErrStrValOOB(x, "x", MIN_COORD, MAX_COORD));
            return;
        }

        if (y < MIN_COORD || y > MAX_COORD) {
            LogFatalAndExit(ErrStrValOOB(y, "y", MIN_COORD, MAX_COORD));
            return;
        }

        xyArr[handle] = (x << XY_X_SHIFT) | (y & XY_Y_MASK);
    }

    /**
     * Sets sprite x position.
     * @param handle sprite handle
     * @param x      x position (-0x8000 to 0x7FFF)
     */
    public static void SetX(int handle, int x) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        if (x < MIN_COORD || x > MAX_COORD) {
            LogFatalAndExit(ErrStrValOOB(x, "x", MIN_COORD, MAX_COORD));
            return;
        }

        xyArr[handle] = (xyArr[handle] & XY_Y_MASK) | (x << XY_X_SHIFT);
    }

    /**
     * Sets sprite y position.
     * @param handle sprite handle
     * @param y      y position (-0x8000 to 0x7FFF)
     */
    public static void SetY(int handle, int y) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        if (y < MIN_COORD || y > MAX_COORD) {
            LogFatalAndExit(ErrStrValOOB(y, "y", MIN_COORD, MAX_COORD));
            return;
        }

        xyArr[handle] = (xyArr[handle] & XY_X_MASK) | (y & XY_Y_MASK);
    }

    /**
     * Sets sprite render layer.
     * @param handle sprite handle
     * @param layer  layer (0-7)
     */
    public static void SetLayer(int handle, byte layer) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        if (layer < 0 || layer > MAX_LAYER) {
            LogFatalAndExit(ErrStrValOOB(layer, "layer", 0, MAX_LAYER));
            return;
        }

        bitfieldArr[handle] = (bitfieldArr[handle] & ~LAYER_MASK)
                | ((layer & MAX_LAYER) << LAYER_SHIFT);
    }

    /**
     * Sets the 5 unused bits (19-23) for application-defined use.
     * @param handle sprite handle
     * @param value  value to store (0-31)
     */
    public static void SetUnused(int handle, byte value) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        if (value < 0 || value > MAX_UNUSED_VAL) {
            LogFatalAndExit(ErrStrValOOB(value, "unused", 0, MAX_UNUSED_VAL));
            return;
        }

        bitfieldArr[handle] = (bitfieldArr[handle] & ~UNUSED_MASK)
                | ((value & MAX_UNUSED_VAL) << UNUSED_SHIFT);
    }

    /**
     * Gets the 5 unused bits (19-23) for application-defined use.
     * @param handle sprite handle
     * @return value stored in unused bits (0-31)
     */
    public static byte GetUnused(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        return (byte)((bitfieldArr[handle] & UNUSED_MASK) >>> UNUSED_SHIFT);
    }

    /**
     * Sets sprite atlas index.
     * @param handle   sprite handle
     * @param atlasIdx atlas index
     */
    public static void SetAtlasIdx(int handle, short atlasIdx) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        if (atlasIdx < 0 || atlasIdx > SpriteAtlas.MAX_ATLAS_IDX) {
            LogFatalAndExit(ErrStrValOOB(atlasIdx, "atlasIdx", 0,
                    SpriteAtlas.MAX_ATLAS_IDX));
            return;
        }

        bitfieldArr[handle] = (bitfieldArr[handle] & ~ATLAS_MASK)
                | (atlasIdx & ATLAS_MASK);
    }

    /**
     * Sets user-visible flags (VISIBLE, FLIPH, FLIPV). FLAG_VALID is preserved.
     * @param handle sprite handle
     * @param flags  flag bits to set
     */
    public static void SetFlags(int handle, byte flags) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        /* preserve FLAG_VALID, apply user flags */
        bitfieldArr[handle] = (bitfieldArr[handle] & ~((FLAGS_MASK) ^ VALID_FLAG_BIT))
                | ((flags & ~FLAG_VALID) << FLAGS_SHIFT);
    }

    /**
     * Sets sprite visibility.
     * @param handle  sprite handle
     * @param visible true to show, false to hide
     */
    public static void SetVisible(int handle, boolean visible) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        if (visible) bitfieldArr[handle] |= FLAG_VISIBLE << FLAGS_SHIFT;
        else bitfieldArr[handle] &= ~(FLAG_VISIBLE << FLAGS_SHIFT);
    }

    /**
     * Sets horizontal flip state.
     * @param handle sprite handle
     * @param flipH  true to flip horizontally
     */
    public static void SetFlipH(int handle, boolean flipH) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        if (flipH) bitfieldArr[handle] |= FLAG_FLIPH << FLAGS_SHIFT;
        else bitfieldArr[handle] &= ~(FLAG_FLIPH << FLAGS_SHIFT);
    }

    /**
     * Sets vertical flip state.
     * @param handle sprite handle
     * @param flipV  true to flip vertically
     */
    public static void SetFlipV(int handle, boolean flipV) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        if (flipV) bitfieldArr[handle] |= FLAG_FLIPV << FLAGS_SHIFT;
        else bitfieldArr[handle] &= ~(FLAG_FLIPV << FLAGS_SHIFT);
    }

    /**
     * Gets sprite x position.
     * @param handle sprite handle
     * @return x position
     */
    public static int GetX(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        return xyArr[handle] >> XY_X_SHIFT;
    }

    /**
     * Gets sprite y position.
     * @param handle sprite handle
     * @return y position
     */
    public static int GetY(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        return (short)(xyArr[handle] & XY_Y_MASK);
    }

    /**
     * Gets sprite render layer.
     * @param handle sprite handle
     * @return layer (0-7)
     */
    public static byte GetLayer(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        return (byte)((bitfieldArr[handle] & LAYER_MASK) >>> LAYER_SHIFT);
    }

    /**
     * Gets sprite atlas index.
     * @param handle sprite handle
     * @return atlas index
     */
    public static short GetAtlasIdx(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        return (short)(bitfieldArr[handle] & ATLAS_MASK);
    }

    /**
     * Gets user-visible flags (FLAG_VALID masked out).
     * @param handle sprite handle
     * @return flag bits
     */
    public static byte GetFlags(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        /* return user-visible flags only, mask out FLAG_VALID */
        return (byte)(((bitfieldArr[handle] & FLAGS_MASK) >>> FLAGS_SHIFT) & ~FLAG_VALID);
    }

    /**
     * Checks if sprite is visible.
     * @param handle sprite handle
     * @return true if visible
     */
    public static boolean IsVisible(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        return (bitfieldArr[handle] & (FLAG_VISIBLE << FLAGS_SHIFT)) != 0;
    }

    /**
     * Checks if sprite is flipped horizontally.
     * @param handle sprite handle
     * @return true if flipped
     */
    public static boolean IsFlippedH(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        return (bitfieldArr[handle] & (FLAG_FLIPH << FLAGS_SHIFT)) != 0;
    }

    /**
     * Checks if sprite is flipped vertically.
     * @param handle sprite handle
     * @return true if flipped
     */
    public static boolean IsFlippedV(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        return (bitfieldArr[handle] & (FLAG_FLIPV << FLAGS_SHIFT)) != 0;
    }

    /**
     * Toggles horizontal flip state.
     * @param handle sprite handle
     */
    public static void FlipH(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        bitfieldArr[handle] ^= FLAG_FLIPH << FLAGS_SHIFT;
    }

    /**
     * Toggles vertical flip state.
     * @param handle sprite handle
     */
    public static void FlipV(int handle) {
        assert(init);
        assert(!(handle < 0) && handle < highMark);
        assert((bitfieldArr[handle] & VALID_FLAG_BIT) != 0);

        bitfieldArr[handle] ^= FLAG_FLIPV << FLAGS_SHIFT;
    }

    static int[] GetXYArr() {
        assert(init);

        return xyArr;
    }

    static int[] GetBitfieldArr() {
        assert(init);

        return bitfieldArr;
    }

    static int GetHighMark() {
        assert(init);

        return highMark;
    }

    static int GetActiveCount() {
        assert(init);

        return activeCount;
    }

    static int GetCap() {
        assert(init);

        return cap;
    }

    static int GetFreeCount() {
        assert(init);

        return freeCount;
    }

    /**
     * Releases all resources. System must be re-initialized before use.
     */
    public static void Shutdown() {
        assert(init);

        xyArr = null;
        bitfieldArr = null;
        freeList = null;

        init = false;
    }

    public static final String CLASS = SpriteSys.class.getSimpleName();
    private static String ErrStrCapOutOfBounds(int c, String s, int def) {
        return String.format("%s defaulted to [%s] capacity [%d] because an " +
                        "invalid [%s] capacity [%d] was requested. Valid " +
                        "range is [%d - %d] inclusive.\n", CLASS, s,
                def, s, c, MIN_CAP, MAX_CAP);
    }
    private static String ErrStrValOOB(int val, String s, int lo, int hi) {
        return String.format("%s attempted to use an out of bounds value," +
                        " [%d] for [%s]. Valid range is [%d - %d] inclusive.\n", CLASS, val, s,
                lo, hi);
    }
}