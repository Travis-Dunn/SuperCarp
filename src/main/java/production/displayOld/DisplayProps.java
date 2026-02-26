package production.displayOld;

public final class DisplayProps {
    final int mode;
    final boolean borderless;
    final boolean vSync;
    final int fpsTarget;
    final int pixelScale;
    final int emulatedW;
    final int emulatedH;
    final int windowW;
    final int windowH;

    /*
        IMPORTANT:
        When you add a mode, update Validate() and ErrStrInvalidMode()!
    */

    /* Calculate emulated resolution given pixel scale */
    public static final int MODE_FULLSCREEN_PIXEL = 0;
    /* Calculate pixel scale given emulated resolution */
    public static final int MODE_FULLSCREEN_EMULATED = 1;
    /* Calculate emulated resolution given pixel scale and window resolution */
    public static final int MODE_WINDOWED_PIXEL_WINDOW = 2;
    /* Calculate window resolution given pixel scale and emulated resolution */
    public static final int MODE_WINDOWED_PIXEL_EMULATED = 3;
    /* Calculate pixel scale given window and emulated resolution */
    public static final int MODE_WINDOWED_WINDOW_EMULATED = 4;
    /* Specify emulated and window resolution, and pixel scale */
    public static final int MODE_FULLSCREEN_SPECIFY_ALL = 5;
    /* Specify emulated and window resolution, and pixel scale */
    public static final int MODE_WINDOWED_SPECIFY_ALL = 6;

    /**
     * Construct a set of display properties to be passed to
     * {@code DisplayConfig.ApplyDisplayProps()}. The {@code mode} parameter
     * determines
     * which fields are inputs and which will be derived during
     * initialization. Fields that are to be derived must be set to zero;
     * input fields must be positive.
     *
     * <p>In fullscreen modes, window dimensions are not used. In windowed
     * modes, the screen resolution is queried internally but is not
     * specified here.</p>
     *
     * <p><b>Mode summary — fields marked (in) are inputs, (out) are
     * derived:</b></p>
     * <pre>
     * Mode                        pixelScale  emulatedW/H  windowW/H
     * ---------------------------------------------------------------
     * FULLSCREEN_PIXEL            in          out          -
     * FULLSCREEN_EMULATED         out         in           -
     * WINDOWED_PIXEL_WINDOW       in          out          in
     * WINDOWED_PIXEL_EMULATED     in          in           out
     * WINDOWED_WINDOW_EMULATED    out         in           in
     * FULLSCREEN_SPECIFY_ALL      in          in           in
     * WINDOWED_SPECIFY_ALL        in          in           in
     * </pre>
     *
     * <p>{@code vSync} and {@code fpsTarget} are independent of mode. If
     * {@code vSync} is true, {@code fpsTarget} must be zero. If
     * {@code vSync} is false, {@code fpsTarget} may be zero for uncapped
     * framerate, or positive for a target.</p>
     *
     * @param mode       one of the {@code MODE_} constants
     * @param borderless whether to draw an undecorated window
     * @param vSync      whether to enable vertical sync
     * @param fpsTarget  target framerate; 0 for uncapped or if vSync is on
     * @param pixelScale ratio of screen pixels to emulated pixels
     * @param emulatedW  width of the internal framebuffer in emulated pixels
     * @param emulatedH  height of the internal framebuffer in emulated pixels
     * @param windowW    window width in screen pixels
     * @param windowH    window height in screen pixels
     */
    public DisplayProps(int mode,
                        boolean borderless,
                        boolean vSync,
                        int fpsTarget,
                        int pixelScale,
                        int emulatedW,
                        int emulatedH,
                        int windowW,
                        int windowH) {
        this.mode = mode;
        this.borderless = borderless;
        this.vSync = vSync;
        this.fpsTarget = fpsTarget;
        this.pixelScale = pixelScale;
        this.emulatedW = emulatedW;
        this.emulatedH = emulatedH;
        this.windowW = windowW;
        this.windowH = windowH;
    }

