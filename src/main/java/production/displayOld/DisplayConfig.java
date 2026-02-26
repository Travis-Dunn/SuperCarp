package production.displayOld;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;

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
     *  Switch on mode, copy/calculate values as appropriate
     *  However, we'll always need to calculate the offsetX/Y regardless of mode
     *  Also need to add the fields and getters
     *  Finally, wire it up and cut out the old stuff
     *  Including conversion methods for the mouse, and letterboxing/
     *  pillarboxing in SpriteBackend
     */
    public static boolean ApplyDisplayProps(DisplayProps p) {
        assert(!init);
        assert(displayW != -1);
        assert(displayH != -1);

        String err;

        if ((err = DisplayProps.Validate(p)) != null) {
            LogFatalAndExit(err);
            return init =false;
        }

        switch(p.mode) {
            case DisplayProps.MODE_FULLSCREEN_PIXEL: {
                fullscreen = true;
                borderless = false; /* undecorated is n/a for fullscreen */
                pixelScale = p.pixelScale;
                vSync = p.vSync;
                fpsTarget = p.fpsTarget;
                windowW = displayW;
                windowH = displayH;
                emulatedW = windowW / pixelScale;
                emulatedH = windowH / pixelScale;
                CalcOffsets();
                return init = true;
            }
            case DisplayProps.MODE_FULLSCREEN_EMULATED: {
                fullscreen = true;
                borderless = false; /* undecorated is n/a for fullscreen */
                vSync = p.vSync;
                fpsTarget = p.fpsTarget;
                windowW = displayW;
                windowH = displayH;
                emulatedW = p.emulatedW;
                emulatedH = p.emulatedH;
                pixelScale = Math.min((windowW / emulatedW),
                        (windowH / emulatedH));
                CalcOffsets();
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
                return init = true;
            }
            case DisplayProps.MODE_FULLSCREEN_SPECIFY_ALL: {
                fullscreen = true;
                borderless = false; /* undecorated is n/a for fullscreen */
                vSync = p.vSync;
                fpsTarget = p.fpsTarget;
                emulatedW = p.emulatedW;
                emulatedH = p.emulatedH;
                windowW = p.windowW;
                windowH = p.windowH;
                pixelScale = p.pixelScale;
                CalcOffsets();
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
                return init = true;
            }
            default: {
                LogFatalAndExit(ErrStrFailedSetMode(p.mode));
                return init = false;
            }
        }
    }

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

    public static int WindowToFramebufferY(int windowY) {
        assert(init);

        /* LWJGL uses bottom left origin */
        return ((windowH - windowY) - offsetY) / pixelScale;
    }

    public static int FramebufferToViewportX(int fbX, int vpLeftBound) {
        assert(init);

        return fbX - vpLeftBound;
    }

    public static int FramebufferToViewportY(int fbY, int vpTopBound) {
        assert(init);

        return fbY - vpTopBound;
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

    private static final String CLASS = DisplayConfig.class.getSimpleName();
    private static final String ERR_STR_LIB_CALL_FAILED = CLASS + " failed " +
            "to query display resolution because " +
            "lwjgl.opengl.Display.getDesktopDisplayMode returned null.\n";
    private static String ErrStrFailedSetMode(int mode) {
        return String.format("%s failed to set display properties because an " +
                "unrecognized mode [%d] was encountered.\n", CLASS, mode);
    }
}
