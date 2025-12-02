package whitetail.utility;

import whitetail.core.GameEngine;
import whitetail.utility.logging.Logger;

import javax.swing.*;

/** Very much WIP */
public final class ErrorHandler {
    private static GameEngine game;

    private ErrorHandler () {}

    public static boolean Init(GameEngine g) {
        assert(g != null);

        game = g;

        return true;
    }

    public static void LogFatalAndExit(String s) {
        JOptionPane.showMessageDialog(null,
                s, "Fatal Error",
                JOptionPane.ERROR_MESSAGE);
        Logger.LogFatal(s, null);
        game.setIsRunning(false);
    }

    public static void LogFatalExcpAndExit(String s, Throwable t) {
        JOptionPane.showMessageDialog(null,
                s + "See session.log for stack trace.\n", "Fatal Error",
                JOptionPane.ERROR_MESSAGE);
        Logger.LogFatal(s, t);
        game.setIsRunning(false);
    }

    public static void LogFatalWithFloatAndExit(String s, float f) {
        String str = s + " {" + f + "}";
        JOptionPane.showMessageDialog(null,
                str, "Fatal Error",
                JOptionPane.ERROR_MESSAGE);
        Logger.LogFatal(str, null);
        game.setIsRunning(false);
    }

    public static void LogFatalExcpWithFloatAndExit(String s, float f,
            Throwable t) {
        String str = s + " {" + f + "}";
        JOptionPane.showMessageDialog(null,
                str, "Fatal Error",
                JOptionPane.ERROR_MESSAGE);
        Logger.LogFatal(str, t);
        game.setIsRunning(false);
    }
}
