package production;

import production.sprite.SpriteAnim;
import production.sprite.SpriteCamera;
import production.sprite.SpriteSys;
import production.tiledmap.Tile;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;

public final class Player {
    static SpriteCamera cam;

    static int spriteHandle;
    static SpriteAnim anim;

    // Logical position (tiles)
    public static int tileX;
    public static int tileY;
    static int screenX, screenY;

    // Previous position for interpolation
    public static int prevTileX;
    public static int prevTileY;

    // Held state (persists until KEYUP)
    static boolean holdingLeft = false;
    static boolean holdingRight = false;
    static boolean holdingDown = false;
    static boolean holdingUp = false;

    // Queued move (survives until next tick consumes it)
    static int queuedDX = 0;
    static int queuedDY = 0;

    private Player() {}

    // Called from KEYDOWN handler
    static void queueMove(int dx, int dy) {
        queuedDX = dx;
        queuedDY = dy;
    }

    static void Update(float dt) {
        prevTileX = tileX;
        prevTileY = tileY;

        // Execute queued move (catches taps)
        if (queuedDX != 0 || queuedDY != 0) {

            int newX = tileX + queuedDX;
            int newY = tileY + queuedDY;

            Tile t = Data.tileMap.getTile((short)newX, (short)newY);

            if (t == null) {
                LogFatalAndExit(ErrStrTileNotFound(newX, newY));
                return;
            }

            if (t.blocked) {
                queuedDX = 0;
                queuedDY = 0;
                if (holdingLeft)       queueMove(-1, 0);
                else if (holdingRight) queueMove(1, 0);
                else if (holdingUp)    queueMove(0, -1);
                else if (holdingDown)  queueMove(0, 1);
                System.out.println("blocked");
                return;
            }

            tileX += queuedDX;
            tileY += queuedDY;
            queuedDX = 0;
            queuedDY = 0;
        }

        // If still holding, queue next move for next tick
        if (holdingLeft)       queueMove(-1, 0);
        else if (holdingRight) queueMove(1, 0);
        else if (holdingUp)    queueMove(0, -1);
        else if (holdingDown)  queueMove(0, 1);
    }

    static void Render() {
        assert(cam != null);

        float t = Data.tickAccumulator / Data.TICK_DURATION;
        t = easeOutQuad(t);

        int prevScreenX = prevTileX * Data.SPRITE_SIZE;
        int prevScreenY = prevTileY * Data.SPRITE_SIZE;
        int currScreenX = tileX * Data.SPRITE_SIZE;
        int currScreenY = tileY * Data.SPRITE_SIZE;

        screenX = (int)(prevScreenX + (currScreenX - prevScreenX) * t);
        screenY = (int)(prevScreenY + (currScreenY - prevScreenY) * t);

        cam.slave(screenX, screenY);
        SpriteSys.SetPosition(spriteHandle, screenX, screenY);
    }

    private static float easeOutQuad(float t) {
        return t * (2.0f - t);
    }

    public static final String CLASS = Player.class.getSimpleName();
    private static String ErrStrTileNotFound(int x, int y) {
        return String.format("Critical error! Tile not found [%d, %d].\n", x,
                y);
    }
}