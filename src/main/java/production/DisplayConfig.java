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
    /* 'framebuffer' Refers to the software emulated sub-scale framebuffer */
    private static int framebufferW, framebufferH;
    /* 'viewport' Refers to the "game window", I.E., not the space in the window
        dedicated to the UI */
    private static int viewportW, viewportH;
    /* Ratio of display pixels to framebuffer pixels */
    private static int iPixelScale;
    private static boolean bFullscreen;

    /**
     * Ingests requested display properties (Probably from config file), queries
     * physical display information from OS, and uses them to determine the
     * actual display properties.
     *
     * @param bFullscreen true if fullscreen is desired
     * @param iPixelScale ratio of screen pixels to framebuffer pixels
     * @return true is success
     */
    public static boolean Init(boolean bFullscreen, int iPixelScale) {
        assert(!init);

        DisplayMode desktop = Display.getDesktopDisplayMode();
        displayW = desktop.getWidth();
        displayH = desktop.getHeight();

        if (!(iPixelScale >= 0)) {
            LogFatalAndExit(ERR_STR_INIT_FAILED_PIXEL_SCALE_NEG);
            return false;
        } else if (iPixelScale == 0) {
            DisplayConfig.iPixelScale = AutoCalcPixelScale();
        } else if (displayW % iPixelScale != 0 || displayH % iPixelScale != 0) {
            LogFatalAndExit(ErrStrInitFailedPixelScale());
            return false;
        } else {
            DisplayConfig.iPixelScale = iPixelScale;
        }

        LogSession(LogLevel.DEBUG, LogStrDetectedMonitor());

        /* TODO: add "starting..." mesgs and calc the other vals */

        DisplayConfig.bFullscreen = bFullscreen;

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

    private static final String CLASS= DisplayConfig.class.getSimpleName();
    private static String LogStrDetectedMonitor() {
        return String.format("%s detected a display with width [%d] and " +
                "height [%d].\n", CLASS, displayW, displayH);
    }
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
}
