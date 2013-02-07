package de.deepamehta.core;

import de.deepamehta.core.model.CompositeValueModel;



public interface CompositeValue {

    Topic getTopic(String childTypeUri);

    Topic getTopic(String childTypeUri, Topic defaultTopic);

    // ---

    boolean has(String childTypeUri);

    // --- Convenience methods ---

    String getString(String childTypeUri);

    // ---

    CompositeValueModel getModel();
}
