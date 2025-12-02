package whitetail.event;

public interface Event {
    EventType       getType();
    long            getTimestamp();
    boolean         isImmediate();
    boolean         isNetworkable();
}
