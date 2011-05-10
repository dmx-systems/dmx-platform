package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.Iterator;



/**
 * A recursive composite of key/value pairs.
 * Keys are strings, values are non-null atomic (string, int, long, boolean) or again a <code>Composite</code>.
 */
public class Composite {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * Internal representation.
     * Key: String, value: non-null atomic (String, Integer, Long, Boolean) or composite (JSONObject).
     */
    private JSONObject values;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public Composite() {
        this.values = new JSONObject();
    }

    public Composite(JSONObject values) {
        this.values = values;
    }

    public Composite(String json) {
        try {
            this.values = new JSONObject(json);
        } catch (Exception e) {
            throw new RuntimeException("Constructing a Composite from a string failed (\"" + json + "\")", e);
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
                return new Composite((JSONObject) value);
            } else {
                return value;
            }
        } catch (Exception e) {
            throw new RuntimeException("Getting a value from a Composite failed (key=\"" +
                key + "\", composite=" + this + ")", e);
        }
    }

    /**
     * @param   value   a String, Integer, Long, Boolean, or a Composite.
     */
    public void put(String key, Object value) {
        try {
            // check argument
            if (value == null) {
                throw new IllegalArgumentException("Tried to put a null value in a Composite");
            }
            if (!(value instanceof String || value instanceof Integer || value instanceof Long ||
                  value instanceof Boolean || value instanceof Composite)) {
                throw new IllegalArgumentException("Tried to put a " + value.getClass().getName() +
                    " value in a Composite (expected are String, Integer, Long, Boolean, or Composite)");
            }
            // put value
            if (value instanceof Composite) {
                value = ((Composite) value).values;
            }
            values.put(key, value);
        } catch (Exception e) {
            throw new RuntimeException("Putting a value in a Composite failed (key=\"" + key +
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
            throw new RuntimeException("Getting the label of a Composite failed (composite=" + this + ")", e);
        }
    }

    public JSONObject toJSON() {
        return values;
    }

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