    static String Validate(DisplayProps p) {
        String err;

        if (p == null) return ERR_STR_PROPS_NULL;

        if ((err = ValidateFramerate(p)) != null) return err;

        switch (p.mode) {
            case MODE_FULLSCREEN_PIXEL: {
                if (!p.borderless && p.pixelScale > 0 && p.emulatedW == 0
                        && p.emulatedH == 0 && p.windowW == 0 && p.windowH == 0)
                    return null;
                return ErrStrInvalidCfgFullscreenPixel(p);
            }
            case MODE_FULLSCREEN_EMULATED: {
                if (!p.borderless && p.pixelScale == 0 && p.emulatedW > 0
                        && p.emulatedH > 0 && p.windowW == 0 && p.windowH == 0)
                    return null;
                return ErrStrInvalidCfgFullscreenEmulated(p);
            }
            case MODE_WINDOWED_PIXEL_WINDOW: {
                if (p.pixelScale > 0 && p.emulatedW == 0
                        && p.emulatedH == 0 && p.windowW > 0 && p.windowH > 0)
                    return null;
                return ErrStrInvalidCfgWindowedPixelWindow(p);
            }
            case MODE_WINDOWED_PIXEL_EMULATED: {
                if (p.pixelScale > 0 && p.emulatedW > 0
                        && p.emulatedH > 0 && p.windowW == 0 && p.windowH == 0)
                    return null;
                return ErrStrInvalidCfgWindowedPixelEmulated(p);
            }
            case MODE_WINDOWED_WINDOW_EMULATED: {
                if (p.pixelScale == 0 && p.emulatedW > 0
                        && p.emulatedH > 0 && p.windowW > 0 && p.windowH > 0)
                    return null;
                return ErrStrInvalidCfgWindowedWindowEmulated(p);
            }
            case MODE_FULLSCREEN_SPECIFY_ALL: {
                if (!p.borderless && p.pixelScale > 0 && p.emulatedW > 0
                        && p.emulatedH > 0 && p.windowW > 0 && p.windowH > 0)
                    return null;
                return ErrStrInvalidCfgFullscreenSpecifyAll(p);
            }
            case MODE_WINDOWED_SPECIFY_ALL: {
                if (p.pixelScale > 0 && p.emulatedW > 0
                        && p.emulatedH > 0 && p.windowW > 0 && p.windowH > 0)
                    return null;
                return ErrStrInvalidCfgWindowedSpecifyAll(p);
            }
        }
        return ErrStrInvalidMode(p.mode);
    }

    private static String ValidateFramerate(DisplayProps p) {
        if (p.fpsTarget < 0) return ErrStrFpsNeg(p);
        if (p.vSync && p.fpsTarget != 0) return ErrStrFpsMustBeZeroIfVSync(p);
        return null;
    }

