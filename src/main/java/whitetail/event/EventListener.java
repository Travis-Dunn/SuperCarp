package whitetail.event;

public interface EventListener {
    boolean     handleEvent             (Event event);
    EventType[] getInterestedEventTypes ();
}
