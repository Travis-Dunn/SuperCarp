package whitetail.core;

import org.lwjgl.input.Keyboard;
import whitetail.audio.AudioContext;
import whitetail.event.EventType;
import whitetail.event.KeyboardEvent;
import whitetail.graphics.RenderContext;
import whitetail.event.EventManager;
import whitetail.loaders.config.ConfigEntry;
import whitetail.loaders.config.ConfigFileParser;
import whitetail.loaders.config.TuningFileParser;
import whitetail.platform.Window;
import whitetail.utility.ErrorHandler;
import whitetail.utility.FramerateManager;
import whitetail.utility.GCPriest;
import whitetail.utility.logging.LogLevel;
import whitetail.utility.logging.Logger;

import java.util.ArrayList;

import static whitetail.utility.ErrorHandler.LogFatalAndExit;
import static whitetail.utility.ErrorHandler.LogFatalExcpAndExit;
import static whitetail.utility.logging.Logger.LogSession;

public abstract class GameEngine {
    protected Window window;
    protected EventManager eventManager;
    protected GameState currentState;
    protected boolean running;
    protected boolean init;
    protected boolean initFirstHalf = false;
    protected boolean initSecondHalf = false;

    private static final int    DEFAULT_WIN_WIDTH =     640;
    private static final int    DEFAULT_WIN_HEIGHT =    480;

    private static final String ERR_STR_FAILED_INIT_GAME_ENGINE =
            "Failed to initialize game engine.";
    private static final String ERR_STR_FAILED_INIT_WINDOW =
            "Failed to initialize window.";
    private static final String ERR_STR_FAILED_INIT_RENDER_CONTEXT =
            "Failed to initialize openGL render context.";
    private static final String ERR_STR_FAILED_INIT_LOGGER =
            "Failed to initialize log file manager.";
    private static final String ERR_STR_FAILED_INIT_ERROR_HANDLER =
            "Failed to initialize error handler.";
    private static final String ERR_STR_FAILED_INIT =
            "Failed to initialize.";
    private static final String ERR_STR_FAILED_INIT_FIRST_HALF;
    private static final String ERR_STR_FAILED_INIT_SECOND_HALF;
    private static final String ERR_STR_FAILED_INIT_GCPRIEST;
    private static final String ERR_STR_FAILED_INIT_CFG_FILE =
            "Failed to initialize config file manager";
    private static final String ERR_STR_FAILED_INIT_TNG_FILE =
            "Failed to initialize tuning file manager";
    private static final String ERR_STR_CFG_FILE_OPT_OUT =
            "Not using config file.";
    private static final String ERR_STR_TNG_FILE_OPT_OUT =
            "Not using tuning file.";
    private static final String ERR_STR_WIN_WIDTH_OUT_OF_RANGE =
            "Window width was not greater than zero. Using default value [" +
            DEFAULT_WIN_WIDTH + "].";
    private static final String ERR_STR_WIN_HEIGHT_OUT_OF_RANGE =
            "Window height was not greater than zero. Using default value [" +
            DEFAULT_WIN_HEIGHT + "].";
    private static final String ERR_STR_FAILED_INIT_GAME =
            "Failed to initialize game";
    private static final String ERR_STR_VSYNC_FPS_INCOMPATIBLE =
            "VSync and Fps settings incompatible. \n If VSync is enabled, Fps" +
                    " must be set to 0. If vSync is disabled, must be greater" +
                    " than zero.";
    private static final String ERR_STR_FAILED_INIT_FRAMERATE_MANAGER =
            "Failed to initialize framerate manager";

    public GameEngine() {
        this.eventManager = new EventManager();
        this.running = false;
        this.init = false;
    }

