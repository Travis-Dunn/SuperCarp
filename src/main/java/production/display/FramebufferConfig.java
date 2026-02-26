package production.display;

import whitetail.utility.logging.LogLevel;

import java.util.HashMap;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.ErrorStrings.ERR_STR_FAILED_INIT_OOM;
import static whitetail.utility.logging.Logger.LogSession;

public final class FramebufferConfig {
    private static boolean init;
    private static boolean resolved;
    private static FramebufferPreset[] presets;
    private static ViewportPreset[] viewports;
    private static HashMap<Integer, Integer> resolutionMap;
    private static boolean allowFallback;
    private static int viewportBounds[];

    /**
     * One-shot. Registers preset data for later resolution.
     *
     * @param presets       non-empty array of emulated resolutions
     * @param viewports     parallel to presets; nullable (full emulated area
     *                      used for any null element, or if the array itself
     *                      is null)
     * @param resMap        each entry is {displayW, displayH, presetIndex}
     * @param allowFallback if true, an unlisted resolution will use the
     *                      preset that covers the most screen area at
     *                      integer scale; if false, unlisted is fatal
     */
    public static boolean Init(
            FramebufferPreset[] presets,
            ViewportPreset[] viewports,
            int[][] resMap,
            boolean allowFallback) {
        assert(!init);

        LogSession(LogLevel.DEBUG, CLASS + " initializing...\n");

        String err;
        int i;
        if ((err = Validate(presets, viewports, resMap)) != null) {
            LogFatalAndExit(err);
            return init = false;
        }

        FramebufferConfig.presets = presets;
        FramebufferConfig.viewports = viewports;
        FramebufferConfig.allowFallback = allowFallback;

        try {
            FramebufferConfig.resolutionMap = new HashMap<Integer, Integer>();
            viewportBounds = new int[4];
        } catch (OutOfMemoryError e) {
            LogFatalAndExit(CLASS + ERR_STR_FAILED_INIT_OOM);
            return init = false;
        }

        for (i = 0; i < resMap.length; ++i) {
            FramebufferConfig.resolutionMap.put(
                    PackRes(resMap[i][0], resMap[i][1]),
                    resMap[i][2]);
        }

        LogSession(LogLevel.DEBUG, CLASS + " initialized with ["
                + presets.length + "] presets, ["
                + (viewports != null ? viewports.length : 0)
                + "] viewports, [" + resMap.length
                + "] resolution mappings, allowFallback ["
                + allowFallback + "].\n");

        return init = true;
    }

    /**
     * One-shot. Looks up or computes the best preset for the given display
     * resolution and sets the viewport bounds. Must be called after
     * {@code Init} and before {@code BuildDisplayProps}.
     *
     * @return the matched preset, or null on fatal failure
     */
    public static FramebufferPreset ResolvePreset(
            int displayW, int displayH) {
        assert(init);
        assert(!resolved);

        LogSession(LogLevel.DEBUG, CLASS + " resolving preset for displayW ["
                + displayW + "], displayH [" + displayH + "]...\n");

        int index = -1;
        Integer mapped = resolutionMap.get(PackRes(displayW, displayH));

        if (mapped != null) {
            index = mapped;
        } else if (allowFallback) {
            index = FindBest(displayW, displayH);
        }

        if (index == -1) {
            LogFatalAndExit(ErrStrNoViablePreset(displayW, displayH));
            return null;
        }

        FramebufferPreset preset = presets[index];
        SetViewport(preset, viewports != null ? viewports[index] : null);
        resolved = true;

        LogSession(LogLevel.DEBUG, CLASS + " resolved preset index ["
                + index + "] with emulatedW [" + preset.emulatedW
                + "], emulatedH [" + preset.emulatedH
                + "], viewport [" + viewportBounds[0] + ", "
                + viewportBounds[1] + ", " + viewportBounds[2] + ", "
                + viewportBounds[3] + "].\n");

        return preset;
    }

