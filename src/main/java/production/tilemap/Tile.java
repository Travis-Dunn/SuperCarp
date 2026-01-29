package production.tilemap;

public final class Tile {
    public final short spriteIdx;
    /* TODO: fix this also */
    public int spriteHandle;
    private boolean blocked;
    private String examine;

    Tile(short spriteIdx, int handle, boolean blocked) {
        this.spriteIdx = spriteIdx;
        this.spriteHandle = handle;
        this.blocked = blocked;
        this.examine = null;
    }

    public String getExamine() {
        return examine;
    }

    /** Package-private setter for use during map loading. */
    void setExamine(String examine) {
        this.examine = examine;
    }

    public void setBlocked(boolean b) { blocked = b; }
    public boolean isBlocked() { return blocked; }
}