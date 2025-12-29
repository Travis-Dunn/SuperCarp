package production;

import production.sprite.SpriteAnim;
import production.sprite.SpriteSys;

/**
 * Very much exploratory
 */
public final class Player {
    static int spriteHandle;
    static SpriteAnim anim;
    static int tileX;
    static int tileY;
    static boolean moveLeft = false;
    static boolean moveRight = false;
    static boolean moveDown = false;
    static boolean moveUp = false;

    private Player() {}

    static void Update(float dt) {
        if (moveLeft) tileX--;
        if (moveRight) tileX++;
        if (moveDown) tileY++;
        if (moveUp) tileY--;
        moveLeft = false;
        moveRight = false;
        moveDown = false;
        moveUp = false;
    }

    static void Render() {
        SpriteSys.SetX(spriteHandle, tileX * Data.SPRITE_SIZE);
        SpriteSys.SetY(spriteHandle, tileY * Data.SPRITE_SIZE);
    }
}
