package systems.dmx.core;

import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.TopicModel;

import java.util.List;



public interface ChildTopics extends Iterable<String> {



    // === Accessors ===

    /**
     * Accesses a single-valued child.
     * Throws if there is no such child.
     */
    RelatedTopic getTopic(String compDefUri);

    RelatedTopic getTopicOrNull(String compDefUri);

    /**
     * Accesses a multiple-valued child.
     * Throws if there is no such child. ### TODO: explain why not return an empty list instead
     */
    List<RelatedTopic> getTopics(String compDefUri);

    List<RelatedTopic> getTopicsOrNull(String compDefUri); // ### TODO: explain why not return an empty list instead

    // ---

    Object get(String compDefUri);

    /**
     * Checks if a child is contained in this ChildTopics.
     */
    boolean has(String compDefUri);

    /**
     * Returns the number of children contained in this ChildTopics.
     * Multiple-valued children count as one.
     */
    int size();

    // ---

    ChildTopicsModel getModel();



    // === Convenience Accessors ===

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    String getString(String compDefUri);

    String getString(String compDefUri, String defaultValue);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    int getInt(String compDefUri);

    int getInt(String compDefUri, int defaultValue);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    long getLong(String compDefUri);

    long getLong(String compDefUri, long defaultValue);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    double getDouble(String compDefUri);

    double getDouble(String compDefUri, double defaultValue);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    boolean getBoolean(String compDefUri);

    boolean getBoolean(String compDefUri, boolean defaultValue);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     *
     * @return  String, Integer, Long, Double, or Boolean. Never null.
     */
    Object getValue(String compDefUri);

    /**
     * @return  String, Integer, Long, Double, or Boolean. Never null.
     */
    Object getValue(String compDefUri, Object defaultValue);

    // ---

    /**
     * Convenience accessor for the *composite* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    ChildTopics getChildTopics(String compDefUri);

    // Note: there are no convenience accessors for a multiple-valued child.



    // === Manipulators ===

    // --- Single-valued Children ---

    ChildTopics setDeletionRef(String compDefUri, long refTopicId);

    ChildTopics setDeletionRef(String compDefUri, String refTopicUri);

    // --- Multiple-valued Children ---

    // TODO: add add(String compDefUri, RelatedTopicModel value)? Compare to ChildTopicsModel

    ChildTopics add(String compDefUri, TopicModel value);

    ChildTopics add(String compDefUri, Object value);

    ChildTopics add(String compDefUri, ChildTopicsModel value);

    // ---

    ChildTopics addRef(String compDefUri, long refTopicId);

    ChildTopics addRef(String compDefUri, long refTopicId, ChildTopicsModel relatingAssocChildTopics);

    ChildTopics addRef(String compDefUri, String refTopicUri);

    ChildTopics addRef(String compDefUri, String refTopicUri, ChildTopicsModel relatingAssocChildTopics);

    // ---

    ChildTopics addDeletionRef(String compDefUri, long refTopicId);

    ChildTopics addDeletionRef(String compDefUri, String refTopicUri);
}
