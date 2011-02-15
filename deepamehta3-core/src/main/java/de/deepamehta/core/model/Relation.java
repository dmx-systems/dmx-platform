package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.HashMap;
import java.util.Map;



/**
 * A relation between 2 {@link Topic}s.
 * A relation has an ID, a type, and a set of properties.
 * <p>
 * Instances of this class are used to pass data around (<i>data transfer object</i>).
 * <p>
 * Note: instances of this class are not backed by a database.
 * That is, direct changes to a Relation object (e.g. by {@link #setProperty}) are not persistent.
 * To make persistent changes use the methods of the DeepaMehta core service
 * ({@link de.deepamehta.core.service.CoreService}).
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class Relation {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    public long id;
    public String typeId;
    public long srcTopicId;
    public long dstTopicId;

    protected Properties properties;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public Relation(long id, String typeId, long srcTopicId, long dstTopicId, Properties properties) {
        this.id = id;
        this.typeId = typeId;
        this.srcTopicId = srcTopicId;
        this.dstTopicId = dstTopicId;
        this.properties = properties != null ? properties : new Properties();
    }

    public Relation(Relation relation) {
        this(relation.id, relation.typeId, relation.srcTopicId, relation.dstTopicId, relation.properties);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public PropValue getProperty(String key) {
        PropValue value = properties.get(key);
        if (value == null) {
            throw new RuntimeException("Property \"" + key + "\" of " + this + " is not initialized. " +
                "Remember: relations obtained by getRelatedTopics() provide no properties. " +
                "Use the providePropertiesHook() to initialize the properties you need.");
        }
        return value;
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

    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("type_id", typeId);
            o.put("src_topic_id", srcTopicId);
            o.put("dst_topic_id", dstTopicId);
            o.put("properties", properties.toJSON());
            return o;
        } catch (JSONException e) {
            throw new RuntimeException("Serializing " + this + " failed", e);
        }
    }

    // ---

    @Override
    public String toString() {
        return "relation " + id + ", connecting topics " + srcTopicId + " and " + dstTopicId +
            " (typeId=" + typeId + ")";
    }
}
