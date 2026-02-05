package whitetail.platform;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import whitetail.utility.logging.LogLevel;

import static whitetail.utility.ErrorHandler.LogFatalExcpAndExit;
import static whitetail.utility.logging.Logger.LogSession;

/**
 * Wraps LWJGL 2 Display into an init/destroy lifecycle.
 *
 * Supports windowed, fullscreen, and undecorated (borderless) modes.
 * Undecorated windows are centered on the primary monitor by default.
 *
 * When undecorated, the caller is responsible for providing its own
 * title bar, close button, and drag behavior. Use {@link #setLocation}
 * to reposition the window in response to drag input.
 */
public final class Window {
    private boolean init;

    private String title;
    private int width;
    private int height;
    private boolean fullscreen;
    private boolean undecorated;

    public Window() { init = false; }

    /**
     * Initialize and create the window.
     *
     * @param title       window title (ignored when undecorated, but still
     *                    set for taskbar / alt-tab)
     * @param width       window width in pixels (ignored when fullscreen)
     * @param height      window height in pixels (ignored when fullscreen)
     * @param vSync       enable vertical sync
     * @param fullscreen  exclusive fullscreen mode
     * @param undecorated remove OS window decorations (borderless); ignored
     *                    when fullscreen
     */
    public boolean init(String title, int width, int height, boolean vSync,
                        boolean fullscreen, boolean undecorated) {
        assert(!init);

        this.title = title != null ? title : "";
        this.width = width;
        this.height = height;
        this.fullscreen = fullscreen;
        /* meaningless in fullscreen, store the intent anyway. */
        this.undecorated = !fullscreen && undecorated;

        System.setProperty("org.lwjgl.opengl.Window.undecorated",
                this.undecorated ? "true" : "false");

        DisplayMode displayMode;
        if (fullscreen) {
            displayMode = Display.getDesktopDisplayMode();
        } else {
            displayMode = new DisplayMode(width, height);
        }

        try {
            Display.setDisplayMode(displayMode);
            Display.setFullscreen(fullscreen);
            Display.setTitle(this.title);
            Display.setVSyncEnabled(vSync);
            Display.create();
        } catch (LWJGLException e) {
            LogFatalExcpAndExit(ErrStrFailedCreate(e.getMessage()), e);
            return false;
        }

        if (!fullscreen) {
            centerOnScreen();
        }

        LogSession(LogLevel.DEBUG, LogStrCreated(displayMode));

        return init = true;
    }

    /**
     * Center the window on the primary monitor.
     * No-op in fullscreen.
     */
    public void centerOnScreen() {
        if (fullscreen) return;

        DisplayMode desktop = Display.getDesktopDisplayMode();
        int x = (desktop.getWidth() - width) / 2;
        int y = (desktop.getHeight() - height) / 2;
        Display.setLocation(x, y);
    }

    /**
     * Move the window to an absolute screen position.
     * Intended for implementing drag on undecorated windows.
     */
    public void setLocation(int x, int y) {
        assert(init);

        Display.setLocation(x, y);
    }

    public void setVSyncEnabled(boolean enabled) {
        assert(init);

        Display.setVSyncEnabled(enabled);
    }

    public void pollEvents() {
        assert(init);

        Display.processMessages();
    }

    public boolean shouldClose() {
        assert(init);

        return Display.isCloseRequested();
    }

    public void swapBuffers() {
        assert(init);

        Display.update();
    }

    public void destroy() {
        if (!init) return;

        Display.destroy();

        /* Reset the property so it doesn't leak into anything that might
           create a display later in the same JVM session. */
        System.setProperty("org.lwjgl.opengl.Window.undecorated", "false");

        init = false;

        LogSession(LogLevel.DEBUG, CLASS + " destroyed.\n");
    }

    /* --- getters --- */

    public String   getTitle()          { return title; }
    public int      getWidth()          { return width; }
    public int      getHeight()         { return height; }
    public boolean  isInitialized()     { return init; }
    public boolean  isFullscreen()      { return fullscreen; }
    public boolean  isUndecorated()     { return undecorated; }

    /* --- error / log strings --- */

    public static final String CLASS = Window.class.getSimpleName();

    private static String ErrStrFailedCreate(String msg) {
        return String.format("%s failed to create the display: %s\n",
                CLASS, msg);
    }

    private static String LogStrCreated(DisplayMode mode) {
        return String.format("%s created [%dx%d].\n",
                CLASS, mode.getWidth(), mode.getHeight());
    }
}