    public final boolean initFirstHalf() {
        assert(!initFirstHalf);
        assert(!initSecondHalf);

        currentState = GameState.INITIALIZING_FIRST_HALF;

        try {
            if (!Logger.Init()) {
                System.err.println(ERR_STR_FAILED_INIT_LOGGER);
                return initFirstHalf = false;
            }
            if (!ErrorHandler.Init(this)) {
                System.err.println(ERR_STR_FAILED_INIT_ERROR_HANDLER);
                return initFirstHalf = false;
            }
            return initFirstHalf = true;
        } catch (Exception e) {
            System.err.println(ERR_STR_FAILED_INIT_FIRST_HALF + "\n" +
                    e.getMessage());
            e.printStackTrace();
            return initFirstHalf = false;
        }
   }

    public final boolean initSecondHalf(
            /* null if not used */
            String winTitle,
            /* must be > 0 */
            int winWidth,
            /* must be > 0 */
            int winHeight,
            /* 0 to disable framerate throttling and allow for vsync,
            otherwise must be > 0 */
            int fps,
            /* if true, fps must be 0, if false fps may or may not be 0 */
            boolean vSync,
            boolean windowed) {
        assert(initFirstHalf);
        assert(!initSecondHalf);

        currentState = GameState.INITIALIZING_SECOND_HALF;
        try {
            if ((vSync && !(fps == 0)) || (!vSync && fps < 0)) {
                LogFatalAndExit(ERR_STR_VSYNC_FPS_INCOMPATIBLE);
                return initSecondHalf = false;
            }
            if (!(winWidth > 0)) {
                LogSession(LogLevel.WARNING, ERR_STR_WIN_WIDTH_OUT_OF_RANGE);
                winWidth = DEFAULT_WIN_WIDTH;
            }
            if (!(winHeight > 0)) {
                LogSession(LogLevel.WARNING, ERR_STR_WIN_HEIGHT_OUT_OF_RANGE);
                winHeight = DEFAULT_WIN_HEIGHT;
            }
            if (!(window = new Window()).init(winTitle, winWidth, winHeight,
                    vSync, windowed)) {
                LogFatalAndExit(ERR_STR_FAILED_INIT_WINDOW);
                return initSecondHalf = false;
            }
            if (!RenderContext.Init(winWidth, winHeight)) {
                LogFatalAndExit(ERR_STR_FAILED_INIT_RENDER_CONTEXT);
                return initSecondHalf = false;
            }
            if (!FramerateManager.Init(fps)) {
                LogFatalAndExit(ERR_STR_FAILED_INIT_FRAMERATE_MANAGER);
                return initSecondHalf = false;
            }
            if (!AudioContext.Init()) {
                LogFatalAndExit(ERR_STR_FAILED_INIT_AUDIO);
                return initSecondHalf = false;
            }
            if (!GCPriest.Init()) {
                LogFatalAndExit(ERR_STR_FAILED_INIT_GCPRIEST);
                return initSecondHalf = false;
            }
            if (!onInit()) {
                LogFatalAndExit(ERR_STR_FAILED_INIT_GAME);
                return initSecondHalf = false;
            }
            return initSecondHalf = true;
        } catch (Exception e) {
            LogFatalExcpAndExit(ERR_STR_FAILED_INIT_SECOND_HALF, e);
            return initSecondHalf = false;
        }
    }

