package production.framebuffer;

public final class FramebufferPreset {
    public final int emulatedW, emulatedH;
    public final int viewportW, viewportH;
    public final int viewportX, viewportY;

    FramebufferPreset(int emulatedW, int emulatedH,
                      int viewportW, int viewportH,
                      int viewportX, int viewportY) {
        this.emulatedW = emulatedW;
        this.emulatedH = emulatedH;
        this.viewportW = viewportW;
        this.viewportH = viewportH;
        this.viewportX = viewportX;
        this.viewportY = viewportY;
    }

    public static final String CLASS = FramebufferPreset.class.getSimpleName();
}
