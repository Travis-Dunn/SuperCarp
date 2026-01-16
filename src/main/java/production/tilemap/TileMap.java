package production.tilemap;

import production.monster.MonsterSpawn;

import java.util.List;

public final class TileMap {
    public final String name;
    public final int width, height;
    public final int originOffsetX, originOffsetY;
    private final Tile tiles[][];
    private final List<MonsterSpawn> spawns;
    final String atlasFilename;

    /* TODO: needs to know what palette it uses! */
    TileMap(String name, int width, int height, String atlasFilename,
            int originOffsetX, int originOffsetY, Tile[][] tiles,
            List<MonsterSpawn> spawns) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.originOffsetX = originOffsetX;
        this.originOffsetY = originOffsetY;
        this.tiles = tiles;
        this.spawns = spawns;
        this.atlasFilename = atlasFilename;
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
}