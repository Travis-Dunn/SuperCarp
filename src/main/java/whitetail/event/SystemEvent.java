package whitetail.event;

public class SystemEvent implements Event {
    private EventType type;
    private long timestamp;
    private boolean immediate;
    private boolean networkable;

    public SystemEvent(EventType type) {
        this(type, true, false);
    }

    public SystemEvent(EventType type, boolean immediate, boolean networkable) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.immediate = immediate;
        this.networkable = networkable;
    }

    public EventType    getType()       { return type; }
    public long         getTimestamp()  { return timestamp; }
    public boolean      isImmediate()   { return immediate; }
    public boolean      isNetworkable() { return networkable; }

    public String toString() {
        return "SystemEvent{type=" + type + ", timestamp=" + timestamp + "}";
    }
}
