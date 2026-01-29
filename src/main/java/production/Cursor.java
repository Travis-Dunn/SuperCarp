package production;

import production.character.Char;
import production.sprite.SpriteCamera;
import production.tilemap.Tile;
import production.tilemap.TileMap;
import production.ui.ChatBox;

import java.util.ArrayList;

import static whitetail.utility.logging.Logger.DevLoggingEnabled;

/**
 * Handles mouse input and converts clicks to tile interactions.
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
        int fbX = (sx * fbWidth) / windowWidth;
        int fbY = ((windowHeight - sy) * fbHeight) / windowHeight;

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
        int tileX = cam.screenToTileX(x, tileSize);
        int tileY = cam.screenToTileY(y, tileSize);

        Char c = map.getCharAt(tileX, tileY);
        if (c != null) {
            handleCharClick(c, tileX, tileY);
            return;
        }

        handleTileClick(tileX, tileY);
    }

    private void handleCharClick(Char c, int tileX, int tileY) {
        if (DevLoggingEnabled()) {
            ChatBox.AddMsg("Clicked on: " + c.displayName);
        }

        ArrayList<Integer> path = Pathfinder.findAdjacent(map,
                Player.tileX, Player.tileY, tileX, tileY);

        if (path == null) {
            if (DevLoggingEnabled()) {
                ChatBox.AddMsg("Can't reach " + c.displayName);
            }
            return;
        }

        if (path.isEmpty()) {
            if (DevLoggingEnabled()) {
                ChatBox.AddMsg("Already next to " + c.displayName);
            }
            /* TODO: initiate dialogue */
            return;
        }

        Player.setPath(path);
        /* TODO: store talk target for dialogue on arrival */
    }

    private void handleTileClick(int tileX, int tileY) {
        Tile tile = map.getTile((short) tileX, (short) tileY);

        if (tile == null) {
            if (DevLoggingEnabled()) {
                ChatBox.AddMsg("Click: no tile at [" + tileX + ", " + tileY + "]");
            }
            return;
        }

        if (tile.getExamine() != null) {
            ChatBox.AddMsg(tile.getExamine());
        }

        ArrayList<Integer> path;

        if (tile.blocked) {
            if (DevLoggingEnabled()) {
                ChatBox.AddMsg("Click: blocked [" + tileX + ", " + tileY + "]");
            }

            path = Pathfinder.findAdjacent(map,
                    Player.tileX, Player.tileY, tileX, tileY);

            if (path == null) {
                if (DevLoggingEnabled()) {
                    ChatBox.AddMsg("Unable to find path to adjacent tile.");
                }
            } else if (path.isEmpty()) {
                if (DevLoggingEnabled()) {
                    ChatBox.AddMsg("Click: already at [" + tileX + ", " + tileY + "]");
                }
            } else {
                if (DevLoggingEnabled()) {
                    ChatBox.AddMsg("Found path to adjacent tile: path length=" + path.size());
                }
                Player.setPath(path);
            }

            return;
        }

        path = Pathfinder.find(map, Player.tileX, Player.tileY, tileX, tileY);

        if (path == null) {
            if (DevLoggingEnabled()) {
                ChatBox.AddMsg("Click: no path to [" + tileX + ", " + tileY + "]");
            }
        } else if (path.isEmpty()) {
            if (DevLoggingEnabled()) {
                ChatBox.AddMsg("Click: already at [" + tileX + ", " + tileY + "]");
            }
        } else {
            if (DevLoggingEnabled()) {
                ChatBox.AddMsg("Click: path length=" + path.size());
            }
            Player.setPath(path);
        }
    }

    private void handleUiClick(int x, int y) {
        /* TODO */
    }
}