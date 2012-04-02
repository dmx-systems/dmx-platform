package de.deepamehta.core.model;

import de.deepamehta.core.util.JSONHelper;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;



/**
 * A recursive composite of key/value pairs. ### FIXDOC
 * <p>
 * Keys are strings, values are non-null atomic (string, int, long, double, boolean)
 * or again a <code>CompositeValue</code>.
 */
public class CompositeValue {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * Internal representation.
     * Key: String, value: TopicModel or Set<TopicModel>
     */
    private Map<String, Object> values = new HashMap();

    // ---------------------------------------------------------------------------------------------------- Constructors

    public CompositeValue() {
    }

    public CompositeValue(JSONObject values) {
        try {
            Iterator<String> i = values.keys();
            while (i.hasNext()) {
                String key = i.next();
                Object value = values.get(key);
                if (value instanceof JSONArray) {
                    Set<TopicModel> models = new LinkedHashSet();
                    for (int j = 0; j < ((JSONArray) value).length(); j++) {
                        models.add(model(((JSONArray) value).get(j)));
                    }
                    put(key, models);
                } else {
                    put(key, model(value));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing CompositeValue failed (JSONObject=" + values + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public Iterable<String> keys() {
        return values.keySet();
    }

    // ---

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

    public Set<TopicModel> getTopics(String key) {
        try {
            Set<TopicModel> topics = (Set<TopicModel>) values.get(key);
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

    public Set<TopicModel> getTopics(String key, Set<TopicModel> defaultValue) {
        try {
            Set<TopicModel> topics = (Set<TopicModel>) values.get(key);
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
                throw new IllegalArgumentException("Tried to put a null value in a CompositeValue");
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

    public CompositeValue put(String key, Set<TopicModel> value) {
        try {
            // check argument
            if (value == null) {
                throw new IllegalArgumentException("Tried to put a null value in a CompositeValue");
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
                throw new IllegalArgumentException("Tried to put a null value in a CompositeValue");
            }
            if (!(value instanceof String || value instanceof Integer || value instanceof Long ||
                  value instanceof Double || value instanceof Boolean || value instanceof CompositeValue)) {
                throw new IllegalArgumentException("Tried to put a " + value.getClass().getName() + " value in " +
                    "a CompositeValue (expected are String, Integer, Long, Double, Boolean, or CompositeValue)");
            }
            // put value
            TopicModel model;
            if (value instanceof CompositeValue) {
                model = new TopicModel(null, (CompositeValue) value);   // typeUri=null
            } else {
                model = new TopicModel(null, new SimpleValue(value));   // typeUri=null
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

    public boolean has(String key) {
        return values.containsKey(key);
    }

    // ---

    /**
     * Convenience method.
     */
    public String getString(String key) {
        return getTopic(key).getSimpleValue().toString();
    }

    /**
     * Convenience method.
     */
    public int getInt(String key) {
        return getTopic(key).getSimpleValue().intValue();
    }

    /**
     * Convenience method.
     */
    public long getLong(String key) {
        return getTopic(key).getSimpleValue().longValue();
    }

    /**
     * Convenience method.
     */
    public double getDouble(String key) {
        return getTopic(key).getSimpleValue().doubleValue();
    }

    /**
     * Convenience method.
     */
    public boolean getBoolean(String key) {
        return getTopic(key).getSimpleValue().booleanValue();
    }

    /**
     * Convenience method.
     */
    public Object get(String key) {
        return getTopic(key).getSimpleValue().value();
    }

    /**
     * Convenience method.
     */
    public CompositeValue getComposite(String key) {
        return getTopic(key).getCompositeValue();
    }

    // ---

    public JSONObject toJSON() {
        try {
            JSONObject json = new JSONObject();
            for (String key : keys()) {
                Object value = values.get(key);
                if (value instanceof TopicModel) {
                    json.put(key, ((TopicModel) value).toJSON());
                } else if (value instanceof Set) {
                    json.put(key, JSONHelper.objectsToJSON((Set<TopicModel>) value));
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
            } else if (value instanceof Set) {
                Set<TopicModel> models = new LinkedHashSet();
                for (TopicModel model : (Set<TopicModel>) value) {
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

    private TopicModel model(Object value) {
        if (value instanceof JSONObject) {
            return new TopicModel(null, new CompositeValue((JSONObject) value));    // typeUri=null
        } else {
            return new TopicModel(null, new SimpleValue(value));                    // typeUri=null
        }
    }

    private void throwInvalidAccess(String key, ClassCastException e) {
        if (e.getMessage().equals("de.deepamehta.core.model.TopicModel cannot be cast to java.util.Set")) {
            throw new RuntimeException("Invalid access to CompositeValue entry \"" + key + "\": " +
                "the caller assumes it to be multiple-value but it is single-value in\n" + this, e);
        } else {
            throw new RuntimeException("Invalid access to CompositeValue entry \"" + key + "\" in\n" + this, e);
        }
    }
}
