package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;



/**
 * A recursive composite of key/value pairs. ### FIXDOC
 * <p>
 * Keys are strings, values are non-null atomic (string, int, long, double, boolean)
 * or again a <code>CompositeValue</code>.
 */
public class CompositeValue {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * Internal representation. ### FIXDOC
     * Key: String, value: non-null atomic (String, Integer, Long, Double, Boolean) or composite (JSONObject).
     */
    private Map<String, TopicModel> values = new HashMap();

    // ---------------------------------------------------------------------------------------------------- Constructors

    public CompositeValue() {
    }

    public CompositeValue(JSONObject values) {
        try {
            Iterator<String> i = values.keys();
            while (i.hasNext()) {
                String key = i.next();
                Object value = values.get(key);
                TopicModel model;
                if (value instanceof JSONObject) {
                    model = new TopicModel(null, new CompositeValue((JSONObject) value));   // typeUri=null
                } else {
                    model = new TopicModel(null, new SimpleValue(value));                   // typeUri=null
                }
                put(key, model);
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing CompositeValue failed (JSONObject=" + values + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public Iterable<String> keys() {
        return values.keySet();
    }

    public TopicModel getTopic(String key) {
        TopicModel topic = values.get(key);
        // error check
        if (topic == null) {
            throw new RuntimeException("No entry \"" + key + "\" in composite value " + this);
        }
        //
        return topic;
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
                json.put(key, getTopic(key).toJSON());
            }
            return json;
        } catch (JSONException e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }



    // ****************
    // *** Java API ***
    // ****************



    @Override
    public CompositeValue clone() {
        CompositeValue clone = new CompositeValue();
        for (String key : keys()) {
            TopicModel model = (TopicModel) getTopic(key).clone();
            clone.put(key, model);
        }
        return clone;
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
