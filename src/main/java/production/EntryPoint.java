package production;

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

        if (!DisplayConfig.Init(CFGData.bFullscreen, CFGData.bBorderless,
                3,
                0, 0,
                1440, 810)) {
            LogFatalAndExit(ERR_STR_FAILED_DISPLAY_CONFIG);
        }

        if (!engine.initSecondHalf("SuperCarp dev build",
                DisplayConfig.GetWindowW(), DisplayConfig.GetWindowH(),
                0, true, DisplayConfig.IsFullscreen(),
                DisplayConfig.IsBorderless(), 0.6)) {
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

        return true;
    }

    public static final String CLASS = EntryPoint.class.getSimpleName();
    private static final String ERR_STR_FAILED_INIT_CFG = " failed to " +
            "initialize, using defaults.\n";
    private static final String ERR_STR_FAILED_SETUP_CFG = " failed to" +
            "set up config file, using defaults.\n";
    private static final String ERR_STR_FAILED_POPULATE_FROM_CFG = " failed " +
            "to populate engine variables from config file, using defaults.\n";
    private static final String ERR_STR_FAILED_DISPLAY_CONFIG = CLASS +
            " failed because " + DisplayConfig.CLASS + " failed to " +
            "initialize.\n";
}
