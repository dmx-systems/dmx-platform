package de.deepamehta.core.model;

import de.deepamehta.core.util.JSONHelper;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.HashMap;
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

    protected Map<String, Object> properties;
    private   Map<String, Object> enrichment;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public Topic(long id, String typeUri, String label, Map properties) {
        this.id = id;
        this.typeUri = typeUri;
        this.label = label;
        this.properties = properties != null ? properties : new HashMap();
        this.enrichment = new HashMap();
    }

    public Topic(Topic topic) {
        this(topic.id, topic.typeUri, topic.label, topic.properties);
    }

    public Topic(JSONObject topic) {
        try {
            typeUri = topic.getString("type_uri");
            properties = JSONHelper.toMap(topic.getJSONObject("properties"));
        } catch (Throwable e) {
            throw new RuntimeException("Error while parsing " + this, e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public Object getProperty(String key) {
        Object value = properties.get(key);
        if (value == null) {
            throw new RuntimeException("Property \"" + key + "\" of " + this + " is not initialized. " +
                "Remember: topics obtained by getRelatedTopics() provide no properties. " +
                "Use the providePropertiesHook() to initialize the properties you need.");
        }
        return value;
    }

    public Object getProperty(String key, Object defaultValue) {
        Object value = properties.get(key);
        return value != null ? value : defaultValue;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    // ---

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    /**
     * Sets various properties at once.
     * Same as consecutive {@link setProperty} calls.
     */
    public void setProperties(Map<String, Object> properties) {
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
            o.put("properties", properties);
            //
            serializeEnrichment(o);
            //
            return o;
        } catch (JSONException e) {
            throw new RuntimeException("Error while serializing " + this, e);
        }
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
