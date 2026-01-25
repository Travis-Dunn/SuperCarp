package production.character;

import production.sprite.SpriteAnim;
import production.sprite.SpriteAnimDef;
import production.sprite.SpriteAnimSys;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;

public final class Char {
    private int spriteHandle;
    public final SpriteAnim anims[];
    public int tileX, tileY;

    public final String name;
    public final String displayName;

    public final short atlasIndices[][];
    public final short frameDurMs[];
    public final boolean loops[];
    public final int animCount;

    Char(String name, String displayName, short atlasIndices[][]
            , short frameDurMs[], boolean loops[]) {
        assert(atlasIndices != null);
        assert(frameDurMs != null);
        assert(loops != null);
        assert(displayName != null);
        assert(name != null);

        this.name = name;
        this.displayName = displayName;
        this.atlasIndices = atlasIndices;
        this.frameDurMs = frameDurMs;
        this.loops = loops;
        this.anims = constructAnims();
        this.animCount = this.loops.length;
    }

    private SpriteAnim[] constructAnims() {
        int i = loops.length;
        SpriteAnim array[] = new SpriteAnim[i];

        if (!(frameDurMs.length == i && i == atlasIndices.length)) {
            LogFatalAndExit(errStrAnimInfoMismatch());
        }

        for (i = 0; i < frameDurMs.length; ++i) {
            array[0] = SpriteAnimSys.Generate(
                    -1, new SpriteAnimDef(atlasIndices[i],
                            frameDurMs[i], loops[i]));
        }
        return array;
    }

    public void setSpriteHandle(int handle) {
        int i;
        spriteHandle = handle;
        for (i = 0; i < animCount; ++i) {
            SpriteAnimSys.SetSpriteHandle(anims[i], handle);
        }
    }

    public static final String CLASS = Char.class.getSimpleName();
    private String errStrAnimInfoMismatch() {
        return String.format("%s construction failed because there were [%d] " +
                "sets of atlas indices, [%d] frame durations, and [%d] loops " +
                "bools. There must be the same number of each.\n", CLASS,
                atlasIndices.length, frameDurMs.length, loops.length);
    }
}
