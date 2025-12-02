package whitetail.loaders.config;

import whitetail.utility.logging.LogLevel;

import java.util.Map;

import static whitetail.loaders.config.ConfigEntryType.*;
import static whitetail.loaders.config.ConfigEntryType.STR;
import static whitetail.utility.logging.Logger.LogSession;

public final class ConfigEntry {
    public final String key;
    public final ConfigEntryType type;
    public final Object min;
    public final Object max;
    public final Object defaultVal;

    private Object val;

    private static final String ERR_STR_MALFORMED_LINE =
            "The following malformed config file line was ignored:\n";
    private static final String ERR_STR_VALUE_LO =
            "Because of the reason: Value below minimum, which is:\n";
    private static final String ERR_STR_VALUE_HI =
            "Because of the reason: Value above maximum, which is:\n";
    private static final String ERR_STR_MALFORMED_BOOL_VAL =
            "Because of the reason: Expected value to be a bool. Allowed\n" +
                    "values include: [true, 1, false, 0]";
    private static final String ERR_STR_STR_VAL_TOO_SHORT =
            "Because of the reason: Str length < min length, which is:\n";
    private static final String ERR_STR_STR_VAL_TOO_LONG =
            "Because of the reason: Str length > max length, which is:\n";
    private static final String ERR_STR_VALUE_INTERPRETATION_FAILURE =
            "Because of the reason: Value interpretation failure\n" +
                    "Required type is: \n";

    public ConfigEntry(String k, ConfigEntryType t, Object min, Object max,
            Object defaultVal) {
        key = k;
        type = t;
        this.min = min;
        this.max = max;
        this.defaultVal = defaultVal;
        val = defaultVal;
    }

    public void assertValid(Map<String, ConfigEntry> hashmap) {
        assert(hashmap != null);
        assert(key != null);
        assert(type != null);
        assert(defaultVal != null);
        assert(!key.trim().isEmpty());
        assert(!hashmap.containsKey(key));
        assert(type == INT || type == FLOAT || type == BOOL || type == STR);

        switch(type) {
            case INT: {
                assert(min != null);
                assert(min instanceof Integer);
                assert(max != null);
                assert(max instanceof Integer);
                assert(defaultVal instanceof Integer);
                assert((Integer)min <= (Integer)defaultVal);
                assert((Integer)defaultVal <= (Integer)max);
            } break;
            case FLOAT: {
                assert(min != null);
                assert(min instanceof Float);
                assert(max != null);
                assert(max instanceof Float);
                assert(defaultVal instanceof Float);
                assert((Float)min <= (Float)defaultVal);
                assert((Float)defaultVal <= (Float)max);
            } break;
            case BOOL: {
                assert(min == null);
                assert(max == null);
                assert(defaultVal instanceof Boolean);
            } break;
            case STR: {
                assert(defaultVal instanceof String);
                assert(min != null);
                assert(max != null);
                assert((Integer)min <= ((String)defaultVal).length());
                assert(((String)defaultVal).length() <= (Integer)max);
            } break;
        }
    }

    public void update(String s, String line) {
        try {
            switch(type) {
                case INT: {
                    int v = Integer.parseInt(s);
                    if (v < (Integer)min) {
                        LogSession(LogLevel.DEBUG, ERR_STR_MALFORMED_LINE);
                        LogSession(LogLevel.DEBUG, "[" + line + "]");
                        LogSession(LogLevel.DEBUG, ERR_STR_VALUE_LO);
                        LogSession(LogLevel.DEBUG, "[" + min + "]");
                    } else if (v > (Integer)max) {
                        LogSession(LogLevel.DEBUG, ERR_STR_MALFORMED_LINE);
                        LogSession(LogLevel.DEBUG, "[" + line + "]");
                        LogSession(LogLevel.DEBUG, ERR_STR_VALUE_HI);
                        LogSession(LogLevel.DEBUG, "[" + max + "]");
                    } else {
                        setVal(v);
                    }
                } break;
                case FLOAT: {
                    float v = Float.parseFloat(s);
                    if (v < (Float)min) {
                        LogSession(LogLevel.DEBUG, ERR_STR_MALFORMED_LINE);
                        LogSession(LogLevel.DEBUG, "[" + line + "]");
                        LogSession(LogLevel.DEBUG, ERR_STR_VALUE_LO);
                        LogSession(LogLevel.DEBUG, "[" + min + "]");
                    } else if (v > (Float)max) {
                        LogSession(LogLevel.DEBUG, ERR_STR_MALFORMED_LINE);
                        LogSession(LogLevel.DEBUG, "[" + line + "]");
                        LogSession(LogLevel.DEBUG, ERR_STR_VALUE_HI);
                        LogSession(LogLevel.DEBUG, "[" + max + "]");
                    } else {
                        setVal(v);
                    }
                } break;
                case BOOL: {
                    if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
                        setVal(true);
                    } else if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
                        setVal(false);
                    } else {
                        LogSession(LogLevel.DEBUG, ERR_STR_MALFORMED_LINE);
                        LogSession(LogLevel.DEBUG, "[" + line + "]");
                        LogSession(LogLevel.DEBUG, ERR_STR_MALFORMED_BOOL_VAL);
                    }
                } break;
                case STR: {
                    if (s.length() < (Integer)min) {
                        LogSession(LogLevel.DEBUG, ERR_STR_MALFORMED_LINE);
                        LogSession(LogLevel.DEBUG, "[" + line + "]");
                        LogSession(LogLevel.DEBUG, ERR_STR_STR_VAL_TOO_SHORT);
                        LogSession(LogLevel.DEBUG, "[" + min + "]");
                    } else if (s.length() > (Integer)max) {
                        LogSession(LogLevel.DEBUG, ERR_STR_MALFORMED_LINE);
                        LogSession(LogLevel.DEBUG, "[" + line + "]");
                        LogSession(LogLevel.DEBUG, ERR_STR_STR_VAL_TOO_LONG);
                        LogSession(LogLevel.DEBUG, "[" + max + "]");
                    } else {
                        setVal(s);
                    }
                } break;
            }
        } catch (NumberFormatException ex) {
            LogSession(LogLevel.DEBUG, ERR_STR_MALFORMED_LINE);
            LogSession(LogLevel.DEBUG, "[" + line + "]");
            LogSession(LogLevel.DEBUG, ERR_STR_VALUE_INTERPRETATION_FAILURE);
            LogSession(LogLevel.DEBUG, "[" + type + "]");
        }
    }

    public void setVal(Object o) { val = o; }

    public Object getVal() { return val; }
}
