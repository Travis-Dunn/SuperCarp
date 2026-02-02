package production;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import whitetail.utility.logging.LogLevel;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.Logger.LogSession;

public final class DisplayConfig {
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

    /**
     * Ingests requested display properties, and queries physical display
     * information from OS, and uses them to determine the actual display
     * properties.
     *
     * Pass only:
     * One of [fullscreen, window resolution]
     * And
     * One of [pixel scale, emulated resolution]
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
            LogFatalAndExit(ERR_STR_INIT_FAILED_PIXEL_SCALE_NEG);
            return init = false;
        }

        DisplayConfig.bFullscreen = bFullscreen;

        if (bFullscreen && windowW == 0 && windowH == 0) {
            if (emulatedW == 0 && emulatedH == 0 && iPixelScale != 0) {
                init = InitMode0(iPixelScale);
            } else if (emulatedW != 0 && emulatedH != 0 && iPixelScale == 0) {
                init = InitMode1();
            }
        } else if (!bFullscreen && windowW != 0 && windowH != 0) {
            if (emulatedW == 0 && emulatedH == 0 && iPixelScale != 0) {
                init = InitMode2();
            } else if (emulatedW != 0 && emulatedH != 0 && iPixelScale == 0) {
                init = InitMode3();
            }
        } else {
            LogFatalAndExit(ErrStrFailedInitIncompatibleParams());
            return init = false;
        }

        SetViewport();

        if (init) LogSession(LogLevel.DEBUG, ERR_STR_INIT);

        return init;
    }

    /***
     * Create fullscreen config given pixel scale.
     *
     * Display resolution must be a multiple of pixel scale.
     */
    private static boolean InitMode0(int iPixelScale) {
        if (!(displayW % iPixelScale == 0 && displayH % iPixelScale == 0)) {
            LogFatalAndExit(ErrStrInitFailedPixelScale());
            return false;
        } else {
            DisplayConfig.iPixelScale = iPixelScale;
            windowW = displayW;
            windowH = displayH;
            emulatedW = displayW / DisplayConfig.iPixelScale;
            emulatedH = displayH / DisplayConfig.iPixelScale;
        }

        return true;
    }

    /***
     * Create fullscreen config given emulated resolution.
     *
     * Display resolution must be a multiple of emulated resolution.
     */
    private static boolean InitMode1() {
        return true;
    }

    private static boolean InitMode2() {
        return true;
    }

    private static boolean InitMode3() {
        return true;
    }

    private static int AutoCalcPixelScale() {
        if (displayW == 1920 && displayH == 1080) {
            return 4;
        } else {
            LogFatalAndExit(ErrStrFailedInitUnsupportedDisplay());
            return 0;
        }
    }

    /***
     * Explicitly handles known combinations, otherwise sets viewport to two
     * thirds of framebuffer.
     */
    private static void SetViewport() {
        if (emulatedW == 480 && displayH == 270) {
            viewportW = 320;
            viewportH = 180;
        } else {
            viewportW = (emulatedW / 3) * 2;
            viewportH = (emulatedH / 3) * 2;
        }
    }

    private static final String CLASS= DisplayConfig.class.getSimpleName();
    private static final String ERR_STR_INIT = CLASS + " initialized with " +
            "display width [" + displayW + "], and display height [" +
            displayH + "]\n frame buffer width [" + emulatedW + "], and " +
            "framebuffer height [" + emulatedH + "]\n viewport port width " +
            "[" + viewportW + "], and viewport height [" + viewportH + "]\n" +
            "pixel scale [" + iPixelScale + "], and " +
            (bFullscreen ? "full screen" : "windowed") + " mode.\n";
    private static final String ERR_STR_INIT_FAILED_PIXEL_SCALE_NEG = CLASS +
            " failed to initialize because iPixelScale was negative.\n";
    private static String ErrStrInitFailedPixelScale() {
        return String.format("%s failed to initialize because display width " +
                "[%d] or display height [%d] was not a multiple of " +
                "iPixelScale [%d].\n", CLASS, displayW, displayH, iPixelScale);
    }
    private static String ErrStrFailedInitUnsupportedDisplay() {
        return String.format("%s failed to initialize because it attempted to" +
                " automatically calculate iPixelScale for a display with " +
                "width [%d] and height [%d], but could not find a suitable " +
                "preset.\n", CLASS, displayW, displayH);
    }
    private static String ErrStrFailedInitIncompatibleParams() {
        return String.format("");
    }
}
