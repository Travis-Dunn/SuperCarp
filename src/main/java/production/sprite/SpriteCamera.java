package production.sprite;

public class SpriteCamera {
    private boolean init;

    /* viewport dimensions in pixels */
    private int viewW;
    private int viewH;

    /* camera position in world/pixel space */
    private float posX;
    private float posY;

    /* movement speed in pixels per second */
    private float moveSpeed;

    /* translation state (set/cleared by events, persists until changed) */
    private boolean translatingLeft;
    private boolean translatingRight;
    private boolean translatingUp;
    private boolean translatingDown;

    /* whether translation is currently allowed */
    private boolean canTranslate;

    private static final float DEF_MOVE_SPEED = 200.0f;

    public SpriteCamera() {}

    public boolean init(int viewportWidth, int viewportHeight) {
        assert(!init);
        assert(viewportWidth > 0);
        assert(viewportHeight > 0);

        viewW = viewportWidth;
        viewH = viewportHeight;
        posX = 0.0f;
        posY = 0.0f;
        moveSpeed = DEF_MOVE_SPEED;
        translatingLeft = false;
        translatingRight = false;
        translatingUp = false;
        translatingDown = false;
        canTranslate = true;

        return init = true;
    }

    public boolean init(int viewportWidth, int viewportHeight, float moveSpeed) {
        assert(!init);
        assert(viewportWidth > 0);
        assert(viewportHeight > 0);
        assert(moveSpeed >= 0.0f);

        viewW = viewportWidth;
        viewH = viewportHeight;
        posX = 0.0f;
        posY = 0.0f;
        this.moveSpeed = moveSpeed;
        translatingLeft = false;
        translatingRight = false;
        translatingUp = false;
        translatingDown = false;
        canTranslate = true;

        return init = true;
    }

    /**
     * Call during onUpdate. Applies translation based on current state.
     * @param dt delta time in seconds
     */
    public void update(float dt) {
        assert(init);

        if (!canTranslate) return;

        float delta = moveSpeed * dt;

        if (translatingLeft)  posX -= delta;
        if (translatingRight) posX += delta;
        if (translatingUp)    posY -= delta;
        if (translatingDown)  posY += delta;
    }

    /* --- translation state setters (call on KEYDOWN/KEYUP events) --- */

    public void setTranslatingLeft(boolean state) {
        assert(init);

        translatingLeft = state;
    }

    public void setTranslatingRight(boolean state) {
        assert(init);

        translatingRight = state;
    }

    public void setTranslatingUp(boolean state) {
        assert(init);

        translatingUp = state;
    }

    public void setTranslatingDown(boolean state) {
        assert(init);

        translatingDown = state;
    }

    public void clearTranslationState() {
        assert(init);

        translatingLeft = false;
        translatingRight = false;
        translatingUp = false;
        translatingDown = false;
    }

    /* --- immediate setters (bypass state model) --- */

    public void setPos(float x, float y) {
        assert(init);

        posX = x;
        posY = y;
    }

    public void setX(float x) {
        assert(init);

        posX = x;
    }

    public void setY(float y) {
        assert(init);

        posY = y;
    }

    public void translate(float dx, float dy) {
        assert(init);

        posX += dx;
        posY += dy;
    }

    public void setMoveSpeed(float speed) {
        assert(init);
        assert(speed >= 0.0f);

        moveSpeed = speed;
    }

    public void setCanTranslate(boolean canTranslate) {
        assert(init);

        this.canTranslate = canTranslate;
    }

    /* --- getters --- */

    public float getX() {
        assert(init);

        return posX;
    }

    public float getY() {
        assert(init);

        return posY;
    }

    public int getViewW() {
        assert(init);

        return viewW;
    }

    public int getViewH() {
        assert(init);

        return viewH;
    }

    public float getMoveSpeed() {
        assert(init);

        return moveSpeed;
    }

    public boolean getCanTranslate() {
        assert(init);

        return canTranslate;
    }

    public boolean isTranslatingLeft() {
        assert(init);

        return translatingLeft;
    }

    public boolean isTranslatingRight() {
        assert(init);

        return translatingRight;
    }

    public boolean isTranslatingUp() {
        assert(init);

        return translatingUp;
    }

    public boolean isTranslatingDown() {
        assert(init);

        return translatingDown;
    }

    public boolean isTranslating() {
        assert(init);

        return translatingLeft || translatingRight || translatingUp || translatingDown;
    }

    public boolean isInitialized() {
        return init;
    }

    public void shutdown() {
        assert(init);

        init = false;
    }
}