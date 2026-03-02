package production;

import production.display.*;
import whitetail.loaders.config.ConfigEntry;
import whitetail.loaders.config.ConfigEntryType;
import whitetail.loaders.config.ConfigFileParser;
import whitetail.utility.logging.LogLevel;

import java.util.ArrayList;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.ErrorStrings.ERR_STR_FAILED_INIT_OOM;
import static whitetail.utility.logging.Logger.LogSession;
import static whitetail.utility.logging.Logger.EnableDevLogging;

public class EntryPoint {
    public static void main(String args[]) {
        int displayW, displayH;
        FramebufferPreset preset;
        DisplayProps props;

        System.out.println("Platform: " + Platform.getName());

        SuperCarpEngine engine = new SuperCarpEngine();

        if (!engine.initFirstHalf()) {
            System.err.println("Engine failed to init first half");
            return;
        }

        EnableDevLogging();

        if (!setupConfig()) {
            LogSession(LogLevel.WARNING, CLASS + ERR_STR_FAILED_SETUP_CFG);
        }
        if (!populateEngineVariablesFromConfig()) {
            LogSession(LogLevel.WARNING, CLASS +
                    ERR_STR_FAILED_POPULATE_FROM_CFG);
        }

        if (!DisplayConfig.QueryDisplayResolution()) {
            LogFatalAndExit(ERR_STR_FAILED_DESKTOP_RES);
            return;
        }

        displayW = DisplayConfig.GetDisplayW();
        displayH = DisplayConfig.GetDisplayH();

        FramebufferPreset fbs[] = new FramebufferPreset[] {
                new FramebufferPreset(384, 216)
        };

        ViewportPreset vps[] = new ViewportPreset[] {
                new ViewportPreset(4, 4, 305, 155)
        };

        int resMap[][] = new int[][] {
                new int[] {1920, 1080, 0}
        };

        if (!FramebufferConfig.Init(fbs, vps, resMap, false)) {
            LogFatalAndExit(ERR_STR_FAILED_FRAMEBUFFER_CONFIG);
            return;
        }

        if ((preset = FramebufferConfig.ResolvePreset(displayW, displayH)) ==
                null) {
            LogFatalAndExit(ERR_STR_FAILED_FRAMEBUFFER_CONFIG_RESOLVE);
            return;
        }

        props = FramebufferConfig.BuildDisplayProps(preset,
                displayW, displayH, CFGData.bBorderless, CFGData.bFullscreen,
                CFGData.bVsync, CFGData.iFpsTarget);

        if (!DisplayConfig.ApplyDisplayProps(props)) {
            LogFatalAndExit(ERR_STR_FAILED_APPLY_DISPLAY_PROPS);
            return;
        }

        if (!engine.initSecondHalf("SuperCarp dev build",
                DisplayConfig.GetWindowW(), DisplayConfig.GetWindowH(),
                DisplayConfig.GetFpsTarget(), DisplayConfig.IsVSync(),
                DisplayConfig.IsFullscreen(), DisplayConfig.IsBorderless(),
                Data.TICK_DUR)) {
            LogFatalAndExit("Engine failed to init second half");
            return;
        }
        engine.run();
    }

    private static boolean setupConfig() {
        try {
            Data.cfgEntries = new ArrayList<ConfigEntry>();
        } catch (OutOfMemoryError e) {
            LogFatalAndExit(CLASS + ERR_STR_FAILED_INIT_OOM);
            return false;
        }

        Data.cfgEntries.add(new ConfigEntry(CFGData.CFG_F_GLOBAL_VOLUME,
                ConfigEntryType.FLOAT,
                0.0f,
                100.0f,
                100.0f));

        Data.cfgEntries.add(new ConfigEntry(CFGData.CFG_B_BORDERLESS,
                ConfigEntryType.BOOL,
                null,
                null,
                true));

        Data.cfgEntries.add(new ConfigEntry(CFGData.CFG_B_FULLSCREEN,
                ConfigEntryType.BOOL,
                null,
                null,
                false));

        Data.cfgEntries.add(new ConfigEntry(CFGData.CFG_B_VSYNC,
                ConfigEntryType.BOOL,
                null,
                null,
                true));

        Data.cfgEntries.add(new ConfigEntry(CFGData.CFG_I_FPS_TARGET,
                ConfigEntryType.INT,
                0,
                0x7FFFFFFF,
                0));

        if (!ConfigFileParser.Init(Data.cfgEntries)) {
            LogSession(LogLevel.WARNING, ConfigFileParser.CLASS
                    + ERR_STR_FAILED_INIT_CFG);
            return false;
        }

        return true;
    }

    private static boolean populateEngineVariablesFromConfig() {
        CFGData.fGlobalVolume = ConfigFileParser.GetFloat(
                CFGData.CFG_F_GLOBAL_VOLUME);

        CFGData.bBorderless = ConfigFileParser.GetBool(
                CFGData.CFG_B_BORDERLESS);

        CFGData.bFullscreen = ConfigFileParser.GetBool(
                CFGData.CFG_B_FULLSCREEN);

        CFGData.bVsync = ConfigFileParser.GetBool(
                CFGData.CFG_B_VSYNC);

        CFGData.iFpsTarget = ConfigFileParser.GetInt(
                CFGData.CFG_I_FPS_TARGET);

        return true;
    }

    public static final String CLASS = EntryPoint.class.getSimpleName();
    private static final String ERR_STR_FAILED_INIT_CFG = " failed to " +
            "initialize, using defaults.\n";
    private static final String ERR_STR_FAILED_SETUP_CFG = " failed to" +
            "set up config file, using defaults.\n";
    private static final String ERR_STR_FAILED_POPULATE_FROM_CFG = " failed " +
            "to populate engine variables from config file, using defaults.\n";
    private static final String ERR_STR_FAILED_DESKTOP_RES = CLASS + " failed" +
            " to initialize because DisplayConfig was unable to query desktop" +
            " resolution.\n";
    private static final String ERR_STR_FAILED_FRAMEBUFFER_CONFIG = CLASS +
            " failed to initialize because " + FramebufferConfig.CLASS +
            " failed to initialize.\n";
    private static final String ERR_STR_FAILED_FRAMEBUFFER_CONFIG_RESOLVE =
            CLASS + " failed to initialize because " + FramebufferConfig.CLASS +
                    " failed to initialize.\n";
    private static final String ERR_STR_FAILED_APPLY_DISPLAY_PROPS = CLASS +
            " failed to initialize because " + DisplayConfig.CLASS + " failed" +
            " to apply the display properties.\n";
}
