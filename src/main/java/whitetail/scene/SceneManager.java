package whitetail.scene;

import java.util.HashMap;
import java.util.Map;

public final class SceneManager {
    private static Map<SceneType, Scene> scenes;
    private static Scene currentScene;
    private static Scene nextScene;
    private static boolean transitioning;
    private static boolean init;

    private SceneManager() {}

    public static boolean Init() {
        assert(!init);

        scenes = new HashMap<SceneType, Scene>();

        currentScene = null;
        nextScene = null;
        transitioning = false;
        return init = true;
    }

    public static void RegisterScene(SceneType t, Scene s) {
        assert(init);
        assert(s != null);
        assert(!scenes.containsKey(t));

        scenes.put(t, s);
    }

    public static void TransitionTo(SceneType t) {
        assert(init);
        Scene s = scenes.get(t);
        assert(s != null);

        nextScene = s;
        transitioning = true;
    }
    
    public static void Update(double delta) {
        assert(init);

        if (transitioning && nextScene != null) {
            if (currentScene != null)
                currentScene.onExit();

            currentScene = nextScene;
            nextScene = null;
            transitioning = false;

            if (!currentScene.init) {
                currentScene.onInit();
                currentScene.init = true;
            }
            currentScene.onEnter();
        }
        if (currentScene != null)
            currentScene.onUpdate(delta);
    }

    public static void Render() {
        assert(init);

        if(currentScene != null)
            currentScene.onRender();
    }

    public static void Shutdown() {
        assert(init);
        for (Scene s : scenes.values())
            if (s.init)
                s.onDestroy();
        scenes.clear();
        currentScene = null;
        init = false;
    }

    public static void OnInput() {
        assert(init);
        if (currentScene != null)
            currentScene.onInput();
    }
}
