package production.scenes;

import org.lwjgl.input.Keyboard;
import production.Data;
import whitetail.event.Event;
import whitetail.event.EventType;
import whitetail.event.KeyboardEvent;
import whitetail.event.MouseEvent;
import whitetail.scene.Scene;

public class SceneGame extends Scene {
    @Override
    public void onInit() {

    }

    @Override
    public void onEnter() {

    }

    @Override
    public void onExit() {

    }

    @Override
    public void onUpdate(double delta) {

    }

    @Override
    public void onRender() {

    }

    @Override
    public void onInput() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public boolean handleEvent(Event e) {
        MouseEvent mouseEvent;
        EventType eventType = e.getType();

        if (eventType == EventType.MOUSE_DOWN) {
            mouseEvent = (MouseEvent) e;

            if (mouseEvent.button != 0) return false;

            Data.cursor.handleMouseClick(mouseEvent.x, mouseEvent.y);
            return true;
        }

        return false;
    }
}
