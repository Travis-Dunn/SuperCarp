package production.displayOld;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import production.framebuffer.FramebufferPreset;
import whitetail.utility.logging.LogLevel;

import java.util.HashMap;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.ErrorStrings.ERR_STR_FAILED_INIT_OOM;
import static whitetail.utility.logging.Logger.LogSession;

public final class DisplayConfigOld {
    private static boolean init;

    /* 'display' Refers to the display resolution that the OS sees */
    private static int displayW, displayH;
    /* 'window' Refers to the window resolution that the OS sees */
    private static int windowW, windowH;
    /* 'emulated' Refers to the software rendered framebuffer */
    private static int emulatedW, emulatedH;
    /* 'viewport' Refers to the "game window", I.E., not dedicated UI space. */
    private static int viewportW, viewportH;
    /* Ratio of display pixels to emulated pixels */
    private static int iPixelScale;
    private static boolean bFullscreen;
    private static boolean bBorderless;
    private static boolean bVsync;
    private static int iFpsTarget;
    private static final int VP_TLX = 4;
    private static final int VP_TLY = 4;
    private static int vp[];

    private static HashMap<Integer, FramebufferPreset> framebufferCfgs;
    private static FramebufferPreset cfgs[];

    private static int offsetX, offsetY;

    /**
     * Ingests requested display properties, and queries physical display
     * information from OS, and uses them to determine the actual display
     * properties.
     *
     * Pass only:
     * Either [fullscreen, window resolution]
     * And
     * Either [pixel scale, emulated resolution]
     * The rest must be 0 or false.
     *
     * @param bFullscreen true if fullscreen is desired
     * @param iPixelScale ratio of screen pixels to framebuffer pixels
     * @param emulatedW width of the emulated display
     * @param emulatedH height of the emulated display
     * @param windowW width of the application window
     * @param windowH height of the application window.
     * @return true is success
     */
    public static boolean Init(
            boolean bFullscreen,
            boolean bBorderless,
            boolean bVsync,
            int iFpsTarget,
            int iPixelScale,
            int emulatedW,
            int emulatedH,
            int windowW,
            int windowH) {
        assert (!init);

        LogSession(LogLevel.DEBUG, CLASS + " initializing...\n");

        DisplayMode desktop = Display.getDesktopDisplayMode();
        displayW = desktop.getWidth();
        displayH = desktop.getHeight();

        if (iPixelScale < 0) {
            LogFatalAndExit(ErrStrFailedInitNeg("iPixelScale",
                    iPixelScale));
            return init = false;
        } else if (emulatedW < 0) {
            LogFatalAndExit(ErrStrFailedInitNeg("emulatedW", emulatedW));
            return init = false;
        } else if (emulatedH < 0) {
            LogFatalAndExit(ErrStrFailedInitNeg("emulatedH", emulatedH));
            return init = false;
        } else if (windowW < 0) {
            LogFatalAndExit(ErrStrFailedInitNeg("windowW", windowW));
            return init = false;
        } else if (windowH < 0) {
            LogFatalAndExit(ErrStrFailedInitNeg("windowH", windowH));
            return init = false;
        } else if (iFpsTarget < 0) {
            LogFatalAndExit(ErrStrFailedInitNeg("iFpsTarget", iFpsTarget));
            return init = false;
        }

        try {
            vp = new int[4];
            InitFramebufferConfigs();
        } catch (OutOfMemoryError e) {
            LogFatalAndExit(CLASS + ERR_STR_FAILED_INIT_OOM);
            return init = false;
        }

        vp[0] = VP_TLX;
        vp[1] = VP_TLY;

        DisplayConfigOld.bFullscreen = bFullscreen;
        DisplayConfigOld.bBorderless = bBorderless;
        DisplayConfigOld.bVsync = bVsync;
        DisplayConfigOld.iFpsTarget = iFpsTarget;

        /* fullscreen */
        if (bFullscreen && emulatedW == 0 && emulatedH == 0 && iPixelScale == 0
                && windowW == 0 && windowH == 0) {
            init = InitMode0();
        /* windowed, auto size */
        } else if (!bFullscreen && emulatedW == 0 && emulatedH == 0
                && iPixelScale == 0 && windowW == 0 && windowH == 0) {
            init = InitMode1();
        /* windowed, specify pixel scale and emulated res */
        } else if () {

        } else {
            LogFatalAndExit(ErrStrFailedInitIncompatibleParams());
            return init = false;
        }

        if (init) {
            SetViewport();
            LogSession(LogLevel.DEBUG, ErrStrInit());
        }

        return init;
    }

