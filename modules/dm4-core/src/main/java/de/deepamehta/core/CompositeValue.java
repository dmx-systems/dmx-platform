package de.deepamehta.core;

import de.deepamehta.core.model.CompositeValueModel;
import java.util.List;



public interface CompositeValue {

    Topic getTopic(String childTypeUri);

    Topic getTopic(String childTypeUri, Topic defaultTopic);

    List<Topic> getTopics(String childTypeUri);

    List<Topic> getTopics(String childTypeUri, List<Topic> defaultValue);

    // ---

    boolean has(String childTypeUri);

    // --- Convenience methods ---

    String getString(String childTypeUri);

    // ---

    CompositeValueModel getModel();
}
