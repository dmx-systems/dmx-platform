package de.deepamehta.core.model;

import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;



/**
 * A recursive composite of key/value pairs. ### FIXDOC
 * <p>
 * Keys are strings, values are non-null atomic (string, int, long, double, boolean)
 * or again a <code>CompositeValue</code>.
 */
public class CompositeValue {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String REF_ID_PREFIX = "ref_id:";
    private static final String REF_URI_PREFIX = "ref_uri:";
    private static final String DEL_PREFIX = "del_id:";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * Internal representation.
     * Key: String, value: TopicModel or List<TopicModel>
     */
    private Map<String, Object> values = new HashMap<String, Object>();
    // Note: it must be List<TopicModel>, not Set<TopicModel> (like before).
    // There may be several TopicModels with the same ID. That occurrs if the webclient user adds several new topics
    // at once (by the means of an "Add" button). In this case the ID is -1. TopicModel equality is defined solely as
    // ID equality (see DeepaMehtaObjectModel.equals()).

    // ---------------------------------------------------------------------------------------------------- Constructors

    public CompositeValue() {
    }

    public CompositeValue(JSONObject values) {
        try {
            Iterator<String> i = DeepaMehtaUtils.cast(values.keys());
            while (i.hasNext()) {
                String key = i.next();
                Object value = values.get(key);
                if (value instanceof JSONArray) {
                    List<TopicModel> models = new ArrayList<TopicModel>();
                    for (int j = 0; j < ((JSONArray) value).length(); j++) {
                        models.add(createTopicModel(key, ((JSONArray) value).get(j)));
                    }
                    put(key, models);
                } else {
                    put(key, createTopicModel(key, value));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing CompositeValue failed (JSONObject=" + values + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public TopicModel getTopic(String key) {
        TopicModel topic = (TopicModel) values.get(key);
        // error check
        if (topic == null) {
            throw new RuntimeException("Invalid access to CompositeValue entry \"" + key + "\": " +
                "no such entry in\n" + this);
        }
        //
        return topic;
    }

    public TopicModel getTopic(String key, TopicModel defaultValue) {
        TopicModel topic = (TopicModel) values.get(key);
        return topic != null ? topic : defaultValue;
    }

    // ---

    public List<TopicModel> getTopics(String key) {
        try {
            List<TopicModel> topics = DeepaMehtaUtils.cast(values.get(key));
            // error check
            if (topics == null) {
                throw new RuntimeException("Invalid access to CompositeValue entry \"" + key + "\": " +
                    "no such entry in\n" + this);
            }
            //
            return topics;
        } catch (ClassCastException e) {
            throwInvalidAccess(key, e);
            return null;    // never reached
        }
    }

    public List<TopicModel> getTopics(String key, List<TopicModel> defaultValue) {
        try {
            List<TopicModel> topics = DeepaMehtaUtils.cast(values.get(key));
            return topics != null ? topics : defaultValue;
        } catch (ClassCastException e) {
            throwInvalidAccess(key, e);
            return null;    // never reached
        }
    }

    // ---

    public CompositeValue put(String key, TopicModel value) {
        try {
            // check argument
            if (value == null) {
                throw new IllegalArgumentException("Tried to put null in a CompositeValue");
            }
            // put value
            values.put(key, value);
            //
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Putting a value in a CompositeValue failed (key=\"" + key +
                "\", value=" + value + ", composite=" + this + ")", e);
        }
    }

    public CompositeValue put(String key, List<TopicModel> value) {
        try {
            // check argument
            if (value == null) {
                throw new IllegalArgumentException("Tried to put null in a CompositeValue");
            }
            // put value
            values.put(key, value);
            //
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Putting a value in a CompositeValue failed (key=\"" + key +
                "\", value=" + value + ", composite=" + this + ")", e);
        }
    }

    /**
     * Convenience method.
     *
     * @param   value   a String, Integer, Long, Double, Boolean, or a CompositeValue.
     *
     * @return  this CompositeValue.
     */
    public CompositeValue put(String key, Object value) {
        try {
            // check argument
            if (value == null) {
                throw new IllegalArgumentException("Tried to put null in a CompositeValue");
            }
            if (!(value instanceof String || value instanceof Integer || value instanceof Long ||
                  value instanceof Double || value instanceof Boolean || value instanceof CompositeValue)) {
                throw new IllegalArgumentException("Tried to put a " + value.getClass().getName() + " in a " +
                    "CompositeValue (expected are String, Integer, Long, Double, Boolean, or CompositeValue)");
            }
            // put value
            TopicModel model;
            if (value instanceof CompositeValue) {
                model = new TopicModel(key, (CompositeValue) value);
            } else {
                model = new TopicModel(key, new SimpleValue(value));
            }
            put(key, model);
            //
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Putting a value in a CompositeValue failed (key=\"" + key +
                "\", value=" + value + ", composite=" + this + ")", e);
        }
    }

    // ---

    public CompositeValue putRef(String key, long refTopicId) {
        put(key, new TopicModel(refTopicId, key));
        return this;
    }

    public CompositeValue putRef(String key, String refTopicUri) {
        put(key, new TopicModel(refTopicUri, key));
        return this;
    }

    // ---

    public Iterable<String> keys() {
        return values.keySet();
    }

    public boolean has(String key) {
        return values.containsKey(key);
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
    public Object get(String key) {
        return getTopic(key).getSimpleValue().value();
    }

    // ---

    /**
     * Convenience method for accessing the *composite* value of a single-valued child.
     */
    public CompositeValue getComposite(String key) {
        return getTopic(key).getCompositeValue();
    }

    // Note: there are no convenience accessors for a multiple-valued child.

    // ---

    public JSONObject toJSON() {
        try {
            JSONObject json = new JSONObject();
            for (String key : keys()) {
                Object value = values.get(key);
                if (value instanceof TopicModel) {
                    json.put(key, ((TopicModel) value).toJSON());
                } else if (value instanceof List) {
                    List<TopicModel> topics = DeepaMehtaUtils.cast(value);
                    json.put(key, DeepaMehtaUtils.objectsToJSON(topics));
                } else {
                    throw new RuntimeException("Unexpected value in a CompositeValue: " + value);
                }
            }
            return json;
        } catch (Exception e) {
            throw new RuntimeException("Serialization of a CompositeValue failed (" + this + ")", e);
        }
    }



    // ****************
    // *** Java API ***
    // ****************



    @Override
    public CompositeValue clone() {
        CompositeValue clone = new CompositeValue();
        for (String key : keys()) {
            Object value = values.get(key);
            if (value instanceof TopicModel) {
                TopicModel model = ((TopicModel) value).clone();
                clone.put(key, model);
            } else if (value instanceof List) {
                List<TopicModel> models = new ArrayList<TopicModel>();
                List<TopicModel> valueList = DeepaMehtaUtils.cast(value);
                for (TopicModel model : valueList) {
                    models.add(model.clone());
                }
                clone.put(key, models);
            } else {
                throw new RuntimeException("Unexpected value in a CompositeValue: " + value);
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
                return new TopicModel(key, new CompositeValue(val));
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

    private void throwInvalidAccess(String key, ClassCastException e) {
        if (e.getMessage().equals("de.deepamehta.core.model.TopicModel cannot be cast to java.util.List")) {
            throw new RuntimeException("Invalid access to CompositeValue entry \"" + key + "\": " +
                "the caller assumes it to be multiple-value but it is single-value in\n" + this, e);
        } else {
            throw new RuntimeException("Invalid access to CompositeValue entry \"" + key + "\" in\n" + this, e);
        }
    }
}
