package whitetail.loaders.config;

import whitetail.utility.logging.LogLevel;

import java.io.*;
import java.util.*;

import static whitetail.loaders.config.ConfigEntryType.*;
import static whitetail.utility.logging.Logger.LogSession;

public final class ConfigFileParser {
    private static final String CONFIG_FILENAME = "config.ini";
    private static final String TUNING_FILENAME = "tuning.ini";
    private static Map<String, ConfigEntry> entries;
    private static boolean init = false;

    private static final String ERR_STR_PARSING_FAILED =
            "Failed to parse config file, using defaults.\n" +
            "Because a file already exists with the name 'config.ini', a\n" +
            "default config file was not generated. For further info,\n" +
            "see readme.txt.\n";
    private static final String ERR_STR_FILE_NOT_FOUND =
            "Failed to find config file, default config file created.";
    private static final String ERR_STR_MALFORMED_LINE =
            "The following malformed config file line was ignored:\n";
    private static final String ERR_STR_NO_ASSIGNMENT_OP =
            "Because of the reason: No assignment operator found";
    private static final String ERR_STR_KEY_NOT_FOUND =
            "Because of the reason: Key not found";
    private static final String ERR_STR_VALUE_INTERPRETATION_FAILURE =
            "Because of the reason: Value interpretation failure\n" +
            "Required type is: \n";
    private static final String ERR_STR_VALUE_LO =
            "Because of the reason: Value below minimum, which is:\n";
    private static final String ERR_STR_VALUE_HI =
            "Because of the reason: Value above maximum, which is:\n";
    private static final String ERR_STR_STR_VAL_TOO_SHORT =
            "Because of the reason: Str length < min length, which is:\n";
    private static final String ERR_STR_STR_VAL_TOO_LONG =
            "Because of the reason: Str length > max length, which is:\n";
    private static final String ERR_STR_MALFORMED_BOOL_VAL =
            "Because of the reason: Expected value to be a bool. Allowed\n" +
            "values include: [true, 1, false, 0]";
    private static final String ERR_STR_FAILED_TO_CREATE_DEFAULT_FILE =
            "Failed to create default config file.";
    private static final String ERR_STR_UNRECOGNIZED_KEY =
            "The following line contains an unrecognized key, and was ignored:";
    private static final String ERR_STR_FAILED_TO_READ_FILE =
            "Failed to read config file";
    private static final String ERR_STR_FAILED_TO_APPEND =
            "Failed to append missing entries to file";
    private static final String ERR_STR_MISSING_ENTRY_APPENDED =
            "The following entry was not found in the config file, and has" +
            " been appended with it's default value: [";

    private ConfigFileParser() {}

    public static boolean Init(ArrayList<ConfigEntry> configEntries) {
        assert(!init);
        assert(configEntries != null);
        assert(!configEntries.isEmpty());

        entries = new HashMap<String, ConfigEntry>();
        for (ConfigEntry e : configEntries) {
            e.assertValid(entries);
            entries.put(e.key, e);
        }

        File configFile = new File(CONFIG_FILENAME);
        if (configFile.exists()) {
            parseFile(configFile);
        } else {
            LogSession(LogLevel.DEBUG, ERR_STR_FILE_NOT_FOUND);
            WriteDefaultFile();
        }

        return init = true;
    }

    private static void parseFile(File f) {
        List<ConfigEntry> missingEntries = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            Set<String> foundKeys = new HashSet<>();

            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) continue;

                int equalsPos = line.indexOf("=");
                if (equalsPos == -1) {
                    LogSession(LogLevel.DEBUG, ERR_STR_MALFORMED_LINE);
                    LogSession(LogLevel.DEBUG, "[" + line + "]");
                    LogSession(LogLevel.DEBUG, ERR_STR_NO_ASSIGNMENT_OP);
                    continue;
                }
                String keyStr = line.substring(0, equalsPos).trim();
                String valStr = line.substring(equalsPos + 1).trim();

