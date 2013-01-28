package de.deepamehta.core.impl;

import de.deepamehta.core.service.Listener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



class EventManager {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * The registered listeners, hashed by event name (name of CoreEvent enum constant, e.g. "POST_CREATE_TOPIC").
     */
    private Map<String, List<Listener>> listenerRegistry = new HashMap();

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void addListener(CoreEvent event, Listener listener) {
        List<Listener> listeners = getListeners(event);
        if (listeners == null) {
            listeners = new ArrayList();
            putListeners(event, listeners);
        }
        listeners.add(listener);
    }

    void removeListener(CoreEvent event, Listener listener) {
        List<Listener> listeners = getListeners(event);
        if (!listeners.remove(listener)) {
            throw new RuntimeException("Removing " + listener + " from " +
                event + " listeners failed: not found in " + listeners);
        }
    }

    // ---

    void fireEvent(CoreEvent event, Object... params) {
        try {
            List<Listener> listeners = getListeners(event);
            if (listeners == null) {
                return;
            }
            // ### FIXME: ConcurrentModificationException might occur
            for (Listener listener : listeners) {
                deliverEvent(listener, event, params);
            }
        } catch (Exception e) {
            throw new RuntimeException("Firing event " + event + " failed (params=" + params + ")", e);
        }
    }

    void deliverEvent(Listener listener, CoreEvent event, Object... params) {
        try {
            event.deliver(listener, params);
        } catch (Exception e) {     // NoSuchMethodException, IllegalAccessException, InvocationTargetException
            throw new RuntimeException("Delivering event " + event + " to " + listener + " failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private List<Listener> getListeners(CoreEvent event) {
        return listenerRegistry.get(event.name());
    }

    private void putListeners(CoreEvent event, List<Listener> listeners) {
        listenerRegistry.put(event.name(), listeners);
    }
}
