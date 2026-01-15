package production.monster;

public final class MonsterDef {
    /* SpriteAnimDef */
    public final short atlasIndices[];
    public final short frameDurMs;
    public final boolean loops;

    public final String name;
    public final String displayName;

    public final int hp;

    MonsterDef(short[] atlasIndices, short frameDurMs, boolean loops,
               String name, String displayName, int hp) {
        this.atlasIndices = atlasIndices;
        this.frameDurMs = frameDurMs;
        this.loops = loops;
        this.name = name;
        this.displayName = displayName;
        this.hp = hp;
    }
}