                ConfigEntry entry = entries.get(keyStr);
                if (entry == null) {
                    LogSession(LogLevel.DEBUG, ERR_STR_UNRECOGNIZED_KEY);
                    LogSession(LogLevel.DEBUG, "[" + line + "]");
                    continue;
                }
                foundKeys.add(keyStr);
                /*
                parseValue(valStr, entry, line);

                 */
                entry.update(valStr, line);
            }
            for (ConfigEntry e : entries.values()) {
                if (!foundKeys.contains(e.key))
                    missingEntries.add(e);
            }
        } catch (IOException e) {
            LogSession(LogLevel.DEBUG, ERR_STR_FAILED_TO_READ_FILE);
        }
        if (!missingEntries.isEmpty()) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(CONFIG_FILENAME, true))) {
                writer.println();
                for (ConfigEntry e : missingEntries) {
                    writer.println(e.key + "=" + e.defaultVal);
                    LogSession(LogLevel.DEBUG, ERR_STR_MISSING_ENTRY_APPENDED +
                            e.key + "]");
                }
            } catch (IOException e) {
                LogSession(LogLevel.DEBUG, ERR_STR_FAILED_TO_APPEND);
            }
        }
    }

    private static void parseValue(String s, ConfigEntry e,
            String line) {
        try {
            switch(e.type) {
                case INT: {
                    int v = Integer.parseInt(s);
                    if (v < (Integer)e.min) {
                        LogSession(LogLevel.DEBUG, ERR_STR_MALFORMED_LINE);
                        LogSession(LogLevel.DEBUG, "[" + line + "]");
                        LogSession(LogLevel.DEBUG, ERR_STR_VALUE_LO);
                        LogSession(LogLevel.DEBUG, "[" + e.min + "]");
                    } else if (v > (Integer)e.max) {
                        LogSession(LogLevel.DEBUG, ERR_STR_MALFORMED_LINE);
                        LogSession(LogLevel.DEBUG, "[" + line + "]");
                        LogSession(LogLevel.DEBUG, ERR_STR_VALUE_HI);
                        LogSession(LogLevel.DEBUG, "[" + e.max + "]");
                    } else {
                        e.setVal(v);
                    }
                } break;
                case FLOAT: {
                    float v = Float.parseFloat(s);
                    if (v < (Float)e.min) {
                        LogSession(LogLevel.DEBUG, ERR_STR_MALFORMED_LINE);
                        LogSession(LogLevel.DEBUG, "[" + line + "]");
                        LogSession(LogLevel.DEBUG, ERR_STR_VALUE_LO);
                        LogSession(LogLevel.DEBUG, "[" + e.min + "]");
                    } else if (v > (Float)e.max) {
                        LogSession(LogLevel.DEBUG, ERR_STR_MALFORMED_LINE);
                        LogSession(LogLevel.DEBUG, "[" + line + "]");
                        LogSession(LogLevel.DEBUG, ERR_STR_VALUE_HI);
                        LogSession(LogLevel.DEBUG, "[" + e.max + "]");
                    } else {
                        e.setVal(v);
                    }
                } break;
                case BOOL: {
                    if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
                        e.setVal(true);
                    } else if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
                        e.setVal(false);
                    } else {
                        LogSession(LogLevel.DEBUG, ERR_STR_MALFORMED_LINE);
                        LogSession(LogLevel.DEBUG, "[" + line + "]");
                        LogSession(LogLevel.DEBUG, ERR_STR_MALFORMED_BOOL_VAL);
                    }
                } break;
                case STR: {
                    if (s.length() < (Integer)e.min) {
                        LogSession(LogLevel.DEBUG, ERR_STR_MALFORMED_LINE);
                        LogSession(LogLevel.DEBUG, "[" + line + "]");
                        LogSession(LogLevel.DEBUG, ERR_STR_STR_VAL_TOO_SHORT);
                        LogSession(LogLevel.DEBUG, "[" + e.min + "]");
                    } else if (s.length() > (Integer)e.max) {
                        LogSession(LogLevel.DEBUG, ERR_STR_MALFORMED_LINE);
                        LogSession(LogLevel.DEBUG, "[" + line + "]");
                        LogSession(LogLevel.DEBUG, ERR_STR_STR_VAL_TOO_LONG);
                        LogSession(LogLevel.DEBUG, "[" + e.max + "]");
                    } else {
                        e.setVal(s);
                    }
                } break;
            }
        } catch (NumberFormatException ex) {
            LogSession(LogLevel.DEBUG, ERR_STR_MALFORMED_LINE);
            LogSession(LogLevel.DEBUG, "[" + line + "]");
            LogSession(LogLevel.DEBUG, ERR_STR_VALUE_INTERPRETATION_FAILURE);
            LogSession(LogLevel.DEBUG, "[" + e.type + "]");
        }
    }


    private static void WriteDefaultFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(CONFIG_FILENAME))) {
            for (ConfigEntry entry : entries.values()) {
                writer.println(entry.key + "=" + entry.defaultVal);
            }
        } catch (IOException e) {
            LogSession(LogLevel.DEBUG, ERR_STR_FAILED_TO_CREATE_DEFAULT_FILE);
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