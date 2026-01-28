package production.script;

import production.GameCtx;
import production.Player;

public final class Scripts {
    public static final Script TEST_TELEPORT = new Script() {
        @Override
        public void run() {
            Player.clearPath();
            Player.tileX = 4;
            Player.tileY = -3;
            Player.prevTileX = 4;
            Player.prevTileY = -3;
        }
    };
}
