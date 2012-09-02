package de.deepamehta.core.impl.service;

import de.deepamehta.core.service.Listener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



class ListenerRegistry {

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

    List<Object> fireEvent(CoreEvent event, Object... params) {
        try {
            List results = new ArrayList();
            List<Listener> listeners = getListeners(event);
            if (listeners == null) {
                return results;
            }
            // ### FIXME: ConcurrentModificationException might occur
            for (Listener listener : listeners) {
                Object result = deliverEvent(listener, event, params);
                if (result != null) {
                    results.add(result);
                }
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Firing event " + event + " failed (params=" + params + ")", e);
        }
    }

    Object deliverEvent(Listener listener, CoreEvent event, Object... params) {
        try {
            Method listenerMethod = listener.getClass().getMethod(event.handlerMethodName, event.paramClasses);
            return listenerMethod.invoke(listener, params);
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
