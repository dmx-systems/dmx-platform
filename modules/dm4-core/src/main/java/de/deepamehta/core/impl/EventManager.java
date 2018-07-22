package de.deepamehta.core.impl;

import de.deepamehta.core.osgi.PluginContext;
import de.deepamehta.core.service.DMXEvent;
import de.deepamehta.core.service.EventListener;
import de.deepamehta.core.service.accesscontrol.AccessControlException;

import javax.ws.rs.WebApplicationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



class EventManager {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * The registered event listeners (key: event class name, value: event listeners).
     */
    private Map<String, List<EventListener>> listenerRegistry = new HashMap();

    // ---------------------------------------------------------------------------------------------------- Constructors

    EventManager() {
        // Note: actually the class CoreEvent does not need to be instantiated as it contains only statics.
        // But if not instantiated OSGi apparently does not load the class at all.
        new CoreEvent();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void addListener(DMXEvent event, EventListener listener) {
        List<EventListener> listeners = getListeners(event);
        if (listeners == null) {
            listeners = new ArrayList();
            putListeners(event, listeners);
        }
        listeners.add(listener);
    }

    void removeListener(DMXEvent event, EventListener listener) {
        List<EventListener> listeners = getListeners(event);
        if (!listeners.remove(listener)) {
            throw new RuntimeException("Removing " + listener + " from " +
                event + " event listeners failed: not found in " + listeners);
        }
    }

    // ---

    void fireEvent(DMXEvent event, Object... params) {
        List<EventListener> listeners = getListeners(event);
        if (listeners != null) {
            for (EventListener listener : listeners) {
                dispatchEvent(listener, event, params);
            }
        }
    }

    // ---

    /**
     * Delivers an event to a particular plugin.
     * If the plugin is not a listener for that event nothing is performed.
     */
    void dispatchEvent(PluginImpl plugin, DMXEvent event, Object... params) {
        PluginContext pluginContext = plugin.getContext();
        if (!isListener(pluginContext, event)) {
            return;
        }
        //
        dispatchEvent((EventListener) pluginContext, event, params);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void dispatchEvent(EventListener listener, DMXEvent event, Object... params) {
        try {
            event.dispatch(listener, params);
        } catch (WebApplicationException e) {
            // Note: a WebApplicationException thrown by a event listener must reach Jersey. So we re-throw here.
            // This allow plugins to produce specific HTTP responses by throwing a WebApplicationException.
            // Consider the Caching plugin: it produces a possible 304 (Not Modified) response this way.
            throw e;
        } catch (AccessControlException e) {
            // Note: an AccessControlException thrown by a event listener must reach the caller in order to
            // recover.
            throw e;
        } catch (Throwable e) {
            // Note: here we also catch errors like NoSuchMethodError or AbstractMethodError.
            // These occur when plugins are not yet adapted to changed Core API.
            throw new RuntimeException("An error occurred in the " + event.getListenerInterface().getSimpleName() +
                " of " + listener, e);
        }
    }

    // ---

    /**
     * Returns true if the given plugin is a listener for the given event.
     */
    private boolean isListener(PluginContext pluginContext, DMXEvent event) {
        return event.getListenerInterface().isAssignableFrom(pluginContext.getClass());
    }

    // ---

    private List<EventListener> getListeners(DMXEvent event) {
        return listenerRegistry.get(event.getClass().getName());
    }

    private void putListeners(DMXEvent event, List<EventListener> listeners) {
        listenerRegistry.put(event.getClass().getName(), listeners);
    }
}
