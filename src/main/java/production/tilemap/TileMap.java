package production.tilemap;

import production.Pathfinder;
import production.character.Char;
import production.monster.MonsterSpawn;
import whitetail.utility.logging.LogLevel;

import java.util.List;
import java.util.Map;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.Logger.LogSession;

public final class TileMap {
    public final String name;
    public final int width, height;
    public final int originOffsetX, originOffsetY;
    private final Tile tiles[][];
    private final List<MonsterSpawn> spawns;
    Map<Integer, Char> charsByPos;
    final String atlasFilename;
    public final int clearColor;

    /* TODO: needs to know what palette it uses! */
    TileMap(String name, int width, int height, String atlasFilename,
            int originOffsetX, int originOffsetY, Tile[][] tiles,
            List<MonsterSpawn> spawns, int clearColor,
            Map<Integer, Char> charsByPos) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.originOffsetX = originOffsetX;
        this.originOffsetY = originOffsetY;
        this.tiles = tiles;
        this.spawns = spawns;
        this.atlasFilename = atlasFilename;
        this.clearColor = clearColor;
        this.charsByPos = charsByPos;
    }

    public Tile getTile(short x, short y) {
        int ax = x - originOffsetX;
        int ay = y - originOffsetY;
        if (ax < 0 || ay < 0 || ax >= width || ay >= height) return null;
        return tiles[ay][ax];
    }

    public void update() {
        for (MonsterSpawn spawn : spawns) {
            if (spawn != null) {
                spawn.update();
            }
        }
    }

    public Char getCharAt(int x, int y) {
        return charsByPos.get(Pathfinder.pack(x, y));
    }

    public void addChar(Char c) {
        assert(c != null);

        if (c == null) {
            LogFatalAndExit(ERR_STR_FAILED_UPDATE_CHAR_POS);
        }

        charsByPos.put(Pathfinder.pack(c.tileX, c.tileY), c);
    }

    public void updateCharPos(Char c, int x, int y) {
        assert(c != null);

        if (c == null) {
            LogFatalAndExit(ERR_STR_FAILED_UPDATE_CHAR_POS);
        }
        if (!charsByPos.values().remove(c)) {
            LogSession(LogLevel.WARNING, errStrFailedUpdateCharPos(c));
        }
        c.tileX = x;
        c.tileY = y;
        charsByPos.put(Pathfinder.pack(x, y), c);
    }

    public static final String CLASS = TileMap.class.getSimpleName();
    private String errStrFailedUpdateCharPos(Char c) {
        return String.format("%s [%s] failed to update Char [%s] position " +
                "because the Char was not found in the map.",
                CLASS, this.name, c.name);
    }
    private static final String ERR_STR_FAILED_UPDATE_CHAR_POS = CLASS +
            " failed to update Char position because Char was null.";
}