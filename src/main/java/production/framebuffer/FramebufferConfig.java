package production.framebuffer;

import production.displayOld.DisplayProps;

import java.util.HashMap;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;

public final class FramebufferConfig {
    private static boolean init;
    private static int viewportW = -1, viewportH = -1;
    private static int viewportBounds[];

    private static HashMap<Integer, FramebufferPreset> presets;
    private static FramebufferPreset presetsArr[];

    public static DisplayProps CreateDisplayProps(
            int displayW,
            int displayH,
            boolean borderless,
            boolean fullscreen,
            boolean vSync,
            int fpsTarget) {
        /*
            Right now we only support two modes: fullscreen/auto, and
            windowed/auto. TODO: Add support for windowed/manual mode.
            In the in-game settings menu, if windowed/manual is selected, there
            will be two more settings: aspect ratio and pixel ratio.
            Aspect ratio will default to [native], I.E., the display's aspect
            ratio, but can be set to any of the aspect ratios present in
            'presets'. Pixel ratio will default to whatever it would be in
            windowed/auto, but can be set to any value between that and 1.
            The point of this is principally to be able to render the game in a
            small window as "Second monitor content", while maintaining pixel-
            perfect graphics.
         */
        assert(!init);

        int mode, pixelScale, emulatedW, emulatedH, windowW, windowH, scale;
        boolean _borderless;
        FramebufferPreset cfg;

        if ((cfg = presets.get(PackRes(displayW, displayH))) == null) {
            if ((cfg = FindBest(displayW, displayH)) == null) {
                LogFatalAndExit(ErrStrNoViableCfg(displayW, displayH));
                return null;
            }
        }

        emulatedW = cfg.emulatedW;
        emulatedH = cfg.emulatedH;
        SetViewport(cfg);

        scale = Math.min(displayW / emulatedW, displayH / emulatedH);

        if (fullscreen) {
            mode = DisplayProps.MODE_FULLSCREEN_SPECIFY_ALL;
            pixelScale = scale;
            windowW = displayW;
            windowH = displayH;
            _borderless = false;
        } else {
            mode = DisplayProps.MODE_WINDOWED_SPECIFY_ALL;
            pixelScale = Math.max(1, scale - 1);
            windowW = pixelScale * emulatedW;
            windowH = pixelScale * emulatedH;
            _borderless = borderless;
        }

        init = true;

        return new DisplayProps(mode, _borderless, vSync,
                (vSync ? 0 : fpsTarget), pixelScale, emulatedW, emulatedH,
                windowW, windowH);
    }

    private static FramebufferPreset FindBest(int displayW, int displayH) {
        FramebufferPreset cfg0, cfg1 = null;
        int i, scale, area0, area1 = 0;

        for (i = 0; i < presetsArr.length; ++i) {
            cfg0 = presetsArr[i];

            scale = Math.min(displayW / cfg0.emulatedW,
                    displayH / cfg0.emulatedH);

            if ((scale > 0) && (area0 = (scale * cfg0.emulatedW) *
                    (scale * cfg0.emulatedH)) > area1) {
                area1 = area0;
                cfg1 = cfg0;
            }
        }
        return cfg1;
    }

    private static void SetViewport(FramebufferPreset cfg) {
        viewportW = cfg.viewportW;
        viewportH = cfg.viewportH;
        viewportBounds[0] = cfg.viewportX;
        viewportBounds[1] = cfg.viewportY;
        viewportBounds[2] = cfg.viewportX + cfg.viewportW;
        viewportBounds[3] = cfg.viewportY + cfg.viewportH;
    }

    private static int PackRes(int w, int h) {
        return ((w & 0xFFFF) << 16) | (h & 0xFFFF);
    }

    public static boolean InViewport(int fbX, int fbY) {
        assert(init);

        return fbX >= viewportBounds[0] && fbX < viewportBounds[2] &&
                fbY >= viewportBounds[1] && fbY < viewportBounds[3];
    }

    public static int GetViewportW()    { assert(init); return viewportW; }
    public static int GetViewportH()    { assert(init); return viewportH; }
    public static int[] GetViewport()   { assert(init); return viewportBounds; }

    static {
        viewportBounds = new int[4];
        presets = new HashMap<Integer, FramebufferPreset>();

        presetsArr = new FramebufferPreset[] {
                new FramebufferPreset(480, 270,
                        320, 180, 4, 4),
                new FramebufferPreset(512, 288,
                        340, 192, 4, 4)
        };

        presets.put(PackRes(1920, 1080), presetsArr[0]);
        presets.put(PackRes(2560, 1440), presetsArr[1]);
        presets.put(PackRes(3840, 2160), presetsArr[0]);
    }

    public static final String CLASS = FramebufferConfig.class.getSimpleName();
    private static String ErrStrNoViableCfg(int displayW, int displayH) {
        return String.format("%s failed to find a viable %s for displayW [%d]" +
                ", displayH [%d].\n", CLASS, FramebufferPreset.CLASS, displayW,
                displayH);
    }
}
