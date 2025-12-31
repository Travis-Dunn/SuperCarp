package production.tiledmap;

public final class Tile {
    public final short spriteIdx;
    /* TODO: fix this also */
    public int spriteHandle;
    public final boolean blocked;

    Tile(short spriteIdx, int handle, boolean blocked) {
        this.spriteIdx = spriteIdx;
        this.spriteHandle = handle;
        this.blocked = blocked;
    }
}