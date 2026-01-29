package production.character;

import production.Pathfinder;
import production.monster.MonsterDef;
import production.monster.MonsterRegistry;
import production.monster.MonsterSpawn;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;

public final class CharFileParser {
    public static Char FromLine(String line) {
        String[] parts = line.split("\t");

        if (parts.length < 3) {
            return null;
        }

        try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            String name = parts[2];

            Char c = CharRegistry.get(name);
            if (c == null) {
                LogFatalAndExit(ErrStrFailParse(x, y, name));
                return null;
            }

            c.tileX = x;
            c.tileY = y;

            return c;

        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static final String CLASS = CharFileParser.class.getSimpleName();
    private static String ErrStrFailParse(int x, int y, String name) {
        return String.format("%s failed to parse line. [%s] at [%d], [%d] " +
                "because the %s was unable to find the Char.\n",
                CLASS, name, x, y, CharRegistry.CLASS);
    }
}
