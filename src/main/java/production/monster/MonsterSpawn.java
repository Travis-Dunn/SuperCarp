package production.monster;

import production.Data;
import production.sprite.SpriteAnim;
import production.sprite.SpriteAnimDef;
import production.sprite.SpriteAnimSys;
import production.sprite.SpritePool;
import whitetail.utility.FramerateManager;

public final class MonsterSpawn {
    public final int x, y;
    public final MonsterDef monsterDef;
    public final int respawnTicks;

    private boolean isAlive;
    private int lastDeath;
    private Monster monster;

    public MonsterSpawn(int x, int y, MonsterDef monsterDef, int respawnTicks) {
        this.x = x;
        this.y = y;
        this.monsterDef = monsterDef;
        this.respawnTicks = respawnTicks;
        this.isAlive = false;
        this.lastDeath = -1000;
        this.monster = null;
    }

    public void update() {
        if (isAlive) return;
        if ((FramerateManager.GetTickCount() - lastDeath) > respawnTicks) {
            spawn();
        }
    }

    private void spawn() {
        isAlive = true;

        int spriteHandle = SpritePool.Create(x * Data.SPRITE_SIZE,
                y * Data.SPRITE_SIZE,
                Data.PLAYER_ATLAS, 0, 0, Data.MAP_PALETTE,
                false, false, true);

        SpriteAnim anim = SpriteAnimSys.Create(spriteHandle, new SpriteAnimDef(
                monsterDef.atlasIndices,
                monsterDef.frameDurMs,
                monsterDef.loops
        ));

        monster = new Monster(this, spriteHandle, anim,
                monsterDef.displayName, x, y, monsterDef.hp);
    }

    private void despawn() {
        isAlive = false;
        lastDeath = (int)FramerateManager.GetTickCount();
    }
}
