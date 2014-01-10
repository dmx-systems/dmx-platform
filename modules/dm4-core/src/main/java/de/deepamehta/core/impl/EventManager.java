package de.deepamehta.core.impl;

import de.deepamehta.core.osgi.PluginContext;
import de.deepamehta.core.service.DeepaMehtaEvent;
import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.service.Listener;

import javax.ws.rs.WebApplicationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



class EventManager {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * The registered listeners (key: event class name, value: listeners).
     */
    private Map<String, List<Listener>> listenerRegistry = new HashMap();

    private DeepaMehtaService dms;

    // ---------------------------------------------------------------------------------------------------- Constructors

    EventManager(DeepaMehtaService dms) {
        this.dms = dms;
        // Note: actually the class CoreEvent does not need to be instantiated as it contains only statics.
        // But if not instantiated OSGi apparently does not load the class at all.
        new CoreEvent();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void addListener(DeepaMehtaEvent event, Listener listener) {
        List<Listener> listeners = getListeners(event);
        if (listeners == null) {
            listeners = new ArrayList();
            putListeners(event, listeners);
        }
        listeners.add(listener);
    }

    void removeListener(DeepaMehtaEvent event, Listener listener) {
        List<Listener> listeners = getListeners(event);
        if (!listeners.remove(listener)) {
            throw new RuntimeException("Removing " + listener + " from " +
                event + " listeners failed: not found in " + listeners);
        }
    }

    // ---

    void fireEvent(DeepaMehtaEvent event, Object... params) {
        List<Listener> listeners = getListeners(event);
        if (listeners != null) {
            for (Listener listener : listeners) {
                deliverEvent(listener, event, params);
            }
        }
    }

    // ---

    /**
     * Checks weather the given plugin is a listener for the given event, and if so, delivers the event to the plugin.
     * Otherwise nothing is performed.
     */
    void deliverEvent(PluginImpl plugin, DeepaMehtaEvent event, Object... params) {
        PluginContext pluginContext = plugin.getContext();
        if (!isListener(pluginContext, event)) {
            return;
        }
        //
        deliverEvent((Listener) pluginContext, event, params);
    }

    /**
     * Convenience method to check weather the specified plugin is a listener for the given event, and if so,
     * delivers the event to the plugin. Otherwise nothing is performed.
     */
    void deliverEvent(String pluginUri, DeepaMehtaEvent event, Object... params) {
        deliverEvent((PluginImpl) dms.getPlugin(pluginUri), event, params);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void deliverEvent(Listener listener, DeepaMehtaEvent event, Object... params) {
        try {
            event.deliver(listener, params);
        } catch (WebApplicationException e) {
            // Note: a WebApplicationException thrown by a event listener must reach Jersey. So we re-throw here.
            // This allow plugins to produce specific HTTP responses by throwing a WebApplicationException.
            // Consider the Caching plugin: it produces a possible 304 (Not Modified) response this way.
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Processing event " + event + " by " + listener + " failed", e);
        }
    }

    // ---

    /**
     * Returns true if the given plugin is a listener for the given event.
     */
    private boolean isListener(PluginContext pluginContext, DeepaMehtaEvent event) {
        return event.getListenerInterface().isAssignableFrom(pluginContext.getClass());
    }

    // ---

    private List<Listener> getListeners(DeepaMehtaEvent event) {
        return listenerRegistry.get(event.getClass().getName());
    }

    private void putListeners(DeepaMehtaEvent event, List<Listener> listeners) {
        listenerRegistry.put(event.getClass().getName(), listeners);
    }
}
