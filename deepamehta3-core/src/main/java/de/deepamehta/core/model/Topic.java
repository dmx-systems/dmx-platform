package de.deepamehta.core.model;

import de.deepamehta.core.util.JSONHelper;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * A topic -- DeepaMehta's core data object.
 * A topic has an ID, a type, a label, a set of properties, and is related to other topics via {@link Relation}s.
 * <p>
 * Instances of this class are used to pass data around (<i>data transfer object</i>).
 * <p>
 * Note: instances of this class are not backed by a database.
 * That is, direct changes to a Topic object (e.g. by {@link #setProperty}) are not persistent.
 * To make persistent changes use the methods of the DeepaMehta core service
 * ({@link de.deepamehta.core.service.CoreService}).
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class Topic {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    public long id;
    public String typeUri;
    public String label;

    protected Properties properties;
    private   Map<String, Object> enrichment;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public Topic(long id, String typeUri, String label, Properties properties) {
        this.id = id;
        this.typeUri = typeUri;
        this.label = label;
        this.properties = properties != null ? properties : new Properties();
        this.enrichment = new HashMap();
    }

    public Topic(Topic topic) {
        this(topic.id, topic.typeUri, topic.label, topic.properties);
    }

    public Topic(JSONObject topic) {
        try {
            typeUri = topic.getString("type_uri");
            properties = new Properties(topic.getJSONObject("properties"));
        } catch (Exception e) {
            throw new RuntimeException("Parsing " + this + " failed", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    /**
     * Returns the property value for a key.
     * If there is no property for that key an exception is thrown.
     */
    public PropValue getProperty(String key) {
        PropValue value = properties.get(key);
        if (value == null) {
            throw new RuntimeException("Property \"" + key + "\" of " + this + " is not initialized. " +
                "Remember: topics obtained by getRelatedTopics() provide no properties. " +
                "Use the providePropertiesHook() to initialize the properties you need.");
        }
        return value;
    }

    /**
     * Returns the property value for a key.
     * If there is no property for that key a default value is returned.
     * This method never returns <code>null</code>.
     *
     * @param   the default value. May be <code>null</code>. In this case a "no-value" respresenting
     *          <code>PropValue</code> object is used as the default value.
     */
    public PropValue getProperty(String key, PropValue defaultValue) {
        PropValue value = properties.get(key);
        return value != null ? value : defaultValue != null ? defaultValue : new PropValue();
    }

    public Properties getProperties() {
        return properties;
    }

    // ---

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public void setProperty(String key, int value) {
        properties.put(key, value);
    }

    public void setProperty(String key, long value) {
        properties.put(key, value);
    }

    public void setProperty(String key, boolean value) {
        properties.put(key, value);
    }

    public void setProperty(String key, PropValue value) {
        properties.put(key, value);
    }

    // ---

    /**
     * Sets various properties at once.
     * Same as consecutive {@link setProperty} calls.
     */
    public void setProperties(Properties properties) {
        this.properties.putAll(properties);
    }

    // ---

    public void setEnrichment(String key, Object value) {
        enrichment.put(key, value);
    }

    // ---

    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("type_uri", typeUri);
            o.put("label", label);
            o.put("properties", properties.toJSON());
            serializeEnrichment(o);
            return o;
        } catch (JSONException e) {
            throw new RuntimeException("Serializing " + this + " failed", e);
        }
    }

    public static JSONArray topicsToJson(List<Topic> topics) {
        JSONArray array = new JSONArray();
        for (Topic topic : topics) {
            array.put(topic.toJSON());
        }
        return array;
    }

    // ---

    @Override
    public String toString() {
        return "topic " + id + " \"" + label + "\" (typeUri=" + typeUri + ")";
    }

    // ----------------------------------------------------------------------------------------------- Protected Methods

    protected void serializeEnrichment(JSONObject o) throws JSONException {
        for (String key : enrichment.keySet()) {
            Object value = enrichment.get(key);
            o.put(key, value instanceof Map ? new JSONObject((Map) value) : value);
        }
    }
}
