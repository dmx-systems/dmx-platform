package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



public class Properties {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, PropValue> values = new HashMap();

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public Properties() {
    }

    public Properties(Properties properties) {
        putAll(properties);
    }

    public Properties(Map<String, Object> map) {
        for (String key : map.keySet()) {
            put(key, new PropValue(map.get(key)));
        }
    }

    /**
     * Called by JAX-RS container to create Properties from a @FormParam
     */
    public Properties(String json) throws JSONException {
        this(new JSONObject(json));
        // FIXME: this conversion is very crude. Compare with TopicType(JSONObject type) constructor.
        // FIXME: this conversion applies to types only whereas Properties is used also for topics.
        put("de/deepamehta/core/property/TypeURI", remove("uri"));
        put("de/deepamehta/core/property/TypeLabel", remove("label"));
    }

    public Properties(JSONObject properties) {
        try {
            Iterator<String> i = properties.keys();
            while (i.hasNext()) {
                String key = i.next();
                PropValue value = new PropValue(properties.get(key));   // throws JSONException
                put(key, value);
            }
        } catch (JSONException e) {
            throw new RuntimeException("Constructing Properties from JSONObject failed", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public PropValue get(String key) {
        return values.get(key);
    }

    // ---

    public void put(String key, String value) {
        if (value == null) {
            throw new NullPointerException("Tried to put null value for key \"" + key +"\"");
        }
        values.put(key, new PropValue(value));
    }

    public void put(String key, int value) {
        values.put(key, new PropValue(value));
    }

    public void put(String key, long value) {
        values.put(key, new PropValue(value));
    }

    public void put(String key, boolean value) {
        values.put(key, new PropValue(value));
    }

    public void put(String key, PropValue value) {
        values.put(key, value);
    }

    // ---

    public void putAll(Properties properties) {
        for (String key : properties.keySet()) {
            put(key, properties.get(key));
        }
    }

    // ---

    public PropValue remove(String key) {
        return values.remove(key);
    }

    public Set<String> keySet() {
        return values.keySet();
    }

    // ---

    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            for (String key : keySet()) {
                o.put(key, get(key).value());
            }
            return o;
        } catch (JSONException e) {
            throw new RuntimeException("Serializing " + this + " failed", e);
        }
    }

    public Map toMap() {
        Map map = new HashMap();
        for (String key : keySet()) {
            map.put(key, get(key).value());
        }
        return map;
    }

    // ---

    @Override
    public String toString() {
        return values.toString();
    }

    @Override
    public boolean equals(Object o) {
        return ((Properties) o).values.equals(values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }
}
