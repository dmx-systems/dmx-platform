package de.deepamehta.core;

import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;

import java.util.List;



public interface CompositeValue {

    /**
     * Accesses a single-valued child.
     * Throws if there is no such child.
     */
    Topic getTopic(String childTypeUri);

    /**
     * Accesses a single-valued child.
     * Returns a default value if there is no such child.
     */
    Topic getTopic(String childTypeUri, Topic defaultTopic);

    /**
     * Accesses a multiple-valued child.
     * Throws if there is no such child.
     */
    List<Topic> getTopics(String childTypeUri);

    /**
     * Accesses a multiple-valued child.
     * Returns a default value if there is no such child.
     */
    List<Topic> getTopics(String childTypeUri, List<Topic> defaultValue);



    // === Convenience methods ===

    /**
     * Convenience method for accessing the *simple* value of a single-valued child.
     */
    String getString(String childTypeUri);

    /**
     * Convenience method for accessing the *simple* value of a single-valued child.
     */
    int getInt(String childTypeUri);

    /**
     * Convenience method for accessing the *simple* value of a single-valued child.
     */
    long getLong(String childTypeUri);

    /**
     * Convenience method for accessing the *simple* value of a single-valued child.
     */
    double getDouble(String childTypeUri);

    /**
     * Convenience method for accessing the *simple* value of a single-valued child.
     */
    boolean getBoolean(String childTypeUri);

    /**
     * Convenience method for accessing the *simple* value of a single-valued child.
     */
    Object getObject(String childTypeUri);

    // ---

    /**
     * Convenience method for accessing the *composite* value of a single-valued child.
     */
    CompositeValue getComposite(String childTypeUri);

    // Note: there are no convenience accessors for a multiple-valued child.



    // ===

    Object get(String childTypeUri);

    boolean has(String childTypeUri);

    Iterable<String> childTypeUris();

    int size();



    // === Manipulators ===

    CompositeValue set(String childTypeUri, TopicModel value,          ClientState clientState, Directives directives);

    CompositeValue set(String childTypeUri, Object value,              ClientState clientState, Directives directives);

    CompositeValue set(String childTypeUri, CompositeValueModel value, ClientState clientState, Directives directives);

    // ---

    CompositeValue setRef(String childTypeUri, long refTopicId,        ClientState clientState, Directives directives);

    CompositeValue setRef(String childTypeUri, String refTopicUri,     ClientState clientState, Directives directives);



    // ===

    CompositeValueModel getModel();
}
