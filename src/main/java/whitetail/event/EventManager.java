package whitetail.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EventManager {
    private Map<EventType, List<EventListener>>     listeners;
    private ConcurrentLinkedQueue<Event>            eventQueue;
    private List<EventListener>                     globalListeners;

    public EventManager() {
        this.listeners = new HashMap<EventType, List<EventListener>>();
        this.eventQueue = new ConcurrentLinkedQueue<Event>();
        this.globalListeners = new ArrayList<EventListener>();
    }

    public void addEventListener(EventListener listener) {
        EventType[] interestedTypes = listener.getInterestedEventTypes();
        if (interestedTypes == null || interestedTypes.length == 0) {
            globalListeners.add(listener);
        } else {
            for (EventType type : interestedTypes) {
                List<EventListener> typeListener = listeners.get(type);
                if (typeListener == null) {
                    typeListener = new ArrayList<EventListener>();
                    listeners.put(type, typeListener);
                }
                typeListener.add(listener);
            }
        }
    }

    public void removeEventListener(EventListener listener) {
        globalListeners.remove(listener);
        for (List<EventListener> typeListeners : listeners.values()) {
            typeListeners.remove(listener);
        }
    }

    public void fireEvent(Event event) {
        if (event.isImmediate()) {
            processEventImmediately(event);
        } else {
            eventQueue.offer(event);
        }
    }

    public void processEvents() {
        Event event;
        while ((event = eventQueue.poll()) != null) {
            processEventImmediately(event);
        }
    }

    private void processEventImmediately(Event event) {
        boolean consumed = false;
        List<EventListener> typeListeners = listeners.get(event.getType());
        if (typeListeners != null) {
            for (EventListener listener : typeListeners) {
                if (listener.handleEvent(event)) {
                    consumed = true;
                    break;
                }
            }
        }
        if (!consumed) {
            for (EventListener listener : globalListeners) {
                if (listener.handleEvent(event)) break;
            }
        }
    }

    public void clearEvents() {
        eventQueue.clear();
    }

    public int getQueuedEventCount() {
        return eventQueue.size();
    }

    public void shutdown() {
        clearEvents();
        listeners.clear();
        globalListeners.clear();
    }
}