    /***
     * Create fullscreen config
     */
    private static boolean InitMode0() {
        FramebufferPreset cfg;
        int packed = PackRes(displayW, displayH);

        if ((cfg = framebufferCfgs.get(packed)) == null) {
            if ((cfg = FindBest()) == null) {
                LogFatalAndExit(ErrStrNoViableConfig());
                return false;
            } else {
                emulatedW = cfg.emulatedW;
                emulatedH = cfg.emulatedH;
                iPixelScale = Math.min(displayW / emulatedW,
                        displayH / emulatedH);
                offsetX = (displayW - (emulatedW * iPixelScale)) / 2;
                offsetY = (displayH - (emulatedH * iPixelScale)) / 2;
                windowW = displayW;
                windowH = displayH;
                SetViewport(cfg);

                return true;
            }
        } else {
            emulatedW = cfg.emulatedW;
            emulatedH = cfg.emulatedH;
            iPixelScale = displayW / emulatedW;
            windowW = displayW;
            windowH = displayH;
            offsetX = 0;
            offsetY = 0;
            SetViewport(cfg);

            return true;
        }
    }

    private static void SetViewport(FramebufferPreset cfg) {
        viewportW = cfg.viewportW;
        viewportH = cfg.viewportH;
        vp[0] = cfg.viewportX;
        vp[1] = cfg.viewportY;
        vp[2] = cfg.viewportX + cfg.viewportW;
        vp[3] = cfg.viewportY + cfg.viewportH;
    }

    private static FramebufferPreset FindBest() {
        FramebufferPreset cfg, cfg2 = null;
        int i, scale, area, area2 = 0;

        for (i = 0; i < cfgs.length; ++i) {
            cfg = cfgs[i];

            scale = Math.min(displayW / cfg.emulatedW,
                    displayH / cfg.emulatedH);

            if ((area = (scale * cfg.emulatedW) * (scale * cfg.emulatedH))
                    > area2) {
                area2 = area;
                cfg2 = cfg;
            }
        }
        return cfg2;
    }

    /***
     * Create fullscreen config given emulated resolution.
     *
     * Display resolution must be a multiple of emulated resolution.
     */
    private static boolean InitMode1(int emulatedW, int emulatedH) {
        DisplayConfigOld.iPixelScale = displayW / emulatedW;

        if (!(displayW == emulatedW * iPixelScale &&
                displayH == emulatedH * iPixelScale)) {
            LogFatalAndExit(ErrStrFailedInitEmulatedRes(emulatedW, emulatedH));
            return false;
        } else {
            windowW = displayW;
            windowH = displayH;
            DisplayConfigOld.emulatedW = emulatedW;
            DisplayConfigOld.emulatedH = emulatedH;
            return true;
        }
    }

    /***
     * Create windowed config given window dimensions and pixel scale
     *
     * Window dimensions must be multiples of pixel scale
     */
    private static boolean InitMode2(int windowW, int windowH, int iPixelScale) {
        if (!(windowW % iPixelScale == 0 && windowH % iPixelScale == 0)) {
            LogFatalAndExit(ErrStrFailedInitWindowRes(windowW, windowH, iPixelScale));
            return false;
        } else {
            DisplayConfigOld.iPixelScale = iPixelScale;
            DisplayConfigOld.windowW = windowW;
            DisplayConfigOld.windowH = windowH;
            emulatedW = DisplayConfigOld.windowW / DisplayConfigOld.iPixelScale;
            emulatedH = DisplayConfigOld.windowH / DisplayConfigOld.iPixelScale;
            return true;
        }
    }

