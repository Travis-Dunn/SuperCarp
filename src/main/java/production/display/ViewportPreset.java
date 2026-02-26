package production.display;

public final class ViewportPreset {
    public final int x, y, w, h;

    public ViewportPreset(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public static final String CLASS = ViewportPreset.class.getSimpleName();
}