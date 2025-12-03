package production;

import whitetail.utility.logging.LogLevel;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.Logger.LogSession;

public class SpriteSys {
    private static boolean init;
    private static int cap;

    private static int x[];
    private static int y[];
    private static byte layer[];
    private static short uvIdx[];
    private static byte flags[];

    private static final int DEF_CAP = 0xFFFF;

    private SpriteSys() {}

    public static boolean Init(int cap) {
        assert(!init);

        if (cap > 0) {
            SpriteSys.cap = cap;
        } else {
            SpriteSys.cap = DEF_CAP;
            LogSession(LogLevel.DEBUG, ErrStrCapOutOfBounds(cap, "cap", DEF_CAP));
        }

        try {
            x = new int[SpriteSys.cap];
            y = new int[SpriteSys.cap];
            layer = new byte[SpriteSys.cap];
            uvIdx = new short[SpriteSys.cap];
            flags = new byte[SpriteSys.cap];

            return init = true;
        } catch (OutOfMemoryError e) {
            LogFatalAndExit(ERR_STR_FAILED_INIT);

            return init = false;
        }
    }

    public static final String CLASS = SpriteSys.class.getSimpleName();
    private static final String ERR_STR_FAILED_INIT = CLASS + " failed to " +
            "initialize because an OutOfMemoryError was encountered.\n";
    private static String ErrStrCapOutOfBounds(int c, String s, int def) {
        return String.format("%s defaulted to [%s] capacity [%d] because an " +
                        "invalid [%s] capacity [%d] was requested.\n", CLASS, s,
                def, s, c);
    }

}
