package de.deepamehta.core.impl.service;

import de.deepamehta.core.service.CoreEvent;
import de.deepamehta.core.service.Listener;
import de.deepamehta.core.service.Plugin;

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
        List<Listener> listeners = get(event);
        if (listeners == null) {
            listeners = new ArrayList();
            put(event, listeners);
        }
        listeners.add(listener);
    }

    List<Object> fireEvent(CoreEvent event, Object... params) {
        List results = new ArrayList();
        List<Listener> listeners = get(event);
        if (listeners == null) {
            return results;
        }
        for (Listener listener : listeners) {
            Object result = handleEvent(listener, event, params);
            if (result != null) {
                results.add(result);
            }
        }
        return results;
    }

    Object handleEvent(Listener listener, CoreEvent event, Object... params) {
        try {
            Method listenerMethod = listener.getClass().getMethod(event.handlerMethodName, event.paramClasses);
            return listenerMethod.invoke(listener, params);
        } catch (Exception e) {     // NoSuchMethodException, IllegalAccessException, InvocationTargetException
            throw new RuntimeException("Handling event " + event + " by " + listener + " failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private List<Listener> get(CoreEvent event) {
        return listenerRegistry.get(event.name());
    }

    private void put(CoreEvent event, List<Listener> listeners) {
        listenerRegistry.put(event.name(), listeners);
    }
}
