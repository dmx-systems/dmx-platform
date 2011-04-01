package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class TopicData {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected String uri;           // is never null, may be empty
    protected TopicValue value;     // is never null, may be constructed on empty string
    protected String typeUri;       // 
    protected Composite composite;  // may be null

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * @param   uri         If <code>null</code> an empty string is used. This is OK.
     * @param   value       If <code>null</code> an empty string value is used. This is OK.
     * @param   typeUri     Mandatory.
     * @param   composite   If <code>null</code> composite is not initialized (remains null). This is OK.
     */
    public TopicData(String uri, TopicValue value, String typeUri, Composite composite) {
        this.uri = uri != null ? uri : "";
        this.value = value != null ? value : new TopicValue("");
        this.typeUri = typeUri;
        this.composite = composite;
    }

    public TopicData(TopicData topicData) {
        this(topicData.uri, topicData.value, topicData.typeUri, topicData.composite);
    }

    public TopicData(Topic topic) {
        this(topic.getUri(), topic.getValue(), topic.getTypeUri(), topic.getComposite());
    }

    public TopicData(JSONObject topicData) {
        try {
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

    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
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
        return "topic data (uri=\"" + uri + "\", value=" + value + ", typeUri=\"" + typeUri +
            "\", composite=" + composite + ")";
    }
}
