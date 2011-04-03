package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;



/**
 * Collection of the data that makes up a {@link Topic}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class TopicData {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected long id;
    protected String uri;           // is never null, may be empty
    protected TopicValue value;     // is never null, may be constructed on empty string
    protected String typeUri;       // 
    protected Composite composite;  // may be null

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicData(String uri, TopicValue value, String typeUri) {
        this(uri, value, typeUri, null);
    }

    public TopicData(String uri, TopicValue value, String typeUri, Composite composite) {
        this(-1, uri, value, typeUri, composite);
    }

    /**
     * @param   uri         If <code>null</code> an empty string is used. This is OK.
     * @param   value       If <code>null</code> an empty string value is used. This is OK.
     * @param   typeUri     Mandatory. Note: only the internal meta type topic (ID 0) has no type URI (null).
     * @param   composite   If <code>null</code> composite is not initialized (remains null). This is OK.
     */
    public TopicData(long id, String uri, TopicValue value, String typeUri, Composite composite) {
        this.id = id;
        this.uri = uri != null ? uri : "";
        this.value = value != null ? value : new TopicValue("");
        this.typeUri = typeUri;
        this.composite = composite;
    }

    public TopicData(TopicData topicData) {
        this(topicData.id, topicData.uri, topicData.value, topicData.typeUri, topicData.composite);
    }

    public TopicData(Topic topic) {
        this(topic.getId(), topic.getUri(), topic.getValue(), topic.getTypeUri(), topic.getComposite());
    }

    public TopicData(JSONObject topicData) {
        try {
            this.id = -1;
            this.uri = topicData.optString("uri");
            this.value = new TopicValue(topicData.optString("value"));
            this.typeUri = topicData.getString("type_uri");
            if (topicData.has("composite")) {
                this.composite = new Composite(topicData.getJSONObject("composite"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing TopicData failed (JSONObject=" + topicData + ")", e);
        }
    }

    // ---

    protected TopicData() {
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public long getId() {
        return id;
    }

    public String getUri() {
        return uri;
    }

    public TopicValue getValue() {
        return value;
    }

    public String getTypeUri() {
        return typeUri;
    }

    public Composite getComposite() {
        return composite;
    }

    // ---

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

    public void setValue(TopicValue value) {
        this.value = value;
    }

    // ---

    public void setComposite(Composite comp) {
        this.composite = comp;
    }

    // ---

    public TopicValue getValue(String assocDefUri) {
        throw new RuntimeException("Method not implemented (" + getClass() + ")");
    }

    public void setValue(String assocDefUri, TopicValue value) {
        throw new RuntimeException("Method not implemented (" + getClass() + ")");
    }

    public Topic getRelatedTopic(String assocTypeUri, String myRoleType, String othersRoleType) {
        throw new RuntimeException("Method not implemented (" + getClass() + ")");
    }

    public Set<Topic> getRelatedTopics(String assocTypeUri, String myRoleType, String othersRoleType) {
        throw new RuntimeException("Method not implemented (" + getClass() + ")");
    }

    public Set<Association> getAssociations(String myRoleType) {
        throw new RuntimeException("Method not implemented (" + getClass() + ")");
    }

    // ---

    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("uri", uri);
            o.put("value", value.value());
            o.put("type_uri", typeUri);
            o.put("composite", composite);
            return o;
        } catch (JSONException e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // ---

    @Override
    public String toString() {
        return "topic data (id=" + id + ", uri=\"" + uri + "\", value=" + value +
            ", typeUri=\"" + typeUri + "\", composite=" + composite + ")";
    }
}
