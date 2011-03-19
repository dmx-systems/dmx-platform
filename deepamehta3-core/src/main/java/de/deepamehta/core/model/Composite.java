package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.Iterator;



public class Composite {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private JSONObject values;

    // ---------------------------------------------------------------------------------------------------- Constructors

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
            throw new RuntimeException("Getting key \"" + key + "\" failed");
        }
    }
}