    /**
     * Constructs a {@code DisplayProps} from the resolved preset.
     * In windowed mode, scale is reduced by one (minimum 1) so the
     * window does not fill the entire screen.
     */
    public static DisplayProps BuildDisplayProps(
            FramebufferPreset preset,
            int displayW,
            int displayH,
            boolean borderless,
            boolean fullscreen,
            boolean vSync,
            int fpsTarget) {
        assert(init);
        assert(resolved);

        int mode, pixelScale, windowW, windowH;
        int scale = Math.min(displayW / preset.emulatedW,
                displayH / preset.emulatedH);

        if (fullscreen) {
            mode = DisplayProps.MODE_FULLSCREEN_SPECIFY_ALL;
            pixelScale = scale;
            windowW = displayW;
            windowH = displayH;
            borderless = false;
        } else {
            mode = DisplayProps.MODE_WINDOWED_SPECIFY_ALL;
            pixelScale = Math.max(1, scale - 1);
            windowW = pixelScale * preset.emulatedW;
            windowH = pixelScale * preset.emulatedH;
        }

        return new DisplayProps(mode, borderless, vSync,
                (vSync ? 0 : fpsTarget), pixelScale,
                preset.emulatedW, preset.emulatedH,
                windowW, windowH);
    }

    /**
     * Selects the preset whose scaled area is largest at integer scale.
     * On tie, the earlier preset wins.
     */
    private static int FindBest(int displayW, int displayH) {
        int scale, i, bestIndex = -1;
        int area, bestArea = 0;
        FramebufferPreset p;

        for (i = 0; i < presets.length; ++i) {
            p = presets[i];
            scale = Math.min(displayW / p.emulatedW,
                    displayH / p.emulatedH);

            if (scale > 0) {
                area = (scale * p.emulatedW) * (scale * p.emulatedH);
                if (area > bestArea) {
                    bestArea = area;
                    bestIndex = i;
                }
            }
        }
        return bestIndex;
    }

    private static void SetViewport(FramebufferPreset preset,
                                    ViewportPreset vp) {
        if (vp != null) {
            viewportBounds[0] = vp.x;
            viewportBounds[1] = vp.y;
            viewportBounds[2] = vp.x + vp.w;
            viewportBounds[3] = vp.y + vp.h;
        } else {
            viewportBounds[0] = 0;
            viewportBounds[1] = 0;
            viewportBounds[2] = preset.emulatedW;
            viewportBounds[3] = preset.emulatedH;
        }
    }

    /** Packs two dimensions into one int. Both must fit in 16 bits. */
    private static int PackRes(int w, int h) {
        return ((w & 0xFFFF) << 16) | (h & 0xFFFF);
    }

    public static boolean InViewport(int fbX, int fbY) {
        assert(init && resolved);

        return fbX >= viewportBounds[0] && fbX < viewportBounds[2] &&
                fbY >= viewportBounds[1] && fbY < viewportBounds[3];
    }

    public static int GetViewportW() {
        assert(init && resolved);

        return viewportBounds[2] - viewportBounds[0];
    }

    public static int GetViewportH() {
        assert(init && resolved);

        return viewportBounds[3] - viewportBounds[1];
    }

    public static int[] GetViewportBounds() {
        assert(init && resolved);

        return viewportBounds;
    }

