package production.sprite;

// Mutable instance — one per animated entity
public final class SpriteAnim {
    private int spriteHandle;
    private SpriteAnimDef def;

    private int frame;
    private int elapsedMs;
    private boolean playing;
    private boolean finished;

    SpriteAnim(int spriteHandle, SpriteAnimDef def) {
        assert SpriteSys.IsValid(spriteHandle);
        assert def != null;

        this.spriteHandle = spriteHandle;
        this.def = def;
        this.frame = 0;
        this.elapsedMs = 0;
        this.playing = true;
        this.finished = false;

        // Set initial frame immediately
        SpriteSys.SetAtlasIdx(spriteHandle, def.frames[0]);
    }

    void update(int dtMs) {
        if (!playing || finished) return;

        elapsedMs += dtMs;

        while (elapsedMs >= def.frameDurationMs) {
            elapsedMs -= def.frameDurationMs;
            frame++;

            if (frame >= def.frames.length) {
                if (def.loops) {
                    frame = 0;
                } else {
                    frame = def.frames.length - 1;
                    finished = true;
                    break;
                }
            }
        }

        SpriteSys.SetAtlasIdx(spriteHandle, def.frames[frame]);
    }

    public void play()  { playing = true; }
    public void pause() { playing = false; }

    public void reset() {
        frame = 0;
        elapsedMs = 0;
        finished = false;
        SpriteSys.SetAtlasIdx(spriteHandle, def.frames[0]);
    }

    public void setDef(SpriteAnimDef def) {
        assert def != null;
        this.def = def;
        reset();
    }

    public void setSpriteHandle(int handle) {
        assert SpriteSys.IsValid(handle);
        this.spriteHandle = handle;
        SpriteSys.SetAtlasIdx(spriteHandle, def.frames[frame]);
    }

    public boolean isPlaying()  { return playing; }
    public boolean isFinished() { return finished; }
    public int getFrame()       { return frame; }
    public int getSpriteHandle() { return spriteHandle; }
    public SpriteAnimDef getDef() { return def; }
}