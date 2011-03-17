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

    public TopicData(String uri, Object value, String typeUri, Composite composite) {
        this.uri = uri;
        this.value = new TopicValue(value);
        this.typeUri = typeUri;
        this.composite = composite;
    }

    public TopicData(TopicData topic) {
        this(topic.uri, topic.value, topic.typeUri, topic.composite);
    }

    public TopicData(JSONObject topic) {
        try {
            this.uri = topic.getString("uri");
            this.value = new TopicValue(topic.get("value"));
            this.typeUri = topic.getString("topic_type");
        } catch (Exception e) {
            throw new RuntimeException("Parsing " + this + " failed", e);
        }
    }

    // ---

    protected TopicData() {
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String getUri() {
        return uri;
    }

    public Object getValue() {
        return value.value();
    }

    public String getTypeUri() {
        return typeUri;
    }

    public Composite getComposite() {
        return composite;
    }
}
