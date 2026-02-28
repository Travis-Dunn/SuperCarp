package production.display;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import whitetail.utility.logging.LogLevel;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.Logger.LogSession;

public final class DisplayConfig {
    private static boolean init;
    private static int displayW = -1, displayH = -1;
    private static int windowW, windowH;
    private static int emulatedW, emulatedH;
    private static boolean fullscreen;
    private static boolean borderless;
    private static boolean vSync;
    private static int fpsTarget;
    private static int pixelScale;
    private static int offsetX, offsetY;

    public static boolean QueryDisplayResolution() {
        DisplayMode desktop = Display.getDesktopDisplayMode();
        if (desktop == null) {
            LogFatalAndExit(ERR_STR_LIB_CALL_FAILED);
            return false;
        }
        displayW = desktop.getWidth();
        displayH = desktop.getHeight();
        return true;
    }

    /**
     * One-shot. Derives any missing display parameters from the given
     * props according to its mode, then stores the results.
     * {@code QueryDisplayResolution} must have succeeded before calling.
     */
    public static boolean ApplyDisplayProps(DisplayProps p) {
        assert(!init);
        assert(displayW != -1);
        assert(displayH != -1);

        LogSession(LogLevel.DEBUG, CLASS + " initializing...\n");

        String err;

        if ((err = DisplayProps.Validate(p)) != null) {
            LogFatalAndExit(err);
            return init = false;
        }

        switch(p.mode) {
            case DisplayProps.MODE_FULLSCREEN_PIXEL: {
                fullscreen = true;
                borderless = false;
                pixelScale = p.pixelScale;
                vSync = p.vSync;
                fpsTarget = p.fpsTarget;
                windowW = displayW;
                windowH = displayH;
                emulatedW = windowW / pixelScale;
                emulatedH = windowH / pixelScale;
                CalcOffsets();
                LogSession(LogLevel.DEBUG, ErrStrInit(p.mode));
                return init = true;
            }
            case DisplayProps.MODE_FULLSCREEN_EMULATED: {
                fullscreen = true;
                borderless = false;
                vSync = p.vSync;
                fpsTarget = p.fpsTarget;
                windowW = displayW;
                windowH = displayH;
                emulatedW = p.emulatedW;
                emulatedH = p.emulatedH;
                pixelScale = Math.min((windowW / emulatedW),
                        (windowH / emulatedH));
                CalcOffsets();
                LogSession(LogLevel.DEBUG, ErrStrInit(p.mode));
                return init = true;
            }
            case DisplayProps.MODE_WINDOWED_PIXEL_WINDOW: {
                fullscreen = false;
                borderless = p.borderless;
                vSync = p.vSync;
                fpsTarget = p.fpsTarget;
                windowW = p.windowW;
                windowH = p.windowH;
                pixelScale = p.pixelScale;
                emulatedW = windowW / pixelScale;
                emulatedH = windowH / pixelScale;
                CalcOffsets();
                LogSession(LogLevel.DEBUG, ErrStrInit(p.mode));
                return init = true;
            }
            case DisplayProps.MODE_WINDOWED_PIXEL_EMULATED: {
                fullscreen = false;
                borderless = p.borderless;
                vSync = p.vSync;
                fpsTarget = p.fpsTarget;
                pixelScale = p.pixelScale;
                emulatedW = p.emulatedW;
                emulatedH = p.emulatedH;
                windowW = pixelScale * emulatedW;
                windowH = pixelScale * emulatedH;
                CalcOffsets();
                LogSession(LogLevel.DEBUG, ErrStrInit(p.mode));
                return init = true;
            }
            case DisplayProps.MODE_WINDOWED_WINDOW_EMULATED: {
                fullscreen = false;
                borderless = p.borderless;
                vSync = p.vSync;
                fpsTarget = p.fpsTarget;
                emulatedW = p.emulatedW;
                emulatedH = p.emulatedH;
                windowW = p.windowW;
                windowH = p.windowH;
                pixelScale = Math.min((windowW / emulatedW),
                        (windowH / emulatedH));
                CalcOffsets();
                LogSession(LogLevel.DEBUG, ErrStrInit(p.mode));
                return init = true;
            }
            case DisplayProps.MODE_FULLSCREEN_SPECIFY_ALL: {
                fullscreen = true;
                borderless = false;
                vSync = p.vSync;
                fpsTarget = p.fpsTarget;
                emulatedW = p.emulatedW;
                emulatedH = p.emulatedH;
                windowW = p.windowW;
                windowH = p.windowH;
                pixelScale = p.pixelScale;
                CalcOffsets();
                LogSession(LogLevel.DEBUG, ErrStrInit(p.mode));
                return init = true;
            }
            case DisplayProps.MODE_WINDOWED_SPECIFY_ALL: {
                fullscreen = false;
                borderless = p.borderless;
                vSync = p.vSync;
                fpsTarget = p.fpsTarget;
                emulatedW = p.emulatedW;
                emulatedH = p.emulatedH;
                windowW = p.windowW;
                windowH = p.windowH;
                pixelScale = p.pixelScale;
                CalcOffsets();
                LogSession(LogLevel.DEBUG, ErrStrInit(p.mode));
                return init = true;
            }
            default: {
                LogFatalAndExit(ErrStrFailedSetMode(p.mode));
                return init = false;
            }
        }
    }