    public static final String CLASS = DisplayProps.class.getSimpleName();
    private static final String ERR_STR_PROPS_NULL = CLASS + " failed " +
            "validation because it was null.\n";
    private static String ErrStrInvalidMode(int mode) {
        return String.format("%s failed validation because mode [%d] was " +
                "unrecognized. Valid modes:\n" +
                "0 - fullscreen:pixel\n" +
                "1 - fullscreen:emulated\n" +
                "2 - windowed:pixel/window\n" +
                "3 - windowed:pixel/emulated\n" +
                "4 - windowed:window/emulated\n" +
                "5 - fullscreen:specify_all\n" +
                "6 - windowed:specify_all\n", CLASS, mode);
    }
    private static String ErrStrInvalidCfgFullscreenPixel(DisplayProps p) {
        return String.format("%s failed validation in mode [fullscreen:pixel]" +
                ".\nBorderless must be FALSE, it was [%B].\n" +
                "Pixel scale must be greater than zero, it was [%d].\n" +
                "Emulated width must be zero, it was [%d].\n" +
                "Emulated height must be zero, it was [%d].\n" +
                "Window width must be zero, it was [%d].\n" +
                "Window height must be zero, it was [%d].\n", CLASS,
                p.borderless, p.pixelScale, p.emulatedW, p.emulatedH, p.windowW,
                p.windowH);
    }
    private static String ErrStrInvalidCfgFullscreenEmulated(DisplayProps p) {
        return String.format("%s failed validation in mode " +
                "[fullscreen:emulated].\n" +
                "Borderless must be FALSE, it was [%B].\n" +
                "Pixel scale must be zero, it was [%d].\n" +
                "Emulated width must be greater than zero, it was [%d].\n" +
                "Emulated height must be greater than zero, it was [%d].\n" +
                "Window width must be zero, it was [%d].\n" +
                "Window height must be zero, it was [%d].\n", CLASS,
                p.borderless, p.pixelScale, p.emulatedW, p.emulatedH, p.windowW,
                p.windowH);
    }
    private static String ErrStrInvalidCfgWindowedPixelWindow(DisplayProps p) {
        return String.format("%s failed validation in mode " +
                "[windowed:pixel/window].\n" +
                "Pixel scale must be greater than zero, it was [%d].\n" +
                "Emulated width must be zero, it was [%d].\n" +
                "Emulated height must be zero, it was [%d].\n" +
                "Window width must be greater than zero, it was [%d].\n" +
                "Window height must be greater than zero, it was [%d].\n", CLASS,
                p.pixelScale, p.emulatedW, p.emulatedH, p.windowW, p.windowH);
    }
    private static String ErrStrInvalidCfgWindowedPixelEmulated(DisplayProps p) {
        return String.format("%s failed validation in mode " +
                "[windowed:pixel/emulated].\n" +
                "Pixel scale must be greater than zero, it was [%d].\n" +
                "Emulated width must be greater than zero, it was [%d].\n" +
                "Emulated height must be greater than zero, it was [%d].\n" +
                "Window width must be zero, it was [%d].\n" +
                "Window height must be zero, it was [%d].\n", CLASS,
                p.pixelScale, p.emulatedW, p.emulatedH, p.windowW, p.windowH);
    }
    private static String ErrStrInvalidCfgWindowedWindowEmulated(DisplayProps p) {
        return String.format("%s failed validation in mode " +
                "[windowed:window/emulated].\n" +
                "Pixel scale must be zero, it was [%d].\n" +
                "Emulated width must be greater than zero, it was [%d].\n" +
                "Emulated height must be greater than zero, it was [%d].\n" +
                "Window width must be greater than zero, it was [%d].\n" +
                "Window height must be greater than zero, it was [%d].\n", CLASS,
                p.pixelScale, p.emulatedW, p.emulatedH, p.windowW, p.windowH);
    }
    private static String ErrStrInvalidCfgFullscreenSpecifyAll(DisplayProps p) {
        return String.format("%s failed validation in mode " +
                "[fullscreen:specify_all].\n" +
                "Borderless must be FALSE, it was [%B].\n" +
                "Pixel scale must be greater than zero, it was [%d].\n" +
                "Emulated width must be greater than zero, it was [%d].\n" +
                "Emulated height must be greater than zero, it was [%d].\n" +
                "Window width must be greater than zero, it was [%d].\n" +
                "Window height must be greater than zero, it was [%d].\n",
                CLASS, p.borderless, p.pixelScale, p.emulatedW, p.emulatedH,
                p.windowW, p.windowH);
    }
    private static String ErrStrInvalidCfgWindowedSpecifyAll(DisplayProps p) {
        return String.format("%s failed validation in mode " +
                "[windowed:specify_all].\n" +
                "Pixel scale must be greater than zero, it was [%d].\n" +
                "Emulated width must be greater than zero, it was [%d].\n" +
                "Emulated height must be greater than zero, it was [%d].\n" +
                "Window width must be greater than zero, it was [%d].\n" +
                "Window height must be greater than zero, it was [%d].\n",
                CLASS, p.pixelScale, p.emulatedW, p.emulatedH, p.windowW,
                p.windowH);
    }
    private static String ErrStrFpsMustBeZeroIfVSync(DisplayProps p) {
        return String.format("%s failed validation because vSync was TRUE, " +
                "and fpsTarget [%d] was non-zero", CLASS, p.fpsTarget);
    }
    private static String ErrStrFpsNeg(DisplayProps p) {
        return String.format("%s failed validation because fpsTarget [%d] was" +
                " negative.\n", CLASS, p.fpsTarget);
    }
}