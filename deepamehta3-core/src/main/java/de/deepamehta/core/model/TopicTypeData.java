package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * A topic type. Part of the meta-model (like a class).
 * <p>
 * A topic type itself is a {@link Topic}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class TopicTypeData extends TopicData {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected String dataTypeUri;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicTypeData(TopicData topicData, String dataTypeUri) {
        super(topicData);
        this.dataTypeUri = dataTypeUri;
    }

    public TopicTypeData(TopicTypeData topicTypeData) {
        super(topicTypeData);
        this.dataTypeUri = topicTypeData.getDataTypeUri();
    }

    public TopicTypeData(JSONObject type) {
        try {
            this.uri = type.getString("uri");
            this.value = new TopicValue(type.get("value"));
            this.typeUri = "dm3.core.topic_type";
            this.dataTypeUri = type.getString("data_type");
        } catch (Exception e) {
            throw new RuntimeException("Parsing " + this + " failed", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String getDataTypeUri() {
        return dataTypeUri;
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            o.put("data_type", dataTypeUri);
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serializing " + this + " failed", e);
        }
    }

    @Override
    public String toString() {
        return "topic type data (uri=\"" + uri + "\", value=" + value + ", typeUri=\"" + typeUri +
            "\", dataTypeUri=\"" + dataTypeUri + "\", composite=" + composite + ")";
    }
}
