package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicDeletionModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicReferenceModel;
import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



class ChildTopicsModelImpl implements ChildTopicsModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * Internal representation.
     * Key: assoc def URI (String), value: RelatedTopicModel or List<RelatedTopicModel>
     */
    private Map<String, Object> childTopics;
    // Note: it must be List<RelatedTopicModel>, not Set<RelatedTopicModel>.
    // There may be several TopicModels with the same ID. That occurrs if the webclient user adds several new topics
    // at once (by the means of an "Add" button). In this case the ID is -1. TopicModel equality is defined solely as
    // ID equality (see DeepaMehtaObjectModel.equals()).

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    ChildTopicsModelImpl(Map<String, Object> childTopics) {
        this.childTopics = childTopics;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Accessors ===

    /**
     * Accesses a single-valued child.
     * Throws if there is no such child.
     */
    @Override
    public RelatedTopicModel getTopic(String assocDefUri) {
        RelatedTopicModel topic = (RelatedTopicModel) get(assocDefUri);
        // error check
        if (topic == null) {
            throw new RuntimeException("Assoc Def URI \"" + assocDefUri + "\" not found in " + childTopics.keySet());
        }
        //
        return topic;
    }

    /**
     * Accesses a single-valued child.
     * Returns a default value if there is no such child. ### TODO: make it getTopicOrNull(), catch ClassCastException
     */
    @Override
    public RelatedTopicModel getTopic(String assocDefUri, RelatedTopicModel defaultValue) {
        RelatedTopicModel topic = (RelatedTopicModel) get(assocDefUri);
        return topic != null ? topic : defaultValue;
    }

    // ---

    /**
     * Accesses a multiple-valued child.
     * Throws if there is no such child.
     */
    @Override
    public List<RelatedTopicModel> getTopics(String assocDefUri) {
        try {
            List<RelatedTopicModel> topics = (List<RelatedTopicModel>) get(assocDefUri);
            // error check
            if (topics == null) {
                throw new RuntimeException("Assoc Def URI \"" + assocDefUri + "\" not found in " +
                    childTopics.keySet());
            }
            //
            return topics;
        } catch (ClassCastException e) {
            throwInvalidMultiAccess(assocDefUri, e);
            return null;    // never reached
        }
    }

    /**
     * Accesses a multiple-valued child.
     * Returns a default value if there is no such child. ### TODO: make it getTopicsOrNull()
     */
    @Override
    public List<RelatedTopicModel> getTopics(String assocDefUri, List<RelatedTopicModel> defaultValue) {
        try {
            List<RelatedTopicModel> topics = (List<RelatedTopicModel>) get(assocDefUri);
            return topics != null ? topics : defaultValue;
        } catch (ClassCastException e) {
            throwInvalidMultiAccess(assocDefUri, e);
            return null;    // never reached
        }
    }

    // ---

    /**
     * Accesses a child generically, regardless of single-valued or multiple-valued.
     * Returns null if there is no such child.
     *
     * @return  A RelatedTopicModel or List<RelatedTopicModel>, or null if there is no such child.
     */
    @Override
    public Object get(String assocDefUri) {
        return childTopics.get(assocDefUri);
    }

    /**
     * Checks if a child is contained in this ChildTopicsModel.
     */
    @Override
    public boolean has(String assocDefUri) {
        return childTopics.containsKey(assocDefUri);
    }

    /**
     * Returns the number of childs contained in this ChildTopicsModel.
     * Multiple-valued childs count as one.
     */
    @Override
    public int size() {
        return childTopics.size();
    }



    // === Convenience Accessors ===

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    @Override
    public String getString(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().toString();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    @Override
    public String getString(String assocDefUri, String defaultValue) {
        TopicModel topic = getTopic(assocDefUri, null);
        return topic != null ? topic.getSimpleValue().toString() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    @Override
    public int getInt(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().intValue();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    @Override
    public int getInt(String assocDefUri, int defaultValue) {
        TopicModel topic = getTopic(assocDefUri, null);
        return topic != null ? topic.getSimpleValue().intValue() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    @Override
    public long getLong(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().longValue();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    @Override
    public long getLong(String assocDefUri, long defaultValue) {
        TopicModel topic = getTopic(assocDefUri, null);
        return topic != null ? topic.getSimpleValue().longValue() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    @Override
    public double getDouble(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().doubleValue();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    @Override
    public double getDouble(String assocDefUri, double defaultValue) {
        TopicModel topic = getTopic(assocDefUri, null);
        return topic != null ? topic.getSimpleValue().doubleValue() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    @Override
    public boolean getBoolean(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().booleanValue();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    @Override
    public boolean getBoolean(String assocDefUri, boolean defaultValue) {
        TopicModel topic = getTopic(assocDefUri, null);
        return topic != null ? topic.getSimpleValue().booleanValue() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    @Override
    public Object getObject(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().value();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    @Override
    public Object getObject(String assocDefUri, Object defaultValue) {
        TopicModel topic = getTopic(assocDefUri, null);
        return topic != null ? topic.getSimpleValue().value() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *composite* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    @Override
    public ChildTopicsModel getChildTopicsModel(String assocDefUri) {
        return getTopic(assocDefUri).getChildTopicsModel();
    }

    /**
     * Convenience accessor for the *composite* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    @Override
    public ChildTopicsModel getChildTopicsModel(String assocDefUri, ChildTopicsModel defaultValue) {
        RelatedTopicModel topic = getTopic(assocDefUri, null);
        return topic != null ? topic.getChildTopicsModel() : defaultValue;
    }

    // Note: there are no convenience accessors for a multiple-valued child.



    // === Manipulators ===

    // --- Single-valued Childs ---

    /**
     * Puts a value in a single-valued child.
     * An existing value is overwritten.
     */
    @Override
    public ChildTopicsModel put(String assocDefUri, RelatedTopicModel value) {
        try {
            // check argument
            if (value == null) {
                throw new IllegalArgumentException("Tried to put null in a ChildTopicsModel");
            }
            //
            childTopics.put(assocDefUri, value);
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Putting a value in a ChildTopicsModel failed (assocDefUri=\"" +
                assocDefUri + "\", value=" + value + ")", e);
        }
    }

    @Override
    public ChildTopicsModel put(String assocDefUri, TopicModel value) {
        return put(assocDefUri, new RelatedTopicModel(value));
    }

    // ---

    /**
     * Convenience method to put a *simple* value in a single-valued child.
     * An existing value is overwritten.
     *
     * @param   value   a String, Integer, Long, Double, or a Boolean.
     *
     * @return  this ChildTopicsModel.
     */
    @Override
    public ChildTopicsModel put(String assocDefUri, Object value) {
        try {
            return put(assocDefUri, new TopicModel(childTypeUri(assocDefUri), new SimpleValue(value)));
        } catch (Exception e) {
            throw new RuntimeException("Putting a value in a ChildTopicsModel failed (assocDefUri=\"" +
                assocDefUri + "\", value=" + value + ")", e);
        }
    }

    /**
     * Convenience method to put a *composite* value in a single-valued child.
     * An existing value is overwritten.
     *
     * @return  this ChildTopicsModel.
     */
    @Override
    public ChildTopicsModel put(String assocDefUri, ChildTopicsModel value) {
        return put(assocDefUri, new TopicModel(childTypeUri(assocDefUri), value));
    }

    // ---

    /**
     * Puts a by-ID topic reference in a single-valued child.
     * An existing reference is overwritten.
     */
    @Override
    public ChildTopicsModel putRef(String assocDefUri, long refTopicId) {
        put(assocDefUri, new TopicReferenceModel(refTopicId));
        return this;
    }

    /**
     * Puts a by-URI topic reference in a single-valued child.
     * An existing reference is overwritten.
     */
    @Override
    public ChildTopicsModel putRef(String assocDefUri, String refTopicUri) {
        put(assocDefUri, new TopicReferenceModel(refTopicUri));
        return this;
    }

    // ---

    /**
     * Puts a by-ID topic deletion reference to a single-valued child.
     * An existing value is overwritten.
     */
    @Override
    public ChildTopicsModel putDeletionRef(String assocDefUri, long refTopicId) {
        put(assocDefUri, new TopicDeletionModel(refTopicId));
        return this;
    }

    /**
     * Puts a by-URI topic deletion reference to a single-valued child.
     * An existing value is overwritten.
     */
    @Override
    public ChildTopicsModel putDeletionRef(String assocDefUri, String refTopicUri) {
        put(assocDefUri, new TopicDeletionModel(refTopicUri));
        return this;
    }

    // ---

    /**
     * Removes a single-valued child.
     */
    @Override
    public ChildTopicsModel remove(String assocDefUri) {
        childTopics.remove(assocDefUri);    // ### TODO: throw if not in map?
        return this;
    }

    // --- Multiple-valued Childs ---

    /**
     * Adds a value to a multiple-valued child.
     */
    @Override
    public ChildTopicsModel add(String assocDefUri, RelatedTopicModel value) {
        List<RelatedTopicModel> topics = getTopics(assocDefUri, null);      // defaultValue=null
        // Note: topics just created have no child topics yet
        if (topics == null) {
            topics = new ArrayList();
            childTopics.put(assocDefUri, topics);
        }
        //
        topics.add(value);
        //
        return this;
    }

    @Override
    public ChildTopicsModel add(String assocDefUri, TopicModel value) {
        return add(assocDefUri, new RelatedTopicModel(value));
    }

    /**
     * Sets the values of a multiple-valued child.
     * Existing values are overwritten.
     */
    @Override
    public ChildTopicsModel put(String assocDefUri, List<RelatedTopicModel> values) {
        childTopics.put(assocDefUri, values);
        return this;
    }

    /**
     * Removes a value from a multiple-valued child.
     */
    @Override
    public ChildTopicsModel remove(String assocDefUri, TopicModel value) {
        List<RelatedTopicModel> topics = getTopics(assocDefUri, null);      // defaultValue=null
        if (topics != null) {
            topics.remove(value);
        }
        return this;
    }

    // ---

    /**
     * Adds a by-ID topic reference to a multiple-valued child.
     */
    @Override
    public ChildTopicsModel addRef(String assocDefUri, long refTopicId) {
        add(assocDefUri, new TopicReferenceModel(refTopicId));
        return this;
    }

    /**
     * Adds a by-URI topic reference to a multiple-valued child.
     */
    @Override
    public ChildTopicsModel addRef(String assocDefUri, String refTopicUri) {
        add(assocDefUri, new TopicReferenceModel(refTopicUri));
        return this;
    }

    // ---

    /**
     * Adds a by-ID topic deletion reference to a multiple-valued child.
     */
    @Override
    public ChildTopicsModel addDeletionRef(String assocDefUri, long refTopicId) {
        add(assocDefUri, new TopicDeletionModel(refTopicId));
        return this;
    }

    /**
     * Adds a by-URI topic deletion reference to a multiple-valued child.
     */
    @Override
    public ChildTopicsModel addDeletionRef(String assocDefUri, String refTopicUri) {
        add(assocDefUri, new TopicDeletionModel(refTopicUri));
        return this;
    }



    // === Iterable Implementation ===

    /**
     * Returns an interator which iterates this ChildTopicsModel's assoc def URIs.
     */
    @Override
    public Iterator<String> iterator() {
        return childTopics.keySet().iterator();
    }



    // ===

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject json = new JSONObject();
            for (String assocDefUri : this) {
                Object value = get(assocDefUri);
                if (value instanceof RelatedTopicModel) {
                    json.put(assocDefUri, ((RelatedTopicModel) value).toJSON());
                } else if (value instanceof List) {
                    json.put(assocDefUri, DeepaMehtaUtils.toJSONArray((List<RelatedTopicModel>) value));
                } else {
                    throw new RuntimeException("Unexpected value in a ChildTopicsModel: " + value);
                }
            }
            return json;
        } catch (Exception e) {
            throw new RuntimeException("Serialization of a ChildTopicsModel failed (" + this + ")", e);
        }
    }

    @Override
    public String childTypeUri(String assocDefUri) {
        return assocDefUri.split("#")[0];
    }



    // ****************
    // *** Java API ***
    // ****************



    @Override
    public ChildTopicsModel clone() {
        ChildTopicsModel clone = new ChildTopicsModel();
        for (String assocDefUri : this) {
            Object value = get(assocDefUri);
            if (value instanceof RelatedTopicModel) {
                RelatedTopicModel model = (RelatedTopicModel) value;
                clone.put(assocDefUri, model.clone());
            } else if (value instanceof List) {
                for (RelatedTopicModel model : (List<RelatedTopicModel>) value) {
                    clone.add(assocDefUri, model.clone());
                }
            } else {
                throw new RuntimeException("Unexpected value in a ChildTopicsModel: " + value);
            }
        }
        return clone;
    }

    @Override
    public String toString() {
        return childTopics.toString();
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    void throwInvalidSingleAccess(String assocDefUri, ClassCastException e) {
        if (e.getMessage().startsWith("java.util.ArrayList cannot be cast to")) {
            throw new RuntimeException("\"" + assocDefUri + "\" is accessed as single but is defined as multi", e);
        } else {
            throw new RuntimeException("Accessing \"" + assocDefUri + "\" failed", e);
        }
    }

    void throwInvalidMultiAccess(String assocDefUri, ClassCastException e) {
        if (e.getMessage().endsWith("cannot be cast to java.util.List")) {
            throw new RuntimeException("\"" + assocDefUri + "\" is accessed as multi but is defined as single", e);
        } else {
            throw new RuntimeException("Accessing \"" + assocDefUri + " failed", e);
        }
    }
}
