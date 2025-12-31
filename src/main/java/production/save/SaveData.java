package production.save;

import production.Player;

/**
 * Immutable snapshot of game state for saving.
 * Captured on the main thread, consumed by the save thread.
 */
public final class SaveData {
    public final int playerTileX;
    public final int playerTileY;

    public SaveData(int playerTileX, int playerTileY) {
        this.playerTileX = playerTileX;
        this.playerTileY = playerTileY;
    }

    /**
     * Capture current game state into a SaveData snapshot.
     * Must be called from the main thread.
     */
    public static SaveData Capture() {
        return new SaveData(Player.tileX, Player.tileY);
    }
}