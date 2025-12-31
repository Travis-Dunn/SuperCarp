package whitetail.event;

public class MouseEvent implements Event {
    public final EventType type;
    public final int button, x, y;

    public MouseEvent(EventType type, int x, int y, int button) {
        this.type = type;
        this.button = button;
        this.x = x;
        this.y = y;
    }

    @Override
    public EventType getType() {
        return type;
    }

    @Override
    public long getTimestamp() {
        return 0;
    }

    @Override
    public boolean isImmediate() {
        return true;
    }

    @Override
    public boolean isNetworkable() {
        return false;
    }
}
