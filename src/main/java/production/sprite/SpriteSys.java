package production.sprite;

import whitetail.utility.logging.LogLevel;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.Logger.LogSession;

public final class SpriteSys {
    private static boolean init;

    static int cap;
    static int fbWidth;
    static int fbHeight;
    public static final int MAX_CAP = 0x000FFFFF;
    public static final int MIN_CAP = 1;
    static int texID;

    public static boolean Init(int cap, int fbWidth, int fbHeight, int bpp) {
        assert(!init);

        LogSession(LogLevel.DEBUG, CLASS + " initializing...\n");

        if (cap >= MIN_CAP && cap <= MAX_CAP) {
            SpriteSys.cap = cap;
        } else {
            SpriteSys.cap = SpritePool.DEF_CAP;
            LogSession(LogLevel.DEBUG, ErrStrCapOutOfBounds(cap));
        }

        if (fbWidth > 0) {
            SpriteSys.fbWidth = fbWidth;
        } else {
            LogFatalAndExit(ErrStrFrameBufferTooNarrow(fbWidth));
            return false;
        }

        if (fbHeight > 0) {
            SpriteSys.fbHeight = fbHeight;
        } else {
            LogFatalAndExit(ErrStrFrameBufferTooShort(fbHeight));
            return false;
        }

        if (!SpritePool.Init()) {
            LogFatalAndExit(ERR_STR_FAILED_INIT_POOL);
            return init = false;
        }
        if (!SpriteBackend.Init(bpp)) {
            LogFatalAndExit(ERR_STR_FAILED_INIT_BACKEND);
            return init = false;
        }
        if (!SpriteRenderer.Init()) {
            LogFatalAndExit(ERR_STR_FAILED_INIT_RENDERER);
            return init = false;
        }

        LogSession(LogLevel.DEBUG, CLASS + " initialized.\n");

        return init = true;
    }

    public static void Shutdown() {
        assert(init);

        LogSession(LogLevel.DEBUG, CLASS + " shutting down...\n");

        SpritePool.Shutdown();
        SpriteBackend.Shutdown();
        SpriteRenderer.Shutdown();

        init = false;

        LogSession(LogLevel.DEBUG, CLASS + " shutdown complete.\n");
    }

    /* TODO: pickup here */
    /* It's not just that everything here needs to be good, but we need to go
    through the other classes, and route any public methods through this */
    public static int GetCapacity() {
        assert(init);

        return cap;
    }

    public static int GetFramebufferWidth() {
        assert(init);

        return fbWidth;
    }

    public static int GetFramebufferHeight() {
        assert(init);

        return fbHeight;
    }

    public static final String CLASS = SpriteSys.class.getSimpleName();
    private static String ErrStrCapOutOfBounds(int c) {
        return String.format("%s defaulted to [%d] capacity because an " +
                "invalid capacity [%d] was requested. Valid range is " +
                "[%d - %d] inclusive.\n", CLASS, SpritePool.DEF_CAP, c
                , MIN_CAP, MAX_CAP);
    }
    private static String ErrStrFrameBufferTooNarrow(int fbWidth) {
        return String.format("%s failed to initialize. Framebuffer width " +
                " [%d] must be greater than zero.\n", CLASS, fbWidth);
    }
    private static String ErrStrFrameBufferTooShort(int fbHeight) {
        return String.format("%s failed to initialize. Framebuffer height " +
                " [%d] must be greater than zero.\n", CLASS, fbHeight);
    }
    private static final String ERR_STR_FAILED_INIT_POOL = CLASS +
            " failed to initialize because " + SpritePool.CLASS + " failed " +
            "to initialize.\n";
    private static final String ERR_STR_FAILED_INIT_BACKEND = CLASS +
            " failed to initialize because " + SpriteBackend.CLASS + " failed" +
            " to initialize.\n";
    private static final String ERR_STR_FAILED_INIT_RENDERER = CLASS +
            " failed to initialize because " + SpriteRenderer.CLASS +
            " failed to initialize.\n";
}
