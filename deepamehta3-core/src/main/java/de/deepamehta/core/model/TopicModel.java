package de.deepamehta.core.model;

import de.deepamehta.core.Topic;

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
public class TopicModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected long id;
    protected String uri;           // is never null, may be empty
    protected TopicValue value;     // is never null, may be constructed on empty string
    protected String typeUri;       //
    protected Composite composite;  // is never null, may be empty

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicModel(String typeUri) {
        this(null, null, typeUri);
    }

    public TopicModel(String typeUri, Composite composite) {
        this(null, null, typeUri, composite);
    }

    public TopicModel(String uri, TopicValue value, String typeUri) {
        this(uri, value, typeUri, null);
    }

    public TopicModel(String uri, TopicValue value, String typeUri, Composite composite) {
        this(-1, uri, value, typeUri, composite);
    }

    /**
     * @param   uri         If <code>null</code> an empty string is set. This is OK.
     * @param   value       If <code>null</code> an empty string value is set. This is OK.
     * @param   typeUri     Mandatory. Note: only the internal meta type topic (ID 0) has no type URI (null).
     * @param   composite   If <code>null</code> an empty composite is set. This is OK.
     */
    public TopicModel(long id, String uri, TopicValue value, String typeUri, Composite composite) {
        this.id = id;
        this.uri = uri != null ? uri : "";
        this.value = value != null ? value : new TopicValue("");
        this.typeUri = typeUri;
        this.composite = composite != null ? composite : new Composite();
    }

    public TopicModel(TopicModel topicModel) {
        this(topicModel.id, topicModel.uri, topicModel.value, topicModel.typeUri, topicModel.composite);
    }

    public TopicModel(JSONObject topicModel) {
        try {
            this.id = topicModel.optLong("id", -1);
            this.uri = topicModel.optString("uri");
            this.value = new TopicValue(topicModel.optString("value"));
            this.typeUri = topicModel.getString("type_uri");
            if (topicModel.has("composite")) {
                this.composite = new Composite(topicModel.getJSONObject("composite"));
            } else {
                this.composite = new Composite();
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing TopicModel failed (JSONObject=" + topicModel + ")", e);
        }
    }

    // ---

    protected TopicModel() {
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

    public void setId(long id) {
        this.id = id;
    }

    public void setUri(String uri) {
        this.uri = uri;
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

    // === Serialization ===

    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("uri", uri);
            o.put("value", value.value());
            o.put("type_uri", typeUri);
            o.put("composite", composite.toJSON());
            return o;
        } catch (JSONException e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    public static JSONArray topicsToJSON(Iterable<Topic> topics) {
        JSONArray array = new JSONArray();
        for (Topic topic : topics) {
            array.put(topic.toJSON());
        }
        return array;
    }

    // === Java API ===

    @Override
    public boolean equals(Object o) {
        return ((TopicModel) o).id == id;
    }

    @Override
    public int hashCode() {
        return ((Long) id).hashCode();
    }

    @Override
    public String toString() {
        return "topic model (id=" + id + ", uri=\"" + uri + "\", value=" + value +
            ", typeUri=\"" + typeUri + "\", composite=" + composite + ")";
    }
}
