package production.character;

import production.sprite.SpriteAnim;
import production.sprite.SpriteAnimSys;

import java.util.HashMap;
import java.util.Map;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;

public final class CharRegistry {
    public static final Map<String, Char> chars = new HashMap<String, Char>();

    private static Char register(Char c) {
        chars.put(c.name, c);
        return c;
    }

    public static Char get(String name) {
        Char c = chars.get(name);
        if (c == null) {
            LogFatalAndExit("Unknown char!\n");
            return null;
        }
        return c;
    }

    public static final Char BILBO = register(new Char(
            "Bilbo",
            "Bilbo",
            new short[][] { { 0, 1, 2 } },
            new short[] { 200 },
            new boolean[] { true }
    ));

}
