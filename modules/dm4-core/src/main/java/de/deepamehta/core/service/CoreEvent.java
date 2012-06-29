package de.deepamehta.core.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.TopicModel;

import java.util.HashMap;
import java.util.Map;



public enum CoreEvent {

    POST_CREATE_TOPIC("postCreateTopic", Topic.class, ClientState.class, Directives.class);
    // ### TODO: transform the other hooks into events

    private final String listenerMethodName;
    private final Class[] paramClasses;

    private static Map<String, CoreEvent> events;

    // ---------------------------------------------------------------------------------------------------- Constructors

    private CoreEvent(String listenerMethodName, Class... paramClasses) {
        this.listenerMethodName = listenerMethodName;
        this.paramClasses = paramClasses;
        // events.put(listenerMethodName, this);
        // ### Doesn't compile: "illegal reference to static field from initializer".
        // ### Enum constants are initialzed before other static fields.
        // ### Lazy initialization outside the constructor solves it.
        put(listenerMethodName, this);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String getListenerMethodName() {
        return listenerMethodName;
    }

    public Class[] getParamClasses() {
        return paramClasses;
    }

    public static CoreEvent fromListenerInterface(Class listenerInterface) {
        String name = listenerInterface.getSimpleName();
        // error check
        if (!name.endsWith("Listener")) {
            throw new RuntimeException("\"" + name + "\" is an invalid listener interface name. " +
                "The name is expected to end with \"Listerner\"");
        }
        //
        name = name.substring(0, 1).toLowerCase() + name.substring(1, name.length() - "Listener".length());
        return events.get(name);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void put(String listenerMethodName, CoreEvent event) {
        // ### must initialize lazily, see above
        if (events == null) {
            events = new HashMap();
        }
        //
        events.put(listenerMethodName, event);
    }
}
