package production.sprite;

public class SpriteCamera {
    private boolean init;

    /* viewport dimensions in pixels */
    private int viewW;
    private int viewH;

    /* camera position in world/pixel space */
    private float posX;
    private float posY;

    public SpriteCamera() {}

    public boolean Init(int viewportWidth, int viewportHeight) {
        assert(!init);
        assert(viewportWidth > 0);
        assert(viewportHeight > 0);

        viewW = viewportWidth;
        viewH = viewportHeight;
        posX = 0.0f;
        posY = 0.0f;

        return init = true;
    }

    public void SetPos(float x, float y) {
        assert(init);

        posX = x;
        posY = y;
    }

    public void SetX(float x) {
        assert(init);

        posX = x;
    }

    public void SetY(float y) {
        assert(init);

        posY = y;
    }

    public void Move(float dx, float dy) {
        assert(init);

        posX += dx;
        posY += dy;
    }

    public float GetX() {
        assert(init);

        return posX;
    }

    public float GetY() {
        assert(init);

        return posY;
    }

    public int GetViewW() {
        assert(init);

        return viewW;
    }

    public int GetViewH() {
        assert(init);

        return viewH;
    }

    public void Shutdown() {
        assert(init);

        init = false;
    }
}