package production.tiledmap;

import production.sprite.SpriteAtlas;

public final class TileMap {
    public final String name;
    public final SpriteAtlas atlas;
    public final int width, height;
    private final int originOffsetX, originOffsetY;
    private final Tile tiles[][];

    TileMap(String name, SpriteAtlas atlas, int width, int height,
            int originOffsetX, int originOffsetY, Tile[][] tiles) {
        this.name = name;
        this.atlas = atlas;
        this.width = width;
        this.height = height;
        this.originOffsetX = originOffsetX;
        this.originOffsetY = originOffsetY;
        this.tiles = tiles;
    }

    public Tile getTile(short x, short y) {
        int ax = x - originOffsetX;
        int ay = y - originOffsetY;
        if (ax < 0 || ay < 0 || ax >= width || ay >= height) return null;
        return tiles[ay][ax];
    }
}