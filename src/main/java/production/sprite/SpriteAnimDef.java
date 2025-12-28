package production.sprite;

public final class SpriteAnimDef {
    public final short[] frames;        // atlas indices
    public final short frameDurationMs; // uniform timing
    public final boolean loops;

    public SpriteAnimDef(short[] frames, short frameDurationMs, boolean loops) {
        assert frames != null && frames.length > 0;
        assert frameDurationMs > 0;

        this.frames = frames;
        this.frameDurationMs = frameDurationMs;
        this.loops = loops;
    }
}