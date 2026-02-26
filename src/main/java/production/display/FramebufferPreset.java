package production.display;

public final class FramebufferPreset {
    public final int emulatedW, emulatedH;

    public FramebufferPreset(int emulatedW, int emulatedH) {
        this.emulatedW = emulatedW;
        this.emulatedH = emulatedH;
    }

    public static final String CLASS = FramebufferPreset.class.getSimpleName();
}