package production.sprite;

import whitetail.utility.logging.LogLevel;

import static whitetail.utility.logging.Logger.LogSession;

public final class SpriteSys {
    private static boolean init;

    public static boolean Init(int cap, int fbWidth, int fbHeight) {
        assert(!init);

        int actualCap;

        if (cap >= SpritePool.MIN_CAP && cap <= SpritePool.MAX_CAP) {
            actualCap = cap;
        } else {
            actualCap = SpritePool.DEF_CAP;
            LogSession(LogLevel.DEBUG, ErrStrCapOutOfBounds(cap));
        }

        SpritePool.Init(actualCap);
        SpriteBackend.Init(fbWidth, fbHeight);
        SpriteRenderer.Init(fbWidth, fbHeight);
        return init = true;
    }

    public static final String CLASS = SpriteSys.class.getSimpleName();
    private static String ErrStrCapOutOfBounds(int c) {
        return String.format("%s defaulted to [%d] capacity because an " +
                "invalid capacity [%d] was requested. Valid range is " +
                "[%d - %d] inclusive.\n", CLASS, SpritePool.DEF_CAP, c
                , SpritePool.MIN_CAP, SpritePool.MAX_CAP);
    }
}
