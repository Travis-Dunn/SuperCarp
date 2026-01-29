package production;

import production.character.Char;
import production.sprite.SpriteCamera;
import production.tilemap.Tile;
import production.tilemap.TileMap;
import production.ui.ChatBox;

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

        /* TODO: dispatch to handleViewportClick if in viewport, else handleUiClick */
        /* TODO: in viewport, if (we clicked an npc), we need to pathfind to the closest adjacent tile
        and somehow... I don't know how exactly... somehow store that we are trying to talk to the npc
        and every tick, attempt to talk, but talking only succeeds if we are adjacent. failing to talk
        does not clear the "talk target" or whatever. That way, it's something like "attempt to talk,
        if failed, don't do anything differently. This will naturally allow the character to continue
        moving along the path, until it is adjacent. Edge case tho - if we attempt to talk to something
        that we can't path to, don't set the talk target, or it will never be clearable. I think...
         */

        if (inViewport(fbX, fbY)) {
            handleViewportClick(fbX, fbY);
        } else {
            handleUiClick(fbX, fbY);
        }

   }

    private boolean inViewport(int x, int y) {
        /* TODO: When we actually have UI, we'll need to do something here */
        return true;
    }

    private void handleViewportClick(int x, int y) {
        /* convert framebuffer coords to tile coords */
        int tileX = cam.screenToTileX(x, tileSize);
        int tileY = cam.screenToTileY(y, tileSize);
        Char c = map.getCharAt(tileX, tileY);

        if (c != null) {
            ChatBox.AddMsg("Clicked on: " + c.displayName);
            return;
        }

        /* look up the tile */
        Tile tile = map.getTile((short) tileX, (short) tileY);

        if (tile == null) {
            ChatBox.AddMsg("Click: no tile at [" + tileX + ", " + tileY + "]");
            return;
        }

        if (tile.blocked) {
            ChatBox.AddMsg("Click: tile [" + tileX + ", " + tileY + "] is blocked");

            if (tile.getExamine() != null) {
                ChatBox.AddMsg(tile.getExamine());
            }

            return;
        }


        if (tile.getExamine() != null) {
            ChatBox.AddMsg(tile.getExamine());
        }

        /* compute path from player to clicked tile */
        ArrayList<Integer> path = Pathfinder.find(map,
                Player.tileX, Player.tileY, tileX, tileY);

        if (path == null) {
            ChatBox.AddMsg("Click: no path to [" + tileX + ", " + tileY + "]");
        } else if (path.isEmpty()) {
            ChatBox.AddMsg("Click: already at [" + tileX + ", " + tileY + "]");
        } else {
            ChatBox.AddMsg("Click: path to [" + tileX + ", " + tileY + "] " +
                    "length=" + path.size());
            Player.setPath(path);
        }
    }

    private void handleUiClick(int x, int y) {

    }
}