package production.tiledmap;

import production.sprite.SpriteAtlas;
import production.sprite.SpriteSys;

public final class TileMap {
    public final String name;
    public final int width, height;
    public final int originOffsetX, originOffsetY;
    private final Tile tiles[][];
    final String atlasFilename;

    TileMap(String name, int width, int height, String atlasFilename,
            int originOffsetX, int originOffsetY, Tile[][] tiles) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.originOffsetX = originOffsetX;
        this.originOffsetY = originOffsetY;
        this.tiles = tiles;
        this.atlasFilename = atlasFilename;
    }

    public Tile getTile(short x, short y) {
        int ax = x - originOffsetX;
        int ay = y - originOffsetY;
        if (ax < 0 || ay < 0 || ax >= width || ay >= height) return null;
        return tiles[ay][ax];
    }
}