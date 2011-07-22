package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.Iterator;



/**
 * A recursive composite of key/value pairs.
 * <p>
 * Keys are strings, values are non-null atomic (string, int, long, double, boolean)
 * or again a <code>CompositeValue</code>.
 */
public class CompositeValue {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * Internal representation.
     * Key: String, value: non-null atomic (String, Integer, Long, Double, Boolean) or composite (JSONObject).
     */
    private JSONObject values;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public CompositeValue() {
        this.values = new JSONObject();
    }

    public CompositeValue(JSONObject values) {
        this.values = values;
    }

    public CompositeValue(String json) {
        try {
            this.values = new JSONObject(json);
        } catch (Exception e) {
            throw new RuntimeException("Constructing a CompositeValue from a string failed (\"" + json + "\")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public Iterator<String> keys() {
        return values.keys();
    }

    public Object get(String key) {
        try {
            Object value = values.get(key);
            if (value instanceof JSONObject) {
                return new CompositeValue((JSONObject) value);
            } else {
                return value;
            }
        } catch (Exception e) {
            throw new RuntimeException("Getting a value from a CompositeValue failed (key=\"" +
                key + "\", composite=" + this + ")", e);
        }
    }

    /**
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
            if (value instanceof CompositeValue) {
                value = ((CompositeValue) value).values;
            }
            values.put(key, value);
            //
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Putting a value in a CompositeValue failed (key=\"" + key +
                "\", value=" + value + ", composite=" + this + ")", e);
        }
    }

    public boolean has(String key) {
        return values.has(key);
    }

    // ---

    public String getLabel() {
        try {
            return getLabel(values);
        } catch (Exception e) {
            throw new RuntimeException("Getting the label of a CompositeValue failed (composite=" + this + ")", e);
        }
    }

    public JSONObject toJSON() {
        return values;
    }



    // ****************
    // *** Java API ***
    // ****************



    @Override
    public String toString() {
        return values.toString();
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private String getLabel(Object value) throws Exception {
        if (value instanceof JSONObject) {
            JSONObject comp = (JSONObject) value;
            Iterator<String> i = comp.keys();
            if (i.hasNext()) {
                return getLabel(comp.get(i.next()));
            } else {
                return "";
            }
        } else {
            return value.toString();
        }
    }
}
