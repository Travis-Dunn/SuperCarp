package production.monster;

import java.util.HashMap;
import java.util.Map;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;

public final class MonsterDefs {
    private static final Map<String, MonsterDef> defs =
            new HashMap<String,MonsterDef>();

    private static MonsterDef register(MonsterDef d) {
        defs.put(d.name, d);
        return d;
    }

    public static MonsterDef get(String name) {
        MonsterDef d = defs.get(name);
        if (d == null) {
            LogFatalAndExit("Unknown monster!\n");
            return null;
        }
        return d;
    }

    public static final MonsterDef GOBLIN = register(new MonsterDef(
            new short[] { 0, 1, 2 },
            (short) 200,
            true,
            "Goblin",
            "Goblin",
            8
    ));
}
