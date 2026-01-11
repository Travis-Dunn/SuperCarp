package production;

import production.sprite.SpriteCamera;
import production.tiledmap.Tile;
import production.tiledmap.TileMap;

import java.util.ArrayList;

/**
 * Handles mouse input and converts clicks to tile interactions.
 * Currently handles movement; will later handle combat, objects, etc.
 */
public final class Cursor {
    private SpriteCamera cam;
    private TileMap map;
    private int tileSize;
    private int windowWidth;
    private int windowHeight;
    private int fbWidth;
    private int fbHeight;

    public Cursor(SpriteCamera cam, TileMap map, int tileSize,
                  int windowWidth, int windowHeight,
                  int fbWidth, int fbHeight) {
        this.cam = cam;
        this.map = map;
        this.tileSize = tileSize;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.fbWidth = fbWidth;
        this.fbHeight = fbHeight;
    }

    public void handleMouseClick(int sx, int sy) {
        /* convert window coords to framebuffer coords */
        /* LWJGL 2 has Y=0 at bottom, flip to top-left origin */
        int fbX = (sx * fbWidth) / windowWidth;
        int fbY = ((windowHeight - sy) * fbHeight) / windowHeight;
        /* convert framebuffer coords to tile coords */
        int tileX = cam.screenToTileX(fbX, tileSize);
        int tileY = cam.screenToTileY(fbY, tileSize);

        /* look up the tile */
        Tile tile = map.getTile((short) tileX, (short) tileY);

        if (tile == null) {
            System.out.println("Click: no tile at [" + tileX + ", " + tileY + "]");
            return;
        }

        if (tile.blocked) {
            System.out.println("Click: tile [" + tileX + ", " + tileY + "] is blocked");
            return;
        }

        /* compute path from player to clicked tile */
        ArrayList<Integer> path = Pathfinder.find(map,
                Player.tileX, Player.tileY, tileX, tileY);

        if (path == null) {
            System.out.println("Click: no path to [" + tileX + ", " + tileY + "]");
        } else if (path.isEmpty()) {
            System.out.println("Click: already at [" + tileX + ", " + tileY + "]");
        } else {
            System.out.println("Click: path to [" + tileX + ", " + tileY + "] " +
                    "length=" + path.size());
            Player.setPath(path);
        }
    }
}