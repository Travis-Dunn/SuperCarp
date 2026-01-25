package production.sprite;

import java.util.ArrayList;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.ErrorStrings.ERR_STR_FAILED_INIT_OOM;

public final class SpriteAnimSys {
    private static boolean init;
    private static ArrayList<SpriteAnim> anims;
    private static byte[] flags;
    private static int flagsCap;

    public static final byte SPRITE_VALID_MASK  = (byte)0x01;
    public static final byte ANIM_DEF_VALID_MASK = (byte)0x02;
    public static final byte ANIM_VALID_MASK =
            SPRITE_VALID_MASK | ANIM_DEF_VALID_MASK;

    private static final int INITIAL_FLAGS_CAPACITY = 64;

    private SpriteAnimSys() {}

    public static boolean Init() {
        assert(!init);

        try {
            anims = new ArrayList<>();
            flagsCap = INITIAL_FLAGS_CAPACITY;
            flags = new byte[flagsCap];
            return init = true;
        } catch (OutOfMemoryError e) {
            LogFatalAndExit(CLASS + ERR_STR_FAILED_INIT_OOM);
            return init = false;
        }
    }

    public static SpriteAnim Create(int spriteHandle, SpriteAnimDef def) {
        assert(init);
        assert(SpritePool.IsValid(spriteHandle));
        assert(def != null);

        if (!(SpritePool.IsValid(spriteHandle))) {
            LogFatalAndExit(ErrStrInvalidSpriteHandle(spriteHandle));
            return null;
        }
        if (def == null) {
            LogFatalAndExit(ERR_STR_DEF_NULL);
            return null;
        }

        SpriteAnim anim = new SpriteAnim(spriteHandle, def);
        addAnim(anim, ANIM_VALID_MASK);
        return anim;
    }

    public static SpriteAnim Generate(int spriteHandle, SpriteAnimDef def) {
        assert(init);

        byte f = (byte)0;
        if (SpritePool.IsValid(spriteHandle)) f |= SPRITE_VALID_MASK;
        if (def != null)                      f |= ANIM_DEF_VALID_MASK;

        SpriteAnim anim = new SpriteAnim(spriteHandle, def);
        addAnim(anim, f);
        return anim;
    }

    private static void addAnim(SpriteAnim anim, byte f) {
        int idx = anims.size();
        anims.add(anim);
        ensureFlagsCapacity(idx + 1);
        flags[idx] = f;
    }

    private static void ensureFlagsCapacity(int required) {
        if (required <= flagsCap) return;

        int newCapacity = flagsCap;
        while (newCapacity < required) {
            newCapacity *= 2;
        }

        byte[] newFlags = new byte[newCapacity];
        System.arraycopy(flags, 0, newFlags, 0, flagsCap);
        flags = newFlags;
        flagsCap = newCapacity;
    }

    public static void Remove(SpriteAnim anim) {
        assert init;

        int idx = anims.indexOf(anim);
        if (idx == -1) return;

        // Swap-and-pop for O(1) removal
        int last = anims.size() - 1;
        if (idx != last) {
            anims.set(idx, anims.get(last));
            flags[idx] = flags[last];
        }
        anims.remove(last);
        flags[last] = 0;
    }

    public static void Update(float dt) {
        assert init;

        int dtMs = (int)(dt * 1000.0f);

        for (int i = 0; i < anims.size(); ++i) {
            if ((flags[i] & ANIM_VALID_MASK) == ANIM_VALID_MASK) {
                anims.get(i).update(dtMs);
            }
        }
    }

    public static void SetSpriteHandle(SpriteAnim anim, int handle) {
        assert init;

        int idx = anims.indexOf(anim);
        if (idx == -1) return;

        boolean valid = SpritePool.IsValid(handle);
        if (valid) {
            flags[idx] |= SPRITE_VALID_MASK;
        } else {
            flags[idx] &= ~SPRITE_VALID_MASK;
        }

        anim.setSpriteHandle(handle);
    }

    public static void SetDef(SpriteAnim anim, SpriteAnimDef def) {
        assert init;

        int idx = anims.indexOf(anim);
        if (idx == -1) return;

        if (def != null) {
            flags[idx] |= ANIM_DEF_VALID_MASK;
        } else {
            flags[idx] &= ~ANIM_DEF_VALID_MASK;
        }

        anim.setDef(def);
    }

    public static byte GetFlags(SpriteAnim anim) {
        assert init;

        int idx = anims.indexOf(anim);
        if (idx == -1) return 0;
        return flags[idx];
    }

    public static boolean IsValid(SpriteAnim anim) {
        return (GetFlags(anim) & ANIM_VALID_MASK) == ANIM_VALID_MASK;
    }

    public static boolean IsSpriteValid(SpriteAnim anim) {
        return (GetFlags(anim) & SPRITE_VALID_MASK) != 0;
    }

    public static boolean IsDefValid(SpriteAnim anim) {
        return (GetFlags(anim) & ANIM_DEF_VALID_MASK) != 0;
    }

    public static int GetActiveCount() {
        assert init;
        return anims.size();
    }

    public static void Shutdown() {
        assert init;

        anims.clear();
        anims = null;
        flags = null;
        flagsCap = 0;

        init = false;
    }

    public static final String CLASS = SpriteAnimSys.class.getSimpleName();
    private static String ErrStrInvalidSpriteHandle(int handle) {
        return String.format("%s failed to an create animation because the " +
                "sprite handle [%d] was invalid.\n", CLASS, handle);
    }
    private static final String ERR_STR_DEF_NULL = CLASS + " failed to create" +
            " an animation because the " + SpriteAnim.CLASS + " was null.\n";
}