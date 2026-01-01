package production.sprite;

import java.util.ArrayList;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.ErrorStrings.ERR_STR_FAILED_INIT_OOM;

public final class SpriteAnimSys {
    private static boolean init;
    private static ArrayList<SpriteAnim> active;

    private SpriteAnimSys() {}

    public static boolean Init() {
        assert(!init);

        try {
            active = new ArrayList<>();
            return init = true;
        } catch (OutOfMemoryError e) {
            LogFatalAndExit(CLASS + ERR_STR_FAILED_INIT_OOM);
            return init = false;
        }
    }

    public static SpriteAnim Create(int spriteHandle, SpriteAnimDef def) {
        assert init;

        SpriteAnim anim = new SpriteAnim(spriteHandle, def);
        active.add(anim);
        return anim;
    }

    public static void Remove(SpriteAnim anim) {
        assert init;

        // Swap-and-pop for O(1) removal
        int idx = active.indexOf(anim);
        if (idx == -1) return;

        int last = active.size() - 1;
        if (idx != last) {
            active.set(idx, active.get(last));
        }
        active.remove(last);
    }

    public static void Update(float dt) {
        assert init;

        int dtMs = (int)(dt * 1000.0f);

        for (int i = 0; i < active.size(); ++i) {
            active.get(i).update(dtMs);
        }
    }

    public static int GetActiveCount() {
        assert init;
        return active.size();
    }

    public static void Shutdown() {
        assert init;

        active.clear();
        active = null;

        init = false;
    }

    public static final String CLASS = SpriteAnimSys.class.getSimpleName();
}