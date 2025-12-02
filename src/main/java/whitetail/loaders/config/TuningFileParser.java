package whitetail.loaders.config;

import whitetail.utility.logging.LogLevel;

import java.io.*;
import java.util.*;

import static whitetail.loaders.config.ConfigEntryType.*;
import static whitetail.loaders.config.ConfigEntryType.STR;
import static whitetail.utility.logging.Logger.LogSession;

public final class TuningFileParser {
    private static boolean enabled;
    private static final String FILENAME = "tuning.ini";
    private static final String ERR_STR_FAILED_TO_CREATE_FILE =
            "Failed to create tuning file";
    private static final String ERR_STR_FAILED_TO_READ_FILE =
            "Failed to read config file";
    private static boolean init;
    private static Map<String, ConfigEntry> entries;
    private static long lastModified = 0;

    public static boolean Init(ArrayList<ConfigEntry> configEntries) {
        assert(!init);
        assert(configEntries != null);
        assert(!configEntries.isEmpty());

        entries = new HashMap<>();
        for (ConfigEntry e : configEntries) {
            e.assertValid(entries);
            entries.put(e.key, e);
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(FILENAME))) {
            for (ConfigEntry entry : entries.values()) {
                writer.println(entry.key + "=" + entry.defaultVal);
            }
        } catch (IOException e) {
            LogSession(LogLevel.DEBUG, ERR_STR_FAILED_TO_CREATE_FILE);
        }
        return init = true;
    }

    /* Called once per frame */
    public static void ParseFile() {
        File f = new File(FILENAME);
        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            long currentModified = f.lastModified();
            if (currentModified <= lastModified) {
                return;
            }
            lastModified = currentModified;

            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) continue;

                int equalsPos = line.indexOf("=");
                if (equalsPos == -1) {
                    continue;
                }
                String keyStr = line.substring(0, equalsPos).trim();
                String valStr = line.substring(equalsPos + 1).trim();

                ConfigEntry entry = entries.get(keyStr);
                if (entry == null) {
                    continue;
                }
                entry.update(valStr, line);
            }
        } catch (IOException e) {
            LogSession(LogLevel.DEBUG, ERR_STR_FAILED_TO_READ_FILE);
        }
    }

    public static int GetInt(String k) {
        assert(init);
        ConfigEntry e = entries.get(k);
        assert(e != null);
        assert(e.type == INT);
        return (Integer)e.getVal();
    }

    public static float GetFloat(String k) {
        assert(init);
        ConfigEntry e = entries.get(k);
        assert(e != null);
        assert(e.type == FLOAT);
        return (Float)e.getVal();
    }

    public static boolean GetBool(String k) {
        assert(init);
        ConfigEntry e = entries.get(k);
        assert(e != null);
        assert(e.type == BOOL);
        return (Boolean)e.getVal();
    }

    public static String GetStr(String k) {
        assert(init);
        ConfigEntry e = entries.get(k);
        assert(e != null);
        assert(e.type == STR);
        return (String)e.getVal();
    }
}
