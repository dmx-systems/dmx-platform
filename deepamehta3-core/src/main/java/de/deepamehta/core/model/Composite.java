package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.Iterator;



/**
 * A composite of key/value pairs. Keys are strings, values are non-null atomic (string, int, long, boolean)
 * or again a <code>Composite</code>.
 */
public class Composite {

    // ---------------------------------------------------------------------------------------------- Instance Variables

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
            throw new RuntimeException("Constructing a Composite from a JSON string failed (\"" + json + "\")", e);
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
            throw new RuntimeException("Getting value failed (key=\"" + key + "\", composite=" + this + ")");
        }
    }

    public void put(String key, Object value) {
        try {
            if (value == null) {
                throw new IllegalArgumentException("Tried to put a null value in a Composite");
            }
            values.put(key, value);
        } catch (Exception e) {
            throw new RuntimeException("Putting value failed (key=\"" + key + "\", value=" + value +
                ", composite=" + this + ")");
        }
    }

    // ---

    public JSONObject toJSON() {
        return values;
    }

    // ---

    @Override
    public String toString() {
        return values.toString();
    }
}
