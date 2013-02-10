package de.deepamehta.core.model;

import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * A recursive composite of key/value pairs.
 * <p>
 * Keys are strings, values are non-null atomic (string, int, long, double, boolean)
 * or again a <code>CompositeValueModel</code>.
 */
public class CompositeValueModel {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String REF_ID_PREFIX = "ref_id:";
    private static final String REF_URI_PREFIX = "ref_uri:";
    private static final String DEL_PREFIX = "del_id:";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * Internal representation.
     * Key: String, value: TopicModel or List<TopicModel>
     */
    private Map<String, Object> values = new HashMap();
    // Note: it must be List<TopicModel>, not Set<TopicModel> (like before).
    // There may be several TopicModels with the same ID. That occurrs if the webclient user adds several new topics
    // at once (by the means of an "Add" button). In this case the ID is -1. TopicModel equality is defined solely as
    // ID equality (see DeepaMehtaObjectModel.equals()).

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public CompositeValueModel() {
    }

    public CompositeValueModel(JSONObject values) {
        try {
            Iterator<String> i = values.keys();
            while (i.hasNext()) {
                String key = i.next();
                Object value = values.get(key);
                if (value instanceof JSONArray) {
                    JSONArray valueArray = (JSONArray) value;
                    for (int j = 0; j < valueArray.length(); j++) {
                        add(key, createTopicModel(key, valueArray.get(j)));
                    }
                } else {
                    put(key, createTopicModel(key, value));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing CompositeValueModel failed (JSONObject=" + values + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    /**
     * Accesses a single-valued child.
     * Throws if there is no such child.
     */
    public TopicModel getTopic(String key) {
        TopicModel topic = (TopicModel) values.get(key);
        // error check
        if (topic == null) {
            throw new RuntimeException("Invalid access to CompositeValueModel entry \"" + key + "\": " +
                "no such entry in\n" + this);
        }
        //
        return topic;
    }

    /**
     * Accesses a single-valued child.
     * Returns a default value if there is no such child.
     */
    public TopicModel getTopic(String key, TopicModel defaultValue) {
        TopicModel topic = (TopicModel) values.get(key);
        return topic != null ? topic : defaultValue;
    }

    // ---

    /**
     * Accesses a multiple-valued child.
     * Throws if there is no such child.
     */
    public List<TopicModel> getTopics(String key) {
        try {
            List<TopicModel> topics = (List<TopicModel>) values.get(key);
            // error check
            if (topics == null) {
                throw new RuntimeException("Invalid access to CompositeValueModel entry \"" + key + "\": " +
                    "no such entry in\n" + this);
            }
            //
            return topics;
        } catch (ClassCastException e) {
            throwInvalidAccess(key, e);
            return null;    // never reached
        }
    }

    /**
     * Accesses a multiple-valued child.
     * Returns a default value if there is no such child.
     */
    public List<TopicModel> getTopics(String key, List<TopicModel> defaultValue) {
        try {
            List<TopicModel> topics = (List<TopicModel>) values.get(key);
            return topics != null ? topics : defaultValue;
        } catch (ClassCastException e) {
            throwInvalidAccess(key, e);
            return null;    // never reached
        }
    }

    // ---

    /**
     * Convenience method for accessing the *simple* value of a single-valued child.
     */
    public String getString(String key) {
        return getTopic(key).getSimpleValue().toString();
    }

    /**
     * Convenience method for accessing the *simple* value of a single-valued child.
     */
    public int getInt(String key) {
        return getTopic(key).getSimpleValue().intValue();
    }

    /**
     * Convenience method for accessing the *simple* value of a single-valued child.
     */
    public long getLong(String key) {
        return getTopic(key).getSimpleValue().longValue();
    }

    /**
     * Convenience method for accessing the *simple* value of a single-valued child.
     */
    public double getDouble(String key) {
        return getTopic(key).getSimpleValue().doubleValue();
    }

    /**
     * Convenience method for accessing the *simple* value of a single-valued child.
     */
    public boolean getBoolean(String key) {
        return getTopic(key).getSimpleValue().booleanValue();
    }

    /**
     * Convenience method for accessing the *simple* value of a single-valued child.
     */
    public Object getObject(String key) {
        return getTopic(key).getSimpleValue().value();
    }

    // ---

    /**
     * Convenience method for accessing the *composite* value of a single-valued child.
     */
    public CompositeValueModel getComposite(String key) {
        return getTopic(key).getCompositeValueModel();
    }

    // Note: there are no convenience accessors for a multiple-valued child.

    // ---

    public Object get(String key) {
        return values.get(key);
    }

    public Iterable<String> keys() {
        return values.keySet();
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public int size() {
        return values.size();
    }

    // ---

    /**
     * Puts a single-valued child. An existing value is overwritten.
     */
    public CompositeValueModel put(String key, TopicModel value) {
        // ### TODO: drop "key" argument? It is supposed to be equal to value.getTypeUri().
        try {
            // check argument
            if (value == null) {
                throw new IllegalArgumentException("Tried to put null in a CompositeValueModel");
            }
            // put value
            values.put(key, value);
            //
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Putting a value in a CompositeValueModel failed (key=\"" + key +
                "\", value=" + value + ", composite=" + this + ")", e);
        }
    }

    /**
     * Convenience method to put a single-valued child. An existing value is overwritten.
     *
     * @param   value   a String, Integer, Long, Double, Boolean, or a CompositeValueModel.
     *
     * @return  this CompositeValueModel.
     */
    public CompositeValueModel put(String key, Object value) {
        try {
            // check argument
            if (value == null) {
                throw new IllegalArgumentException("Tried to put null in a CompositeValueModel");
            } else if (value instanceof Iterable) {
                throw new IllegalArgumentException("Tried to put a " + value.getClass().getName() + " in a " +
                    "CompositeValueModel => for multiple values use add() instead of put()");
            } else if (!(value instanceof String || value instanceof Integer || value instanceof Long ||
                  value instanceof Double || value instanceof Boolean || value instanceof CompositeValueModel)) {
                throw new IllegalArgumentException("Tried to put a " + value.getClass().getName() + " in a " +
                    "CompositeValueModel (expected are String, Integer, Long, Double, Boolean, or " +
                    "CompositeValueModel)");
            }
            // put value
            TopicModel model;
            if (value instanceof CompositeValueModel) {
                model = new TopicModel(key, (CompositeValueModel) value);
            } else {
                model = new TopicModel(key, new SimpleValue(value));
            }
            put(key, model);
            //
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Putting a value in a CompositeValueModel failed (key=\"" + key +
                "\", value=" + value + ", composite=" + this + ")", e);
        }
    }

    // ---

    /**
     * Puts a by-ID topic reference for a single-valued child. An existing reference is overwritten.
     *
     * Used to maintain the assigment of an *aggregated* child.
     * Not applicable for a *compositioned* child.
     */
    public CompositeValueModel putRef(String key, long refTopicId) {
        put(key, new TopicModel(refTopicId, key));
        return this;
    }

    /**
     * Puts a by-URI topic reference for a single-valued child. An existing reference is overwritten.
     *
     * Used to maintain the assigment of an *aggregated* child.
     * Not applicable for a *compositioned* child.
     */
    public CompositeValueModel putRef(String key, String refTopicUri) {
        put(key, new TopicModel(refTopicUri, key));
        return this;
    }

    // ---

    /**
     * Adds a value to a multiple-valued child.
     */
    public CompositeValueModel add(String key, TopicModel value) {
        // ### TODO: drop "key" argument? It is supposed to be equal to value.getTypeUri().
        List<TopicModel> topics = getTopics(key, null);     // defaultValue=null
        // Note: topics just created have no child topics yet
        if (topics == null) {
            topics = new ArrayList();
            values.put(key, topics);
        }
        topics.add(value);
        return this;
    }

    /**
     * Removes a value from a multiple-valued child.
     */
    public CompositeValueModel remove(String key, TopicModel value) {
        List<TopicModel> topics = getTopics(key, null);     // defaultValue=null
        if (topics != null) {
            topics.remove(value);
        }
        return this;
    }

    // ---

    /**
     * Adds a by-ID topic reference to a multiple-valued child.
     *
     * Used to maintain the assigments of *aggregated* childs.
     * Not applicable for *compositioned* childs.
     */
    public CompositeValueModel addRef(String key, long refTopicId) {
        add(key, new TopicModel(refTopicId, key));
        return this;
    }

    /**
     * Adds a by-URI topic reference to a multiple-valued child.
     *
     * Used to maintain the assigments of *aggregated* childs.
     * Not applicable for *compositioned* childs.
     */
    public CompositeValueModel addRef(String key, String refTopicUri) {
        add(key, new TopicModel(refTopicUri, key));
        return this;
    }

    // ---

    public JSONObject toJSON() {
        try {
            JSONObject json = new JSONObject();
            for (String key : keys()) {
                Object value = values.get(key);
                if (value instanceof TopicModel) {
                    json.put(key, ((TopicModel) value).toJSON());
                } else if (value instanceof List) {
                    json.put(key, DeepaMehtaUtils.objectsToJSON((List<TopicModel>) value));
                } else {
                    throw new RuntimeException("Unexpected value in a CompositeValueModel: " + value);
                }
            }
            return json;
        } catch (Exception e) {
            throw new RuntimeException("Serialization of a CompositeValueModel failed (" + this + ")", e);
        }
    }



    // ****************
    // *** Java API ***
    // ****************



    @Override
    public CompositeValueModel clone() {
        CompositeValueModel clone = new CompositeValueModel();
        for (String key : keys()) {
            Object value = values.get(key);
            if (value instanceof TopicModel) {
                TopicModel model = (TopicModel) value;
                clone.put(key, model.clone());
            } else if (value instanceof List) {
                for (TopicModel model : (List<TopicModel>) value) {
                    clone.add(key, model.clone());
                }
            } else {
                throw new RuntimeException("Unexpected value in a CompositeValueModel: " + value);
            }
        }
        return clone;
    }

    @Override
    public String toString() {
        return values.toString();
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Creates a topic model from a JSON value.
     *
     * Both topic serialization formats are supported:
     * 1) canonic format -- contains entire topic models.
     * 2) compact format -- contains the topic value only (simple or composite).
     */
    private TopicModel createTopicModel(String key, Object value) {
        if (value instanceof JSONObject) {
            JSONObject val = (JSONObject) value;
            // we detect the canonic format by checking for a mandatory topic property
            if (val.has("type_uri")) {
                // canonic format
                return new TopicModel(val);
            } else {
                // compact format (composite topic)
                return new TopicModel(key, new CompositeValueModel(val));
            }
        } else {
            // compact format (simple topic or topic reference)
            if (value instanceof String) {
                String val = (String) value;
                if (val.startsWith(REF_ID_PREFIX)) {
                    return new TopicModel(refTopicId(val), key);    // topic reference by-ID
                } else if (val.startsWith(REF_URI_PREFIX)) {
                    return new TopicModel(refTopicUri(val), key);   // topic reference by-URI
                } else if (val.startsWith(DEL_PREFIX)) {
                    return new TopicDeletionModel(delTopicId(val)); // topic deletion reference
                }
            }
            // compact format (simple topic)
            return new TopicModel(key, new SimpleValue(value));
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
        return Long.parseLong(val.substring(DEL_PREFIX.length()));
    }

    // ---

    /**
     * ### TODO: should not be public. Specify interfaces also for model classes?
     */
    public void throwInvalidAccess(String key, ClassCastException e) {
        if (e.getMessage().endsWith("cannot be cast to java.util.List")) {
            throw new RuntimeException("Invalid access to CompositeValueModel entry \"" + key + "\": " +
                "the caller assumes it to be multiple-value but it is single-value in\n" + this, e);
        } else {
            throw new RuntimeException("Invalid access to CompositeValueModel entry \"" + key + "\" in\n" + this, e);
        }
    }
}
