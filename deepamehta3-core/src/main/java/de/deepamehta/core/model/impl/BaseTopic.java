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
import java.util.Set;



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

    /**
     * @param   uri     If <code>null</code> the topic will have no URI. This is OK.
     */
    protected BaseTopic(long id, String uri, TopicValue value, String typeUri, Composite composite) {
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
    public TopicValue getValue(String assocDefUri) {
        throw new RuntimeException("Method not implemented (" + getClass() + ")");
    }

    @Override
    public void setValue(String assocDefUri, TopicValue value) {
        throw new RuntimeException("Method not implemented (" + getClass() + ")");
    }

    @Override
    public Topic getRelatedTopic(String assocTypeUri, String myRoleType, String othersRoleType) {
        throw new RuntimeException("Method not implemented (" + getClass() + ")");
    }

    @Override
    public Set<Topic> getRelatedTopics(String assocTypeUri, String myRoleType, String othersRoleType) {
        throw new RuntimeException("Method not implemented (" + getClass() + ")");
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            o.put("id", id);
            return o;
        } catch (JSONException e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // ---

    @Override
    public String toString() {
        return "topic " + id + " \"" + value + "\" (uri=\"" + uri + "\", typeUri=\"" + typeUri + "\")";
    }
}
