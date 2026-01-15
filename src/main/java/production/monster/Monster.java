package production.monster;

import production.sprite.SpriteAnim;

public final class Monster {
    public final MonsterSpawn spawn;
    public final int spriteHandle;
    public final SpriteAnim anim;
    public final String displayName;

    private int tileX, tileY;
    private int hp;

    Monster(MonsterSpawn spawn, int spriteHandle, SpriteAnim anim,
            String displayName, int tileX, int tileY, int hp) {
        this.spawn = spawn;
        this.spriteHandle = spriteHandle;
        this.anim = anim;
        this.displayName = displayName;
        this.tileX = tileX;
        this.tileY = tileY;
        this.hp = hp;
    }

    public int getTileX() { return tileX; }
    public int getTileY() { return tileY; }
    public int getHP() { return hp; }
    public void setTileX(int x) { tileX = x; }
    public void setTileY(int y) { tileY = y; }
    public void setHP(int hp) { this.hp = hp; }
}
