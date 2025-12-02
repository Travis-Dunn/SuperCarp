package whitetail.platform;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.Display;

import static whitetail.utility.ErrorHandler.LogFatalExcpAndExit;

public class Window {

    private String title;
    private int width;
    private int height;
    private boolean init;
    private boolean fullscreen;

    private static final String ERR_STR_FAIL_CREATE =
            "Failed to initialize window.";

    public Window() { init = false; }

    public boolean init(String title, int width, int height, boolean vSync,
                        boolean fullscreen) {
        this.title = title != null ? title : "";
        this.width = width;
        this.height = height;
        this.fullscreen = fullscreen;

        DisplayMode displayMode;
        if (!fullscreen) {
            displayMode = new DisplayMode(width, height);
        } else {
            displayMode = Display.getDesktopDisplayMode();
        }

        try {
            Display.setDisplayMode(displayMode);
            Display.setFullscreen(fullscreen);
            Display.setTitle(title);
            Display.setVSyncEnabled(vSync);
            Display.create();

            return (init = true);
        } catch (LWJGLException e) {
            LogFatalExcpAndExit(ERR_STR_FAIL_CREATE, e);
            return false;
        }
    }

    public void setVSyncEnabled(boolean enabled) {
        Display.setVSyncEnabled(enabled);
    }

    public void pollEvents() {
        Display.processMessages();
    }

    public boolean shouldClose() {
        return Display.isCloseRequested();
    }

    public void swapBuffers() {
        Display.update();
    }

    public void destroy() {
        if (init) {
            Display.destroy();
            init = false;
        }
    }

    public String   getTitle()      { return title; }
    public int      getWidth()      { return width; }
    public int      getHeight()     { return height; }
    public boolean  getInit()       { return init; }
}
