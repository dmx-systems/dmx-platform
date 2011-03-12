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

    protected long id;
    protected String uri;
    protected TopicValue value;

    protected String typeUri;
    protected String label;

    private   Map<String, Object> enrichment;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public Topic(long id, String uri, TopicValue value, String typeUri, String label) {
        this.id = id;
        this.uri = uri;
        this.value = value;
        this.typeUri = typeUri;
        this.label = label;
        this.enrichment = new HashMap();
    }

    public Topic(Topic topic) {
        this(topic.id, topic.uri, topic.value, topic.typeUri, topic.label);
    }

    public Topic(JSONObject topic) {
        try {
            this.uri = topic.getString("uri");
            this.value = new TopicValue(topic.get("value"));
            this.typeUri = topic.getString("topic_type");
        } catch (Exception e) {
            throw new RuntimeException("Parsing " + this + " failed", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public void setValue(String value) {
        setValue(new TopicValue(value));
    }

    public void setValue(int value) {
        setValue(new TopicValue(value));
    }

    public void setValue(long value) {
        setValue(new TopicValue(value));
    }

    public void setValue(boolean value) {
        setValue(new TopicValue(value));
    }

    public void setValue(PropValue value) {
        this.value = value;
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
            o.put("uri", uri);
            o.put("value", value.value());
            o.put("topic_type", typeUri);
            o.put("label", label);
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
