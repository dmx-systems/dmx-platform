package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



/**
 * Collection of the data that makes up a {@link TopicType}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class TopicTypeData extends TopicData {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String dataTypeUri;
    private Map<String, AssociationDefinition> assocDefs;
    private Set<TopicData> viewConfig;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicTypeData(TopicTypeData topicTypeData) {
        super(topicTypeData);
        this.dataTypeUri = topicTypeData.getDataTypeUri();
        this.assocDefs = topicTypeData.getAssocDefs();
        this.viewConfig = topicTypeData.getViewConfig();
    }

    public TopicTypeData(Topic typeTopic, String dataTypeUri, Set<TopicData> viewConfig) {
        super(typeTopic);
        this.dataTypeUri = dataTypeUri;
        this.assocDefs = new HashMap();
        this.viewConfig = viewConfig;
    }

    public TopicTypeData(String uri, String value, String dataTypeUri) {
        super(uri, new TopicValue(value), "dm3.core.topic_type");
        this.dataTypeUri = dataTypeUri;
        this.assocDefs = new HashMap();
        this.viewConfig = new HashSet();
    }

    public TopicTypeData(JSONObject topicType) {
        try {
            this.id = -1;
            this.uri = topicType.getString("uri");
            this.value = new TopicValue(topicType.get("value"));
            this.typeUri = "dm3.core.topic_type";
            this.composite = null;
            //
            this.dataTypeUri = topicType.getString("data_type_uri");
            this.assocDefs = new HashMap();
            this.viewConfig = parseViewConfig(topicType);
            parseAssocDefs(topicType);
        } catch (Exception e) {
            throw new RuntimeException("Parsing TopicTypeData failed (JSONObject=" + topicType + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String getDataTypeUri() {
        return dataTypeUri;
    }

    public Map<String, AssociationDefinition> getAssocDefs() {
        return assocDefs;
    }

    public Set<TopicData> getViewConfig() {
        return viewConfig;
    }

    // ---

    public AssociationDefinition getAssocDef(String assocDefUri) {
        AssociationDefinition assocDef = assocDefs.get(assocDefUri);
        if (assocDef == null) {
            throw new RuntimeException("Association definition \"" + assocDefUri + "\" not found (in " + this + ")");
        }
        return assocDef;
    }

    // FIXME: abstraction. Adding should be the factory's resposibility
    public void addAssocDef(AssociationDefinition assocDef) {
        String assocDefUri = assocDef.getUri();
        AssociationDefinition existing = assocDefs.get(assocDefUri);
        if (existing != null) {
            throw new RuntimeException("Ambiguity: topic type definition \"" + uri + "\" has more than " +
                "one association definitions with uri \"" + assocDefUri + "\" -- Use distinct part role types or " +
                "specifiy an unique uri");
        }
        assocDefs.put(assocDefUri, assocDef);
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            o.put("data_type_uri", dataTypeUri);
            //
            List assocDefs = new ArrayList();
            for (AssociationDefinition assocDef : this.assocDefs.values()) {
                assocDefs.add(assocDef.toJSON());
            }
            o.put("assoc_defs", assocDefs);
            //
            List viewConfigTopics = new ArrayList();
            for (TopicData topicData : viewConfig) {
                viewConfigTopics.add(topicData.toJSON());
            }
            o.put("view_config_topics", viewConfigTopics);
            //
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    @Override
    public String toString() {
        return "topic type data (id=" + id + ", uri=\"" + uri + "\", value=" + value + ", typeUri=\"" + typeUri +
            "\", dataTypeUri=\"" + dataTypeUri + "\",\nassocDefs=" + assocDefs + ",\nviewConfig=" + viewConfig + ")";
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void parseAssocDefs(JSONObject topicType) throws Exception {
        JSONArray assocDefs = topicType.optJSONArray("assoc_defs");
        if (assocDefs != null) {
            for (int i = 0; i < assocDefs.length(); i++) {
                addAssocDef(new AssociationDefinition(assocDefs.getJSONObject(i), this.uri));
            }
        }
    }

    private Set<TopicData> parseViewConfig(JSONObject topicType) throws Exception {
        Set<TopicData> viewConfig = new HashSet();
        JSONArray topics = topicType.optJSONArray("view_config_topics");
        if (topics != null) {
            for (int i = 0; i < topics.length(); i++) {
                viewConfig.add(new TopicData(topics.getJSONObject(i)));
            }
        }
        return viewConfig;
    }
}
