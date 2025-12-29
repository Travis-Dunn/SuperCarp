package production;

import production.sprite.SpriteAnim;
import production.sprite.SpriteSys;

public final class Player {
    static int spriteHandle;
    static SpriteAnim anim;

    // Logical position (tiles)
    static int tileX;
    static int tileY;

    // Previous position for interpolation
    static int prevTileX;
    static int prevTileY;

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
        float t = Data.tickAccumulator / Data.TICK_DURATION;
        t = easeOutQuad(t);

        int prevScreenX = prevTileX * Data.SPRITE_SIZE;
        int prevScreenY = prevTileY * Data.SPRITE_SIZE;
        int currScreenX = tileX * Data.SPRITE_SIZE;
        int currScreenY = tileY * Data.SPRITE_SIZE;

        int screenX = (int)(prevScreenX + (currScreenX - prevScreenX) * t);
        int screenY = (int)(prevScreenY + (currScreenY - prevScreenY) * t);

        SpriteSys.SetPosition(spriteHandle, screenX, screenY);
    }

    private static float easeOutQuad(float t) {
        return t * (2.0f - t);
    }
}