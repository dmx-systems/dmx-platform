package de.deepamehta.core;

import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;

import java.util.List;



public interface CompositeValue {



    // === Accessors ===

    /**
     * Accesses a single-valued child.
     * Throws if there is no such child.
     */
    Topic getTopic(String childTypeUri);

    /**
     * Accesses a multiple-valued child.
     * Throws if there is no such child.
     */
    List<Topic> getTopics(String childTypeUri);

    // ---

    Object get(String childTypeUri);

    boolean has(String childTypeUri);

    Iterable<String> childTypeUris();

    int size();

    // ---

    CompositeValueModel getModel();



    // === Convenience Accessors ===

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    String getString(String childTypeUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    int getInt(String childTypeUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    long getLong(String childTypeUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    double getDouble(String childTypeUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    boolean getBoolean(String childTypeUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    Object getObject(String childTypeUri);

    // ---

    /**
     * Convenience accessor for the *composite* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    CompositeValue getCompositeValue(String childTypeUri);

    // Note: there are no convenience accessors for a multiple-valued child.



    // === Manipulators ===

    /**
     * Sets a child.
     * Works for both, single-valued child and multiple-valued child (cardinality "many").
     */
    CompositeValue set(String childTypeUri, TopicModel value);

    /**
     * Convenience method to set the simple value of a child.
     * Works for both, single-valued child and multiple-valued child (cardinality "many").
     *
     * @param   value   The simple value.
     *                  Either String, Integer, Long, Double, or Boolean. Primitive values are auto-boxed.
     */
    CompositeValue set(String childTypeUri, Object value);

    /**
     * Convenience method to set the composite value of a child.
     * Works for both, single-valued child and multiple-valued child (cardinality "many").
     */
    CompositeValue set(String childTypeUri, CompositeValueModel value);

    // ---

    CompositeValue setRef(String childTypeUri, long refTopicId);

    CompositeValue setRef(String childTypeUri, String refTopicUri);

    // ---

    CompositeValue remove(String childTypeUri, long topicId);
}
