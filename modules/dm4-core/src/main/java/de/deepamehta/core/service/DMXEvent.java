package de.deepamehta.core.service;

import java.util.HashMap;
import java.util.Map;



/**
 * Base class for all events.
 * That is core events as well as plugin events.
 */
public abstract class DMXEvent {

    // ------------------------------------------------------------------------------------------------- Class Variables

    /**
     * A map of all known events (contains core events as well as plugin events).
     * Used to find the event that corresponds to a certain listener interface.
     */
    private static Map<String, DMXEvent> events = new HashMap();

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * The corresponding listener interface for this event.
     */
    private final Class listenerInterface;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public DMXEvent(Class listenerInterface) {
        this.listenerInterface = listenerInterface;
        putEvent(this, listenerInterface);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    /**
     * Delivers this event to the given listener.
     */
    public abstract void dispatch(EventListener listener, Object... params);

    /**
     * Returns the corresponding listener interface for this event.
     */
    public Class getListenerInterface() {
        return listenerInterface;
    }

    /**
     * Returns the event that corresponds to the given listener interface.
     */
    public static DMXEvent getEvent(Class listenerInterface) {
        DMXEvent event = events.get(listenerInterface.getName());
        //
        if (event == null) {
            throw new RuntimeException("The event object for listener " + listenerInterface + " is unknown");
        }
        //
        return event;
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void putEvent(DMXEvent event, Class listenerInterface) {
        events.put(listenerInterface.getName(), event);
    }
}