    private static String Validate(
            FramebufferPreset[] presets,
            ViewportPreset[] viewports,
            int[][] resMap) {
        int i, idx;

        if (presets == null || presets.length == 0)
            return ERR_STR_PRESETS_EMPTY;
        if (viewports != null && viewports.length != presets.length)
            return ErrStrViewportLengthMismatch(presets.length, viewports.length);
        if (resMap == null)
            return ERR_STR_RES_MAP_NULL;

        for (i = 0; i < presets.length; ++i) {
            if (presets[i] == null)
                return ErrStrPresetNull(i);
            if (presets[i].emulatedW <= 0 || presets[i].emulatedH <= 0)
                return ErrStrPresetInvalid(i, presets[i]);
        }

        for (i = 0; i < (viewports != null ? viewports.length : 0); ++i) {
            if (viewports[i] == null) continue;
            ViewportPreset vp = viewports[i];
            FramebufferPreset fp = presets[i];
            if (vp.x < 0 || vp.y < 0 || vp.w <= 0 || vp.h <= 0)
                return ErrStrViewportInvalid(i, vp);
            if (vp.x + vp.w > fp.emulatedW || vp.y + vp.h > fp.emulatedH)
                return ErrStrViewportExceedsEmulated(i, vp, fp);
        }

        for (i = 0; i < resMap.length; ++i) {
            if (resMap[i] == null || resMap[i].length != 3)
                return ErrStrResMapEntryInvalid(i);
            if (resMap[i][0] <= 0 || resMap[i][1] <= 0)
                return ErrStrResMapDimsInvalid(i, resMap[i][0], resMap[i][1]);
            idx = resMap[i][2];
            if (idx < 0 || idx >= presets.length)
                return ErrStrResMapIndexOOB(i, idx, presets.length);
        }

        return null;
    }

    public static final String CLASS =
            FramebufferConfig.class.getSimpleName();
    private static final String ERR_STR_PRESETS_EMPTY = CLASS +
            " failed validation because presets was null or empty.\n";
    private static final String ERR_STR_RES_MAP_NULL = CLASS +
            " failed validation because resolution map was null.\n";
    private static String ErrStrViewportLengthMismatch(
            int presetsLen, int viewportsLen) {
        return String.format("%s failed validation because viewports length " +
                "[%d] did not match presets length [%d].\n", CLASS,
                viewportsLen, presetsLen);
    }
    private static String ErrStrPresetNull(int index) {
        return String.format("%s failed validation because presets[%d] was " +
                "null.\n", CLASS, index);
    }
    private static String ErrStrPresetInvalid(int index,
                                              FramebufferPreset p) {
        return String.format("%s failed validation because presets[%d] had " +
                "invalid dimensions: emulatedW [%d], emulatedH [%d].\n",
                CLASS, index, p.emulatedW, p.emulatedH);
    }
    private static String ErrStrViewportInvalid(int index, ViewportPreset vp) {
        return String.format("%s failed validation because viewports[%d] had " +
                "invalid bounds: x [%d], y [%d], w [%d], h [%d].\n",
                CLASS, index, vp.x, vp.y, vp.w, vp.h);
    }
    private static String ErrStrViewportExceedsEmulated(
            int index, ViewportPreset vp, FramebufferPreset fp) {
        return String.format("%s failed validation because viewports[%d] " +
                "(x [%d] + w [%d] = %d, y [%d] + h [%d] = %d) exceeded " +
                "emulated dimensions (emulatedW [%d], emulatedH [%d]).\n",
                CLASS, index, vp.x, vp.w, vp.x + vp.w, vp.y, vp.h,
                vp.y + vp.h, fp.emulatedW, fp.emulatedH);
    }
    private static String ErrStrResMapEntryInvalid(int index) {
        return String.format("%s failed validation because resolutionMap[%d] " +
                "was null or did not have exactly 3 elements.\n",
                CLASS, index);
    }
    private static String ErrStrResMapDimsInvalid(int index,
                                                  int w, int h) {
        return String.format("%s failed validation because resolutionMap[%d] " +
                "had non-positive dimensions: w [%d], h [%d].\n",
                CLASS, index, w, h);
    }
    private static String ErrStrResMapIndexOOB(int index, int idx, int len) {
        return String.format("%s failed validation because resolutionMap[%d] " +
                "referenced preset index [%d], which is out of bounds for " +
                "presets of length [%d].\n", CLASS, index, idx, len);
    }
    private static String ErrStrNoViablePreset(int displayW, int displayH) {
        return String.format("%s failed to find a viable %s for displayW " +
                "[%d], displayH [%d].\n", CLASS, FramebufferPreset.CLASS,
                displayW, displayH);
    }
}