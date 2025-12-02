package whitetail.event;

import whitetail.utility.FramerateManager;

public class KeyboardEvent implements Event {
    public final EventType type;
    public final long timestamp;
    public final int keyCode;
    public final char keyChar;
    public final boolean repeat;

    public KeyboardEvent(EventType type, int keyCode, char keyChar,
           boolean repeat) {
        this.type = type;
        this.keyCode = keyCode;
        this.keyChar = keyChar;
        this.repeat = repeat;
        this.timestamp = FramerateManager.CurrentTimeNanos();
    }

    @Override
    public EventType getType() { return type; }
    @Override
    public long getTimestamp() { return timestamp; }
    @Override
    public boolean isImmediate() { return true; }
    @Override
    public boolean isNetworkable() { return false; }
}
