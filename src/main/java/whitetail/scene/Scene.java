package whitetail.scene;

public abstract class Scene {
    protected boolean init = false;

    /* Called once when the scene is entered for the first time */
    public abstract void onInit();

    /* Called each time the scene is transitioned to */
    public abstract void onEnter();

    /* Called each time the scene is transitioned from */
    public abstract void onExit();

    /* Called each frame if the scene is the active scene */
    public abstract void onUpdate(double delta);

    /* Called each frame if the scene is the active scene */
    public abstract void onRender();

    /* Called each frame if the scene is the active scene */
    public abstract void onInput();

    /* Call if the scene will not be needed again */
    public abstract void onDestroy();
}
