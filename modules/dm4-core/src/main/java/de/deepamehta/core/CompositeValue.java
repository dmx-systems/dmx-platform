package de.deepamehta.core;

import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;

import java.util.List;



public interface CompositeValue {

    Topic getTopic(String childTypeUri);

    Topic getTopic(String childTypeUri, Topic defaultTopic);

    List<Topic> getTopics(String childTypeUri);

    List<Topic> getTopics(String childTypeUri, List<Topic> defaultValue);

    // ---

    void set(String childTypeUri, SimpleValue value, ClientState clientState, Directives directives);

    // ---

    boolean has(String childTypeUri);

    // --- Convenience methods ---

    String getString(String childTypeUri);

    // ---

    CompositeValueModel getModel();
}
