package production;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import whitetail.utility.logging.LogLevel;

import static whitetail.utility.logging.Logger.LogSession;

/**
 * Computes and holds display configuration based on monitor capabilities
 * and user preferences from config file.
 *
 * Initialize after config file is parsed, before engine second half init.
 *
 * Coordinate spaces:
 *   Window   - physical pixels on screen
 *   FB       - framebuffer (logical render target)
 *   Viewport - game world area within framebuffer
 *   UI       - chrome/HUD areas within framebuffer (outside viewport)
 */
public final class DisplayConfigOld {
    private static boolean init;

    /* --- Monitor (read-only, queried) --- */
    private static int monitorW;
    private static int monitorH;

    /* --- Window (physical) --- */
    private static int windowW;
    private static int windowH;
    private static boolean fullscreen;

    /* --- Framebuffer (logical) --- */
    private static int fbW;
    private static int fbH;
    private static int pixelScale;

    /* --- Viewport (game world area within FB) --- */
    private static int vpX;
    private static int vpY;
    private static int vpW;
    private static int vpH;

    /* --- Derived/cached --- */
    private static float vpAspect;

    /*
     * Supported base resolutions (framebuffer sizes).
     * All are 16:9. Game assets are designed for 480x270.
     */
    private static final int[][] BASE_RESOLUTIONS = {
            {480, 270},   // 1x at 480p, 4x at 1080p
            {640, 360},   // 3x at 1080p
            {960, 540},   // 2x at 1080p
    };

    /*
     * UI chrome sizes (pixels in FB space).
     * Right panel and bottom bar, like classic RuneScape.
     */
    private static final int UI_RIGHT_WIDTH = 96;   // right panel
    private static final int UI_BOTTOM_HEIGHT = 54; // bottom bar

    private DisplayConfigOld() {}

    /**
     * Initialize display configuration.
     * Call after config file is parsed.
     *
     * @param cfgFullscreen user preference for fullscreen
     * @param cfgPixelScale user preference for pixel scale (0 = auto)
     * @return true if successful
     */
    public static boolean Init(boolean cfgFullscreen, int cfgPixelScale) {
        assert(!init);

        /* Query monitor */
        DisplayMode desktop = Display.getDesktopDisplayMode();
        monitorW = desktop.getWidth();
        monitorH = desktop.getHeight();

        LogSession(LogLevel.DEBUG, CLASS + " detected monitor: " +
                monitorW + "x" + monitorH);

        fullscreen = cfgFullscreen;

        /* Determine pixel scale */
        if (cfgPixelScale > 0) {
            pixelScale = cfgPixelScale;
        } else {
            pixelScale = computeAutoPixelScale();
        }

        /* Compute framebuffer size */
        fbW = BASE_RESOLUTIONS[0][0];  // 480
        fbH = BASE_RESOLUTIONS[0][1];  // 270

        /* Compute window size */
        if (fullscreen) {
            windowW = monitorW;
            windowH = monitorH;
            /* In fullscreen, we might want to adjust pixel scale
               to best fit monitor */
            pixelScale = Math.min(monitorW / fbW, monitorH / fbH);
        } else {
            windowW = fbW * pixelScale;
            windowH = fbH * pixelScale;

            /* Clamp to monitor size with margin */
            int maxW = monitorW - 100;
            int maxH = monitorH - 100;
            while (windowW > maxW || windowH > maxH) {
                pixelScale--;
                if (pixelScale < 1) {
                    pixelScale = 1;
                    windowW = Math.min(fbW, maxW);
                    windowH = Math.min(fbH, maxH);
                    break;
                }
                windowW = fbW * pixelScale;
                windowH = fbH * pixelScale;
            }
        }

        /* Compute viewport (game area) within framebuffer */
        vpX = 0;
        vpY = 0;
        vpW = fbW - UI_RIGHT_WIDTH;
        vpH = fbH - UI_BOTTOM_HEIGHT;

        vpAspect = (float) vpW / (float) vpH;

        LogSession(LogLevel.DEBUG, CLASS + " configured:" +
                " window=" + windowW + "x" + windowH +
                " fb=" + fbW + "x" + fbH +
                " viewport=" + vpW + "x" + vpH + " at (" + vpX + "," + vpY + ")" +
                " scale=" + pixelScale +
                " fullscreen=" + fullscreen);

        return init = true;
    }

    private static int computeAutoPixelScale() {
        /* Target: largest integer scale that fits monitor with some margin */
        int baseW = BASE_RESOLUTIONS[0][0];
        int baseH = BASE_RESOLUTIONS[0][1];

        int maxScaleW = (monitorW - 100) / baseW;
        int maxScaleH = (monitorH - 100) / baseH;

        int scale = Math.min(maxScaleW, maxScaleH);
        return Math.max(1, scale);
    }

    /* --- Coordinate conversion utilities --- */

    /**
     * Convert window coordinates to framebuffer coordinates.
     */
    public static int windowToFbX(int windowX) {
        assert(init);
        return (windowX * fbW) / windowW;
    }

    public static int windowToFbY(int windowY) {
        assert(init);
        /* Window Y is bottom-up (LWJGL), FB Y is top-down */
        return ((windowH - windowY) * fbH) / windowH;
    }

    /**
     * Check if framebuffer coordinates are within the game viewport.
     */
    public static boolean isInViewport(int fbX, int fbY) {
        assert(init);
        return fbX >= vpX && fbX < vpX + vpW &&
                fbY >= vpY && fbY < vpY + vpH;
    }

    /**
     * Convert framebuffer coordinates to viewport-relative coordinates.
     * Only valid if isInViewport() is true.
     */
    public static int fbToViewportX(int fbX) {
        assert(init);
        return fbX - vpX;
    }

    public static int fbToViewportY(int fbY) {
        assert(init);
        return fbY - vpY;
    }

    /* --- Getters --- */

    public static int getMonitorW()   { assert(init); return monitorW; }
    public static int getMonitorH()   { assert(init); return monitorH; }
    public static int getWindowW()    { assert(init); return windowW; }
    public static int getWindowH()    { assert(init); return windowH; }
    public static int getFbW()        { assert(init); return fbW; }
    public static int getFbH()        { assert(init); return fbH; }
    public static int getPixelScale() { assert(init); return pixelScale; }
    public static int getVpX()        { assert(init); return vpX; }
    public static int getVpY()        { assert(init); return vpY; }
    public static int getVpW()        { assert(init); return vpW; }
    public static int getVpH()        { assert(init); return vpH; }
    public static float getVpAspect() { assert(init); return vpAspect; }
    public static boolean isFullscreen() { assert(init); return fullscreen; }

    public static final String CLASS = DisplayConfigOld.class.getSimpleName();
}