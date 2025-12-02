package production;

import whitetail.core.GameEngine;
import whitetail.event.Event;
import whitetail.event.EventListener;
import whitetail.event.EventType;

public class SuperCarpEngine extends GameEngine implements EventListener {

    public SuperCarpEngine() { super(); }

    @Override
    protected void onProcessInput() {

    }

    @Override
    protected boolean onInit() {
        return true;
    }

    @Override
    protected void onUpdate(double delta) {

    }

    @Override
    protected void onRender() {
        window.swapBuffers();
    }

    @Override
    protected void onShutdown() {

    }

    @Override
    public boolean handleEvent(Event event) {
        return false;
    }

    @Override
    public EventType[] getInterestedEventTypes() {
        return new EventType[0];
    }
}