    /***
     * Create windowed config given window and emulated res
     *
     * Window resolution must be a multiple of emulated resolution
     */
    private static boolean InitMode3(int windowW, int windowH,
            int emulatedW, int emulatedH) {
        if (!(windowW % emulatedW == 0 &&
                windowW * emulatedH == windowH * emulatedW)) {
            LogFatalAndExit(ErrStrFailedInitWindowEmulatedRes(windowW, windowH,
                    emulatedW, emulatedH));
            return false;
        } else {
            DisplayConfigOld.windowW = windowW;
            DisplayConfigOld.windowH = windowH;
            DisplayConfigOld.emulatedW = emulatedW;
            DisplayConfigOld.emulatedH = emulatedH;
            iPixelScale = DisplayConfigOld.windowW / DisplayConfigOld.emulatedW;
            return true;
        }
    }

    /***
     * Explicitly handles known combinations, otherwise sets viewport to two
     * thirds of framebuffer.
     * TODO: add explicit cases for each conceivable emulated framebuffer size.
     */
    private static void SetViewport() {
        if (emulatedW == 480 && emulatedH == 270) {
            viewportW = 320;
            viewportH = 180;
        } else if (emulatedW == 360 && emulatedH == 270) {
            viewportW = 240;
            viewportH = 180;
        } else {
            viewportW = (emulatedW / 3) * 2;
            viewportH = (emulatedH / 3) * 2;
            LogSession(LogLevel.DEBUG, ErrStrUnrecognizedEmulatedDimensions());
        }
        vp[2] = vp[0] + viewportW;
        vp[3] = vp[1] + viewportH;
    }

    private static void InitFramebufferConfigs() {
        cfgs = new FramebufferPreset[] {
                new FramebufferPreset(480, 270,
                        320, 180, 4, 4),
                new FramebufferPreset(512, 288,
                        340, 192, 4, 4),
                new FramebufferPreset(360, 270,
                        240, 180, 4, 4)
        };

        framebufferCfgs = new HashMap<Integer, FramebufferPreset>();

        framebufferCfgs.put(PackRes(1920, 1080), cfgs[0]);
        framebufferCfgs.put(PackRes(3840, 2160), cfgs[0]);
        framebufferCfgs.put(PackRes(2560, 1440), cfgs[1]);
    }

    private static int PackRes(int w, int h) {
        return ((w & 0xFFFF) << 16) | (h & 0xFFFF);
    }

    public static boolean isInViewport(int fbX, int fbY) {
        assert(init);
        return fbX >= vp[0] && fbX < vp[2] &&
                fbY >= vp[1] && fbY < vp[3];
    }

    public static int fbToVpX(int fbX) {
        assert(init);
        return fbX - vp[0];
    }

    public static int fbToVpY(int fbY) {
        assert(init);
        return fbY - vp[1];
    }

    public static int windowToFbX(int windowX) {
        assert(init);
        return (windowX * emulatedW) / windowW;
    }

    public static int windowToFbY(int windowY) {
        assert(init);
        // Window Y is bottom-up (LWJGL), FB Y is top-down
        return ((windowH - windowY) * emulatedH) / windowH;
    }

