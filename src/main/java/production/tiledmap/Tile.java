package production.tiledmap;

public final class Tile {
    public final short spriteIdx;
    /* TODO: fix this also */
    public int spriteHandle;

    Tile(short spriteIdx, int handle) {
        this.spriteIdx = spriteIdx;
        this.spriteHandle = handle;
    }
}