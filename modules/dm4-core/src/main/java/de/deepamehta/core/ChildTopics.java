package de.deepamehta.core;

import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.TopicModel;

import java.util.List;



public interface ChildTopics extends Iterable<String> {



    // === Accessors ===

    /**
     * Accesses a single-valued child.
     * Throws if there is no such child.
     */
    RelatedTopic getTopic(String assocDefUri);

    RelatedTopic getTopicOrNull(String assocDefUri);

    /**
     * Accesses a multiple-valued child.
     * Throws if there is no such child.
     */
    List<RelatedTopic> getTopics(String assocDefUri);

    List<RelatedTopic> getTopicsOrNull(String assocDefUri);

    // ---

    Object get(String assocDefUri);

    boolean has(String assocDefUri);

    int size();

    // ---

    ChildTopicsModel getModel();



    // === Convenience Accessors ===

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    String getString(String assocDefUri);

    String getStringOrNull(String assocDefUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    int getInt(String assocDefUri);

    Integer getIntOrNull(String assocDefUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    long getLong(String assocDefUri);

    Long getLongOrNull(String assocDefUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    double getDouble(String assocDefUri);

    Double getDoubleOrNull(String assocDefUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    boolean getBoolean(String assocDefUri);

    Boolean getBooleanOrNull(String assocDefUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    Object getObject(String assocDefUri);

    Object getObjectOrNull(String assocDefUri);

    // ---

    /**
     * Convenience accessor for the *composite* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    ChildTopics getChildTopics(String assocDefUri);

    // Note: there are no convenience accessors for a multiple-valued child.



    // === Manipulators ===

    // --- Single-valued Childs ---

    /**
     * Sets a child.
     */
    ChildTopics set(String assocDefUri, TopicModel value);

    // ---

    /**
     * Convenience method to set the simple value of a child.
     *
     * @param   value   The simple value.
     *                  Either String, Integer, Long, Double, or Boolean. Primitive values are auto-boxed.
     */
    ChildTopics set(String assocDefUri, Object value);

    /**
     * Convenience method to set the composite value of a child.
     */
    ChildTopics set(String assocDefUri, ChildTopicsModel value);

    // ---

    ChildTopics setRef(String assocDefUri, long refTopicId);

    ChildTopics setRef(String assocDefUri, long refTopicId, ChildTopicsModel relatingAssocChildTopics);

    ChildTopics setRef(String assocDefUri, String refTopicUri);

    ChildTopics setRef(String assocDefUri, String refTopicUri, ChildTopicsModel relatingAssocChildTopics);

    // ---

    ChildTopics setDeletionRef(String assocDefUri, long refTopicId);

    ChildTopics setDeletionRef(String assocDefUri, String refTopicUri);

    // --- Multiple-valued Childs ---

    ChildTopics add(String assocDefUri, TopicModel value);

    // ---

    ChildTopics add(String assocDefUri, Object value);

    ChildTopics add(String assocDefUri, ChildTopicsModel value);

    // ---

    ChildTopics addRef(String assocDefUri, long refTopicId);

    ChildTopics addRef(String assocDefUri, long refTopicId, ChildTopicsModel relatingAssocChildTopics);

    ChildTopics addRef(String assocDefUri, String refTopicUri);

    ChildTopics addRef(String assocDefUri, String refTopicUri, ChildTopicsModel relatingAssocChildTopics);

    // ---

    ChildTopics addDeletionRef(String assocDefUri, long refTopicId);

    ChildTopics addDeletionRef(String assocDefUri, String refTopicUri);
}