    public final boolean init(
            /* null if not using config file */
            ArrayList<ConfigEntry> cfgEntries,
            /* null if not using tuning file */
            ArrayList<ConfigEntry> tngEntries,
            /* null if not used */
            String winTitle,
            /* must be > 0 */
            int winWidth,
            /* must be > 0 */
            int winHeight,
            /* 0 to disable framerate throttling, otherwise must be > 0 */
            int fps) {
        try {

            if (!Logger.Init()) {
                System.err.println(ERR_STR_FAILED_INIT_LOGGER);
                return init = false;
            }
            if (!ErrorHandler.Init(this)) {
                System.err.println(ERR_STR_FAILED_INIT_ERROR_HANDLER);
                return init = false;
            }
            if (cfgEntries != null && !ConfigFileParser.Init(cfgEntries)) {
                LogSession(LogLevel.WARNING, ERR_STR_FAILED_INIT_CFG_FILE);
            } else LogSession(LogLevel.DEBUG, ERR_STR_CFG_FILE_OPT_OUT);

            if (tngEntries != null && !TuningFileParser.Init(tngEntries)) {
                LogSession(LogLevel.WARNING, ERR_STR_FAILED_INIT_TNG_FILE);
            } else LogSession(LogLevel.DEBUG, ERR_STR_TNG_FILE_OPT_OUT);

            if (!(winWidth > 0)) {
                LogSession(LogLevel.WARNING, ERR_STR_WIN_WIDTH_OUT_OF_RANGE);
                winWidth = DEFAULT_WIN_WIDTH;
            }
            if (!(winHeight > 0)) {
                LogSession(LogLevel.WARNING, ERR_STR_WIN_HEIGHT_OUT_OF_RANGE);
                winHeight = DEFAULT_WIN_HEIGHT;
            }
            /*
            if (!(window = new Window()).init(winTitle, winWidth, winHeight)) {
                LogFatalAndExit(ERR_STR_FAILED_INIT_GAME_ENGINE);
                return init = false;
            }

             */
            if (!RenderContext.Init(winWidth, winHeight)) {
                LogFatalAndExit(ERR_STR_FAILED_INIT_GAME_ENGINE);
                return init = false;
            }

            if (!FramerateManager.Init(fps)) {
                LogFatalAndExit(ERR_STR_FAILED_INIT_GAME_ENGINE);
                return init = false;
            }

            if (!onInit()) {
                LogFatalAndExit(ERR_STR_FAILED_INIT_GAME);
                return init = false;
            }


            return init = true;
        } catch (Exception e) {
            System.err.println(ERR_STR_FAILED_INIT + "\n" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /* TODO: Find out how to make the program run with a console
       TODO: Fix Framerate manager
     */

    public final void run() {
        assert(initFirstHalf);
        assert(initSecondHalf);

        running = true;
        currentState = GameState.RUNNING;

        System.out.println("Starting Whitetail...");

        while (running && !window.shouldClose()) {
            FramerateManager.Update();

            eventManager.processEvents();
            processInput();
            onUpdate(FramerateManager.deltaTime);
            render();
        }
        shutdown();
    }

    private void processInput() {
        window.pollEvents();

        if (window.shouldClose()) {
            stop();
        }

        onProcessInput();
    }

    protected void stop() {
        running = false;
    }

    private void shutdown() {
        System.out.println("Shutting down Whitetail...");
        currentState = GameState.SHUTDOWN;

        onShutdown();
        RenderContext.Shutdown();
        AudioContext.Shutdown();

        if (window != null) window.destroy();

        eventManager.shutdown();
        Logger.Shutdown();
        System.out.println("Engine shutdown complete.");
    }

    private void render() {
        RenderContext.BeginFrame();
        RenderContext.Render();
        onRender();
    }

    protected abstract void     onProcessInput();
    protected abstract boolean  onInit();
    protected abstract void     onUpdate(double delta);
    protected abstract void     onRender();
    protected abstract void     onShutdown();

    public Window           getWindow()         { return window; }
    public EventManager     getEventManager()   { return eventManager; }
    public GameState        getCurrentState()   { return currentState; }
    public boolean          isRunning()         { return running; }

    public void setIsRunning(boolean r) { running = r; }

    private static final String CLASS = GameEngine.class.getSimpleName();
    private static final String ERR_STR_FAILED_INIT_AUDIO = String.format(
            "The [%s] failed the second half of initialization because [%s]." +
            "Init returned false.\n", CLASS, AudioContext.CLASS);
    static {
        ERR_STR_FAILED_INIT_FIRST_HALF = GameEngine.class.getSimpleName() +
            " failed the first half of initialization because an exception" +
            " was encountered.\n";
        ERR_STR_FAILED_INIT_SECOND_HALF = GameEngine.class.getSimpleName() +
            " failed the second half of initialization because an exception" +
            " was encountered.\n";
        ERR_STR_FAILED_INIT_GCPRIEST = GCPriest.class.getSimpleName() +
            " failed to initialize.\n";
    }
}