    /**
     * Letterbox/pillarbox offsets to center the emulated area in the window.
     */
    private static void CalcOffsets() {
        assert(windowW > 0 && windowH > 0 && emulatedW > 0 && emulatedH > 0 &&
                pixelScale > 0);

        offsetX = (windowW - (emulatedW * pixelScale)) / 2;
        offsetY = (windowH - (emulatedH * pixelScale)) / 2;
    }

    public static int WindowToFramebufferX(int windowX) {
        assert(init);

        return (windowX - offsetX) / pixelScale;
    }

    /**
     * Flips and converts y.
     * @param windowY   bottom origin window y coordinate
     * @return          framebuffer y
     */
    public static int WindowToFramebufferY(int windowY) {
        assert(init);

        return ((windowH - windowY) - offsetY) / pixelScale;
    }

    public static int FramebufferToViewportX(int fbX) {
        assert(init);

        return fbX - FramebufferConfig.GetViewportBounds()[0];
    }

    public static int FramebufferToViewportY(int fbY) {
        assert(init);

        return fbY - FramebufferConfig.GetViewportBounds()[1];
    }

    public static int GetDisplayW() { assert(displayW != -1); return displayW; }
    public static int GetDisplayH() { assert(displayH != -1); return displayH; }
    public static int GetWindowW()          { assert(init); return windowW; }
    public static int GetWindowH()          { assert(init); return windowH; }
    public static int GetEmulatedW()        { assert(init); return emulatedW; }
    public static int GetEmulatedH()        { assert(init); return emulatedH; }
    public static boolean IsFullscreen()    { assert(init); return fullscreen; }
    public static boolean IsBorderless()    { assert(init); return borderless; }
    public static boolean IsVSync()         { assert(init); return vSync; }
    public static int GetPixelScale()       { assert(init); return pixelScale; }
    public static int GetFpsTarget()        { assert(init); return fpsTarget; }
    public static int GetOffsetX()          { assert(init); return offsetX; }
    public static int GetOffsetY()          { assert(init); return offsetY; }

    public static final String CLASS = DisplayConfig.class.getSimpleName();
    private static final String ERR_STR_LIB_CALL_FAILED = CLASS + " failed " +
            "to query display resolution because " +
            "lwjgl.opengl.Display.getDesktopDisplayMode returned null.\n";
    private static String ErrStrFailedSetMode(int mode) {
        return String.format("%s failed to set display properties because an " +
               "unrecognized mode [%d] was encountered.\n", CLASS, mode);
    }
    private static String ErrStrInit(int mode) {
        return String.format("%s initialized with mode [%d], " +
                "windowW [%d], windowH [%d], emulatedW [%d], emulatedH [%d] " +
                "pixel scale [%d], fullscreen [%B], borderless [%B], " +
                "vSync [%B], fps target [%d], offsetX [%d], offsetY [%d].\n",
                CLASS, mode, windowW, windowH, emulatedW, emulatedH, pixelScale,
                fullscreen, borderless, vSync, fpsTarget, offsetX, offsetY);
    }

}