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

    protected String uri;
    protected TopicValue value;
    protected String typeUri;
    protected Composite composite;

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * @param   uri     If <code>null</code> the topic will have no URI. This is OK.
     * @param   value   If <code>null</code> the topic will have no value. This is OK.
     */
    public TopicData(String uri, TopicValue value, String typeUri, Composite composite) {
        this.uri = uri;
        this.value = value;
        this.typeUri = typeUri;
        this.composite = composite;
    }

    public TopicData(TopicData topicData) {
        this(topicData.uri, topicData.value, topicData.typeUri, topicData.composite);
    }

    public TopicData(JSONObject topicData) {
        try {
            if (topicData.has("uri")) {
                this.uri = topicData.getString("uri");
            }
            if (topicData.has("value")) {
                this.value = new TopicValue(topicData.get("value"));
            }
            this.typeUri = topicData.getString("type_uri");
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
            // Note: "uri" and "value" are optional
            o.put("uri", uri);
            if (value != null) {
                o.put("value", value.value());
            }
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
