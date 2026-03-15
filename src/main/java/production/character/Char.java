package production.character;

import production.dialogue.DialogueNode;
import production.sprite.SpriteAnim;
import production.sprite.SpriteAnimDef;
import production.sprite.SpriteAnimSys;
import production.ui.Bitmap;
import whitetail.utility.logging.LogLevel;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.logging.Logger.LogSession;

public final class Char {
    private int spriteHandle;
    public final SpriteAnim anims[];
    public int tileX, tileY;

    public final String name;
    public final String displayName;
    private Bitmap portrait;

    public final short atlasIndices[][];
    public final short frameDurMs[];
    public final boolean loops[];
    public final int animCount;

    public DialogueNode dialogueRoot;

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

    public void setPortrait(Bitmap portrait) {
        if (portrait == null) {
            LogSession(LogLevel.WARNING, ERR_STR_PORTRAIT_NULL);
            return;
        }

        this.portrait = portrait;
    }

    public Bitmap getPortrait() { return portrait; }

    public static final String CLASS = Char.class.getSimpleName();
    private String errStrAnimInfoMismatch() {
        return String.format("%s construction failed because there were [%d] " +
                "sets of atlas indices, [%d] frame durations, and [%d] loops " +
                "bools. There must be the same number of each.\n", CLASS,
                atlasIndices.length, frameDurMs.length, loops.length);
    }
    private static final String ERR_STR_PORTRAIT_NULL = CLASS + " tried to " +
            "portrait to null.\n";
}
