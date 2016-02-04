package de.deepamehta.core.model;

import de.deepamehta.core.JSONEnabled;

import java.util.List;



/**
 * A recursive composite of key/value pairs. ### FIXDOC
 * <p>
 * Keys are strings, values are non-null atomic (string, int, long, double, boolean)
 * or again a <code>ChildTopicsModel</code>. ### FIXDOC
 */
public interface ChildTopicsModel extends JSONEnabled, Iterable<String> {



    // === Accessors ===

    /**
     * Accesses a single-valued child.
     * Throws if there is no such child.
     */
    RelatedTopicModel getTopic(String assocDefUri);

    /**
     * Accesses a single-valued child.
     * Returns a default value if there is no such child. ### TODO: make it getTopicOrNull(), catch ClassCastException
     */
    RelatedTopicModel getTopic(String assocDefUri, RelatedTopicModel defaultValue);

    // ---

    /**
     * Accesses a multiple-valued child.
     * Throws if there is no such child.
     */
    List<RelatedTopicModel> getTopics(String assocDefUri);

    /**
     * Accesses a multiple-valued child.
     * Returns a default value if there is no such child. ### TODO: make it getTopicsOrNull()
     */
    List<RelatedTopicModel> getTopics(String assocDefUri, List<RelatedTopicModel> defaultValue);

    // ---

    /**
     * Accesses a child generically, regardless of single-valued or multiple-valued.
     * Returns null if there is no such child.
     *
     * @return  A RelatedTopicModel or List<RelatedTopicModel>, or null if there is no such child.
     */
    Object get(String assocDefUri);

    /**
     * Checks if a child is contained in this ChildTopicsModel.
     */
    boolean has(String assocDefUri);

    /**
     * Returns the number of childs contained in this ChildTopicsModel.
     * Multiple-valued childs count as one.
     */
    int size();



    // === Convenience Accessors ===

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    String getString(String assocDefUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    String getString(String assocDefUri, String defaultValue);

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    int getInt(String assocDefUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    int getInt(String assocDefUri, int defaultValue);

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    long getLong(String assocDefUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    long getLong(String assocDefUri, long defaultValue);

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    double getDouble(String assocDefUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    double getDouble(String assocDefUri, double defaultValue);

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    boolean getBoolean(String assocDefUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    boolean getBoolean(String assocDefUri, boolean defaultValue);

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    Object getObject(String assocDefUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    Object getObject(String assocDefUri, Object defaultValue);

    // ---

    /**
     * Convenience accessor for the *composite* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    ChildTopicsModel getChildTopicsModel(String assocDefUri);

    /**
     * Convenience accessor for the *composite* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    ChildTopicsModel getChildTopicsModel(String assocDefUri, ChildTopicsModel defaultValue);

    // Note: there are no convenience accessors for a multiple-valued child.



    // === Manipulators ===

    // --- Single-valued Childs ---

    /**
     * Puts a value in a single-valued child.
     * An existing value is overwritten.
     */
    ChildTopicsModel put(String assocDefUri, RelatedTopicModel value);

    ChildTopicsModel put(String assocDefUri, TopicModel value);

    // ---

    /**
     * Convenience method to put a *simple* value in a single-valued child.
     * An existing value is overwritten.
     *
     * @param   value   a String, Integer, Long, Double, or a Boolean.
     *
     * @return  this ChildTopicsModel.
     */
    ChildTopicsModel put(String assocDefUri, Object value);

    /**
     * Convenience method to put a *composite* value in a single-valued child.
     * An existing value is overwritten.
     *
     * @return  this ChildTopicsModel.
     */
    ChildTopicsModel put(String assocDefUri, ChildTopicsModel value);

    // ---

    /**
     * Puts a by-ID topic reference in a single-valued child.
     * An existing reference is overwritten.
     */
    ChildTopicsModel putRef(String assocDefUri, long refTopicId);

    /**
     * Puts a by-URI topic reference in a single-valued child.
     * An existing reference is overwritten.
     */
    ChildTopicsModel putRef(String assocDefUri, String refTopicUri);

    // ---

    /**
     * Puts a by-ID topic deletion reference to a single-valued child.
     * An existing value is overwritten.
     */
    ChildTopicsModel putDeletionRef(String assocDefUri, long refTopicId);

    /**
     * Puts a by-URI topic deletion reference to a single-valued child.
     * An existing value is overwritten.
     */
    ChildTopicsModel putDeletionRef(String assocDefUri, String refTopicUri);

    // ---

    /**
     * Removes a single-valued child.
     */
    ChildTopicsModel remove(String assocDefUri);

    // --- Multiple-valued Childs ---

    /**
     * Adds a value to a multiple-valued child.
     */
    ChildTopicsModel add(String assocDefUri, RelatedTopicModel value);

    ChildTopicsModel add(String assocDefUri, TopicModel value);

    /**
     * Sets the values of a multiple-valued child.
     * Existing values are overwritten.
     */
    ChildTopicsModel put(String assocDefUri, List<RelatedTopicModel> values);

    /**
     * Removes a value from a multiple-valued child.
     */
    ChildTopicsModel remove(String assocDefUri, TopicModel value);

    // ---

    /**
     * Adds a by-ID topic reference to a multiple-valued child.
     */
    ChildTopicsModel addRef(String assocDefUri, long refTopicId);

    /**
     * Adds a by-URI topic reference to a multiple-valued child.
     */
    ChildTopicsModel addRef(String assocDefUri, String refTopicUri);

    // ---

    /**
     * Adds a by-ID topic deletion reference to a multiple-valued child.
     */
    ChildTopicsModel addDeletionRef(String assocDefUri, long refTopicId);

    /**
     * Adds a by-URI topic deletion reference to a multiple-valued child.
     */
    ChildTopicsModel addDeletionRef(String assocDefUri, String refTopicUri);



    // ===

    String childTypeUri(String assocDefUri);    // ###

    // ---

    ChildTopicsModel clone();
}
