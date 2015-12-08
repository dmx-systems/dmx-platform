package de.deepamehta.core.model;

import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * A recursive composite of key/value pairs. ### FIXDOC
 * <p>
 * Keys are strings, values are non-null atomic (string, int, long, double, boolean)
 * or again a <code>ChildTopicsModel</code>. ### FIXDOC
 */
public class ChildTopicsModel implements Iterable<String> {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String REF_ID_PREFIX  = "ref_id:";
    private static final String REF_URI_PREFIX = "ref_uri:";
    private static final String DEL_ID_PREFIX  = "del_id:";
    private static final String DEL_URI_PREFIX = "del_uri:";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * Internal representation.
     * Key: assoc def URI (String), value: RelatedTopicModel or List<RelatedTopicModel>
     */
    private Map<String, Object> childTopics = new HashMap();
    // Note: it must be List<RelatedTopicModel>, not Set<RelatedTopicModel>.
    // There may be several TopicModels with the same ID. That occurrs if the webclient user adds several new topics
    // at once (by the means of an "Add" button). In this case the ID is -1. TopicModel equality is defined solely as
    // ID equality (see DeepaMehtaObjectModel.equals()).

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public ChildTopicsModel() {
    }

    public ChildTopicsModel(JSONObject values) {
        try {
            Iterator<String> i = values.keys();
            while (i.hasNext()) {
                String assocDefUri = i.next();
                String childTypeUri = childTypeUri(assocDefUri);
                Object value = values.get(assocDefUri);
                if (!(value instanceof JSONArray)) {
                    put(assocDefUri, createTopicModel(childTypeUri, value));
                } else {
                    JSONArray valueArray = (JSONArray) value;
                    for (int j = 0; j < valueArray.length(); j++) {
                        add(assocDefUri, createTopicModel(childTypeUri, valueArray.get(j)));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing ChildTopicsModel failed (JSONObject=" + values + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Accessors ===

    /**
     * Accesses a single-valued child.
     * Throws if there is no such child.
     */
    public RelatedTopicModel getTopic(String assocDefUri) {
        RelatedTopicModel topic = (RelatedTopicModel) get(assocDefUri);
        // error check
        if (topic == null) {
            throw new RuntimeException("Invalid access to ChildTopicsModel entry \"" + assocDefUri +
                "\": no such entry in\n" + this);
        }
        //
        return topic;
    }

    /**
     * Accesses a single-valued child.
     * Returns a default value if there is no such child.
     */
    public RelatedTopicModel getTopic(String assocDefUri, RelatedTopicModel defaultValue) {
        RelatedTopicModel topic = (RelatedTopicModel) get(assocDefUri);
        return topic != null ? topic : defaultValue;
    }

    // ---

    /**
     * Accesses a multiple-valued child.
     * Throws if there is no such child.
     */
    public List<RelatedTopicModel> getTopics(String assocDefUri) {
        try {
            List<RelatedTopicModel> topics = (List<RelatedTopicModel>) get(assocDefUri);
            // error check
            if (topics == null) {
                throw new RuntimeException("Invalid access to ChildTopicsModel entry \"" + assocDefUri +
                    "\": no such entry in\n" + this);
            }
            //
            return topics;
        } catch (ClassCastException e) {
            throwInvalidAccess(assocDefUri, e);
            return null;    // never reached
        }
    }

    /**
     * Accesses a multiple-valued child.
     * Returns a default value if there is no such child.
     */
    public List<RelatedTopicModel> getTopics(String assocDefUri, List<RelatedTopicModel> defaultValue) {
        try {
            List<RelatedTopicModel> topics = (List<RelatedTopicModel>) get(assocDefUri);
            return topics != null ? topics : defaultValue;
        } catch (ClassCastException e) {
            throwInvalidAccess(assocDefUri, e);
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
    public Object get(String assocDefUri) {
        return childTopics.get(assocDefUri);
    }

    /**
     * Checks if a child is contained in this ChildTopicsModel.
     */
    public boolean has(String assocDefUri) {
        return childTopics.containsKey(assocDefUri);
    }

    /**
     * Returns the number of childs contained in this ChildTopicsModel.
     * Multiple-valued childs count as one.
     */
    public int size() {
        return childTopics.size();
    }



    // === Convenience Accessors ===

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    public String getString(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().toString();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    public String getString(String assocDefUri, String defaultValue) {
        TopicModel topic = getTopic(assocDefUri, null);
        return topic != null ? topic.getSimpleValue().toString() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    public int getInt(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().intValue();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    public int getInt(String assocDefUri, int defaultValue) {
        TopicModel topic = getTopic(assocDefUri, null);
        return topic != null ? topic.getSimpleValue().intValue() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    public long getLong(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().longValue();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    public long getLong(String assocDefUri, long defaultValue) {
        TopicModel topic = getTopic(assocDefUri, null);
        return topic != null ? topic.getSimpleValue().longValue() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    public double getDouble(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().doubleValue();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    public double getDouble(String assocDefUri, double defaultValue) {
        TopicModel topic = getTopic(assocDefUri, null);
        return topic != null ? topic.getSimpleValue().doubleValue() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    public boolean getBoolean(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().booleanValue();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    public boolean getBoolean(String assocDefUri, boolean defaultValue) {
        TopicModel topic = getTopic(assocDefUri, null);
        return topic != null ? topic.getSimpleValue().booleanValue() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    public Object getObject(String assocDefUri) {
        return getTopic(assocDefUri).getSimpleValue().value();
    }

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    public Object getObject(String assocDefUri, Object defaultValue) {
        TopicModel topic = getTopic(assocDefUri, null);
        return topic != null ? topic.getSimpleValue().value() : defaultValue;
    }

    // ---

    /**
     * Convenience accessor for the *composite* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    public ChildTopicsModel getChildTopicsModel(String assocDefUri) {
        return getTopic(assocDefUri).getChildTopicsModel();
    }

    /**
     * Convenience accessor for the *composite* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
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
    public ChildTopicsModel put(String assocDefUri, ChildTopicsModel value) {
        return put(assocDefUri, new TopicModel(childTypeUri(assocDefUri), value));
    }

    // ---

    /**
     * Puts a by-ID topic reference in a single-valued child.
     * An existing reference is overwritten.
     */
    public ChildTopicsModel putRef(String assocDefUri, long refTopicId) {
        put(assocDefUri, new TopicReferenceModel(refTopicId));
        return this;
    }

    /**
     * Puts a by-URI topic reference in a single-valued child.
     * An existing reference is overwritten.
     */
    public ChildTopicsModel putRef(String assocDefUri, String refTopicUri) {
        put(assocDefUri, new TopicReferenceModel(refTopicUri));
        return this;
    }

    // ---

    /**
     * Puts a by-ID topic deletion reference to a single-valued child.
     * An existing value is overwritten.
     */
    public ChildTopicsModel putDeletionRef(String assocDefUri, long refTopicId) {
        put(assocDefUri, new TopicDeletionModel(refTopicId));
        return this;
    }

    /**
     * Puts a by-URI topic deletion reference to a single-valued child.
     * An existing value is overwritten.
     */
    public ChildTopicsModel putDeletionRef(String assocDefUri, String refTopicUri) {
        put(assocDefUri, new TopicDeletionModel(refTopicUri));
        return this;
    }

    // ---

    /**
     * Removes a single-valued child.
     */
    public ChildTopicsModel remove(String assocDefUri) {
        childTopics.remove(assocDefUri);    // ### TODO: throw if not in map?
        return this;
    }

    // --- Multiple-valued Childs ---

    /**
     * Adds a value to a multiple-valued child.
     */
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

    public ChildTopicsModel add(String assocDefUri, TopicModel value) {
        return add(assocDefUri, new RelatedTopicModel(value));
    }

    /**
     * Sets the values of a multiple-valued child.
     * Existing values are overwritten.
     */
    public ChildTopicsModel put(String assocDefUri, List<RelatedTopicModel> values) {
        childTopics.put(assocDefUri, values);
        return this;
    }

    /**
     * Removes a value from a multiple-valued child.
     */
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
    public ChildTopicsModel addRef(String assocDefUri, long refTopicId) {
        add(assocDefUri, new TopicReferenceModel(refTopicId));
        return this;
    }

    /**
     * Adds a by-URI topic reference to a multiple-valued child.
     */
    public ChildTopicsModel addRef(String assocDefUri, String refTopicUri) {
        add(assocDefUri, new TopicReferenceModel(refTopicUri));
        return this;
    }

    // ---

    /**
     * Adds a by-ID topic deletion reference to a multiple-valued child.
     */
    public ChildTopicsModel addDeletionRef(String assocDefUri, long refTopicId) {
        add(assocDefUri, new TopicDeletionModel(refTopicId));
        return this;
    }

    /**
     * Adds a by-URI topic deletion reference to a multiple-valued child.
     */
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



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Creates a topic model from a JSON value.
     *
     * Both topic serialization formats are supported:
     * 1) canonic format -- contains entire topic models.
     * 2) simplified format -- contains the topic value only (simple or composite).
     */
    private RelatedTopicModel createTopicModel(String childTypeUri, Object value) throws JSONException {
        if (value instanceof JSONObject) {
            JSONObject val = (JSONObject) value;
            // we detect the canonic format by checking for mandatory topic properties
            if (val.has("value") || val.has("childs")) {
                // canonic format (topic or topic reference)
                AssociationModel relatingAssoc = null;
                if (val.has("assoc")) {
                    relatingAssoc = new AssociationModel(val.getJSONObject("assoc"));
                }
                if (val.has("value")) {
                    RelatedTopicModel topicRef = createReferenceModel(val.get("value"), relatingAssoc);
                    if (topicRef != null) {
                        return topicRef;
                    }
                }
                //
                initTypeUri(val, childTypeUri);
                //
                TopicModel topic = new TopicModel(val);
                if (relatingAssoc != null) {
                    return new RelatedTopicModel(topic, relatingAssoc);
                } else {
                    return new RelatedTopicModel(topic);
                }
            } else {
                // simplified format (composite topic)
                return new RelatedTopicModel(new TopicModel(childTypeUri, new ChildTopicsModel(val)));
            }
        } else {
            // simplified format (simple topic or topic reference)
            RelatedTopicModel topicRef = createReferenceModel(value, null);
            if (topicRef != null) {
                return topicRef;
            }
            // simplified format (simple topic)
            return new RelatedTopicModel(new TopicModel(childTypeUri, new SimpleValue(value)));
        }
    }

    private RelatedTopicModel createReferenceModel(Object value, AssociationModel relatingAssoc) {
        if (value instanceof String) {
            String val = (String) value;
            if (val.startsWith(REF_ID_PREFIX)) {
                long topicId = refTopicId(val);
                if (relatingAssoc != null) {
                    return new TopicReferenceModel(topicId, relatingAssoc);
                } else {
                    return new TopicReferenceModel(topicId);
                }
            } else if (val.startsWith(REF_URI_PREFIX)) {
                String topicUri = refTopicUri(val);
                if (relatingAssoc != null) {
                    return new TopicReferenceModel(topicUri, relatingAssoc);
                } else {
                    return new TopicReferenceModel(topicUri);
                }
            } else if (val.startsWith(DEL_ID_PREFIX)) {
                return new TopicDeletionModel(delTopicId(val));
            } else if (val.startsWith(DEL_URI_PREFIX)) {
                return new TopicDeletionModel(delTopicUri(val));
            }
        }
        return null;
    }

    private void initTypeUri(JSONObject value, String childTypeUri) throws JSONException {
        if (!value.has("type_uri")) {
            value.put("type_uri", childTypeUri);
        } else {
            // sanity check
            String typeUri = value.getString("type_uri");
            if (!typeUri.equals(childTypeUri)) {
                throw new IllegalArgumentException("A \"" + childTypeUri + "\" topic model has type_uri=\"" +
                    typeUri + "\"");
            }
        }
    }

    // ---

    private long refTopicId(String val) {
        return Long.parseLong(val.substring(REF_ID_PREFIX.length()));
    }

    private String refTopicUri(String val) {
        return val.substring(REF_URI_PREFIX.length());
    }

    private long delTopicId(String val) {
        return Long.parseLong(val.substring(DEL_ID_PREFIX.length()));
    }

    private String delTopicUri(String val) {
        return val.substring(DEL_URI_PREFIX.length());
    }

    // ---

    /**
     * ### TODO: should not be public. Specify interfaces also for model classes?
     */
    public void throwInvalidAccess(String assocDefUri, ClassCastException e) {
        if (e.getMessage().endsWith("cannot be cast to java.util.List")) {
            throw new RuntimeException("Invalid access to ChildTopicsModel entry \"" + assocDefUri +
                "\": the caller assumes it to be multiple-value but it is single-value in\n" + this, e);
        } else {
            throw new RuntimeException("Invalid access to ChildTopicsModel entry \"" + assocDefUri +
                "\" in\n" + this, e);
        }
    }
}
