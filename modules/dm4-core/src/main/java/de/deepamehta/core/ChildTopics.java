package de.deepamehta.core;

import de.deepamehta.core.model.ChildTopicsModel;



public interface ChildTopics {

    Topic getTopic(String childTypeUri);

    Topic getTopic(String childTypeUri, Topic defaultTopic);

    // ---

    boolean has(String childTypeUri);

    // --- Convenience methods ---

    String getString(String childTypeUri);

    // ---

    ChildTopicsModel getModel();
}
