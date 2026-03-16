package production;

import production.carpscript.ScriptState;
import production.character.Char;
import production.sprite.SpriteAnim;
import production.sprite.SpriteCamera;
import production.sprite.SpritePool;
import production.tilemap.Tile;
import production.ui.Bitmap;
import whitetail.utility.FramerateManager;
import whitetail.utility.logging.LogLevel;

import java.util.ArrayList;

import static production.ui.BitmapRegistry.MISSING_PORTRAIT;
import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.Logger.LogSession;

public final class Player {
    static SpriteCamera cam;

    static int spriteHandle;
    static SpriteAnim anim;

    private static Bitmap portrait;

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

    // Path following
    static ArrayList<Integer> path = null;
    static int pathIndex = 0;

    private static Char dialogueTarget;

    private static ScriptState dialogueScript;

    private Player() {}

    // Called from KEYDOWN handler
    static void queueMove(int dx, int dy) {
        queuedDX = dx;
        queuedDY = dy;
    }

    static void Update(float dt) {
        prevTileX = tileX;
        prevTileY = tileY;

        /* path following takes priority over keyboard input */
        if (path != null && pathIndex < path.size()) {
            int nextPacked = path.get(pathIndex);
            int nextX = Pathfinder.unpackX(nextPacked);
            int nextY = Pathfinder.unpackY(nextPacked);

            /* verify tile is still walkable (in case world changed) */
            Tile t = Data.tileMap.getTile((short) nextX, (short) nextY);
            if (t == null || t.isBlocked()) {
                clearPath();
            } else {
                tileX = nextX;
                tileY = nextY;
                pathIndex++;

                /* clear keyboard queue while pathing */
                queuedDX = 0;
                queuedDY = 0;
                return;
            }
        }

        /* check if we arrived at a pending talk target */
        /* TODO: right now I believe that this is going to be a little more
        sophisticated. Specifically, we probably should check every frame to
        see if we are adjacent to the target, rather than completing the path
        and then doing the action. The reason for this is that the target might
        have moved. Same is going to be true for combat */
        if (path != null && pathIndex >= path.size()) {
            clearPath();

            /*
            if (DialogueManager.hasPendingTarget()) {
                Char target = DialogueManager.getPendingTarget();
                DialogueManager.start(target, target.dialogueRoot);
            }
             */

            if (dialogueTarget == null) return;
            ScriptState s = Data.scriptRunner.fireTrigger(Data.TRIGGER_DIALOGUE,
                    dialogueTarget.name, Data.playerVars);
            if (s != null) {
                Player.SetDialogueScriptState(s);
            }
        }

        // Execute queued move (catches taps)
        if (queuedDX != 0 || queuedDY != 0) {

            int newX = tileX + queuedDX;
            int newY = tileY + queuedDY;

            Tile t = Data.tileMap.getTile((short)newX, (short)newY);

            if (t == null) {
                LogFatalAndExit(ErrStrTileNotFound(newX, newY));
                return;
            }

            if (t.isBlocked()) {
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

        float t = (float)FramerateManager.InterpolationFactor();
        t = easeOutQuad(t);

        int prevScreenX = prevTileX * Data.SPRITE_SIZE;
        int prevScreenY = prevTileY * Data.SPRITE_SIZE;
        int currScreenX = tileX * Data.SPRITE_SIZE;
        int currScreenY = tileY * Data.SPRITE_SIZE;

        screenX = (int)(prevScreenX + (currScreenX - prevScreenX) * t);
        screenY = (int)(prevScreenY + (currScreenY - prevScreenY) * t);

        cam.slave(screenX, screenY);
        SpritePool.SetPosition(spriteHandle, screenX, screenY);
    }

    private static float easeOutQuad(float t) {
        return t * (2.0f - t);
    }

    static void setPath(ArrayList<Integer> newPath) {
        path = newPath;
        pathIndex = 0;
    }

    public static void clearPath() {
        path = null;
        pathIndex = 0;
    }

    public static void SetDialogueTarget(Char c) {
        if (c == null) {
            LogSession(LogLevel.WARNING, ERR_STR_DIALOGUE_TARGET_NULL);
            return;
        }
        dialogueTarget = c;
    }

    public static void ClearDialogueTarget() { dialogueTarget = null; }

    public static void SetDialogueScriptState(ScriptState s) {
        dialogueScript = s;
    }

    public static ScriptState GetDialogueScriptState() {
        return dialogueScript;
    }

    public static void ClearDialogueScriptState() { dialogueScript = null; }

    public static Bitmap GetPortrait() { return portrait; }
    public static void SetPortrait(Bitmap portrait) {
        if (portrait == null) {
            LogSession(LogLevel.WARNING, ERR_STR_PORTRAIT_NULL);
            Player.portrait = MISSING_PORTRAIT;
            return;
        }
        Player.portrait = portrait;
    }

    public static final String CLASS = Player.class.getSimpleName();
    private static String ErrStrTileNotFound(int x, int y) {
        return String.format("Critical error! Tile not found [%d, %d].\n", x,
                y);
    }
    private static final String ERR_STR_DIALOGUE_TARGET_NULL = CLASS +
            " tried to set dialogue target to null.\n";
    private static final String ERR_STR_PORTRAIT_NULL = CLASS + " tried to " +
            "portrait to null.\n";
}