    /***
     * Get viewport bounds in emulated dimensions
     *
     * @return { tlx, tly, brx, bry }
     */
    public static int[] GetVP()         { assert(init); return vp; }
    public static int GetWindowW()      { assert(init); return windowW; }
    public static int GetWindowH()      { assert(init); return windowH; }
    public static int GetEmulatedW()    { assert(init); return emulatedW; }
    public static int GetEmulatedH()    { assert(init); return emulatedH; }
    public static int GetPixelScale()   { assert(init); return iPixelScale; }
    public static boolean IsFullscreen(){ assert(init); return bFullscreen; }
    public static boolean IsBorderless(){ assert(init); return bBorderless; }
    public static boolean IsVsync()     { assert(init); return bVsync; }
    public static int GetFpsTarget()    { assert(init); return iFpsTarget; }
    public static int GetViewportW()    { assert(init); return viewportW; }
    public static int GetViewportH()    { assert(init); return viewportH; }
    public static int GetViewportX()    { assert(init); return VP_TLX; }
    public static int GetViewportY()    { assert(init); return VP_TLY; }

    public static final String CLASS= DisplayConfigOld.class.getSimpleName();
    private static String ErrStrInit() {
        return String.format("%s initialized with " +
                "display width [%d], display height [%d], " +
                "window width [%d], window height [%d], " +
                "emulated width [%d], emulated height [%d], " +
                "viewport width [%d], viewport height [%d], " +
                "pixel scale [%d], and " +
                (bFullscreen ? "full screen" : "windowed") + " mode." +
                (bBorderless ? "borderless" : "decorated") + " mode.\n", CLASS,
                displayW, displayH, windowW, windowH, emulatedW, emulatedH,
                viewportW, viewportH, iPixelScale);
    }
    private static String ErrStrFailedInitNeg(String arg, int val) {
        return String.format("%s failed to initialize because [%s] was [%d]." +
                " It must be positive.\n", CLASS, arg, val);
    }
    private static String ErrStrFailedInitPixelScale() {
        return String.format("%s failed to initialize because display width " +
                "[%d] or display height [%d] was not a multiple of " +
                "iPixelScale [%d].\n", CLASS, displayW, displayH, iPixelScale);
    }
    private static String ErrStrFailedInitEmulatedRes(int emulatedW,
            int emulatedH) {
        return String.format("%s failed to initialize because display width " +
                "[%d] was not a multiple of emulated width [%d] or display " +
                "height [%d] was not a multiple of emulated height [%d].\n",
                CLASS, displayW, emulatedW, displayH, emulatedH);
    }
    private static String ErrStrFailedInitWindowRes(int windowW, int windowH,
            int iPixelScale) {
        return String.format("%s failed to initialize because window width " +
                "[%d] or window height [%d] was not a multiple of iPixelScale" +
                " [%d].\n", CLASS, windowW, windowH, iPixelScale);
    }
    private static String ErrStrFailedInitWindowEmulatedRes(int windowW,
            int windowH, int emulatedW, int emulatedH) {
        return String.format("%s failed to initialize because window width " +
                "[%d] was not a multiple of emulated width [%d], window " +
                "height [%d] was not a multiple of emulated height [%d], " +
                "or the aspect ratio was different.\n", CLASS, windowW,
                emulatedW, windowH, emulatedH);
    }
    private static String ErrStrFailedInitIncompatibleParams() {
        return String.format("%s failed to initialize because an incompatible" +
                " set of parameters was passed. Pass either bFullscreen, or " +
                "window dimensions, and either iPixelScale or emulated " +
                "dimensions. The other parameters must be 0 or false.\n",
                CLASS);
    }
    private static String ErrStrUnrecognizedEmulatedDimensions() {
        return String.format("%s encountered an unexpected combination of " +
                "emulated width [%d] and emulated height [%d]. Setting " +
                "viewport width to [%d] and viewport height to [%d] via auto " +
                "mode.\n", CLASS, emulatedW, emulatedH, viewportW, viewportH);
    }
    private static String ErrStrNoViableConfig() {
        return String.format("%s was not able to find a viable framebuffer " +
                "configuration for display width [%d], display height [%d].\n",
                CLASS, displayW, displayH);
    }
}