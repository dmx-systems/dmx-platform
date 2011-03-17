package de.deepamehta.core.model.impl;

import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicData;
import de.deepamehta.core.model.TopicValue;
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
public class BaseTopic extends TopicData implements Topic {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected long id;

    // ---------------------------------------------------------------------------------------------------- Constructors

    protected BaseTopic() {
    }

    protected BaseTopic(long id, String uri, Object value, String typeUri, Composite composite) {
        super(uri, value, typeUri, composite);
        this.id = id;
    }

    protected BaseTopic(Topic topic) {
        super(topic.getUri(), topic.getValue(), topic.getTypeUri(), topic.getComposite());
        this.id = topic.getId();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public long getId() {
        return id;
    }

    // ---

    @Override
    public void setValue(String value) {
        setValue(new TopicValue(value));
    }

    @Override
    public void setValue(int value) {
        setValue(new TopicValue(value));
    }

    @Override
    public void setValue(long value) {
        setValue(new TopicValue(value));
    }

    @Override
    public void setValue(boolean value) {
        setValue(new TopicValue(value));
    }

    @Override
    public void setValue(TopicValue value) {
        this.value = value;
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("uri", uri);
            o.put("value", value.value());
            o.put("topic_type", typeUri);
            o.put("composite", composite);
            return o;
        } catch (JSONException e) {
            throw new RuntimeException("Serializing " + this + " failed", e);
        }
    }

    public static JSONArray topicsToJson(List<BaseTopic> topics) {
        JSONArray array = new JSONArray();
        for (BaseTopic topic : topics) {
            array.put(topic.toJSON());
        }
        return array;
    }

    // ---

    @Override
    public String toString() {
        return "topic " + id + " \"" + value + "\" (uri=\"" + uri + "\", typeUri=\"" + typeUri + "\")";
    }
}
