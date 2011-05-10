package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



/**
 * Collection of the data that makes up a {@link TopicType}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class TopicTypeModel extends TopicModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String dataTypeUri;
    private Set<IndexMode> indexModes;
    private Map<String, AssociationDefinition> assocDefs;   // is never null, may be empty
    private ViewConfiguration viewConfig;                   // is never null

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicTypeModel(TopicTypeModel topicTypeModel) {
        super(topicTypeModel);
        this.dataTypeUri = topicTypeModel.getDataTypeUri();
        this.indexModes = topicTypeModel.getIndexModes();
        this.assocDefs = topicTypeModel.getAssocDefs();
        this.viewConfig = topicTypeModel.getViewConfig();
    }

    public TopicTypeModel(Topic typeTopic, String dataTypeUri, Set<IndexMode> indexModes,
                                                               ViewConfiguration viewConfig) {
        super(typeTopic);
        this.dataTypeUri = dataTypeUri;
        this.indexModes = indexModes;
        this.assocDefs = new LinkedHashMap();
        this.viewConfig = viewConfig;
    }

    public TopicTypeModel(String uri, String value, String dataTypeUri) {
        super(uri, new TopicValue(value), "dm3.core.topic_type");
        this.dataTypeUri = dataTypeUri;
        this.indexModes = new HashSet();
        this.assocDefs = new LinkedHashMap();
        this.viewConfig = new ViewConfiguration();
    }

    public TopicTypeModel(JSONObject topicTypeModel) {
        try {
            this.id = -1;
            this.uri = topicTypeModel.getString("uri");
            this.value = new TopicValue(topicTypeModel.get("value"));
            this.typeUri = "dm3.core.topic_type";
            this.composite = new Composite();
            //
            this.dataTypeUri = topicTypeModel.getString("data_type_uri");
            this.indexModes = IndexMode.parse(topicTypeModel);
            this.assocDefs = new LinkedHashMap();
            this.viewConfig = new ViewConfiguration(topicTypeModel);
            parseAssocDefs(topicTypeModel);
        } catch (Exception e) {
            throw new RuntimeException("Parsing TopicTypeModel failed (JSONObject=" + topicTypeModel + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String getDataTypeUri() {
        return dataTypeUri;
    }

    // ---

    public Set<IndexMode> getIndexModes() {
        return indexModes;
    }

    public void setIndexModes(Set<IndexMode> indexModes) {
        this.indexModes = indexModes;
    }

    // ---

    public Map<String, AssociationDefinition> getAssocDefs() {
        return assocDefs;
    }

    public AssociationDefinition getAssocDef(String assocDefUri) {
        AssociationDefinition assocDef = assocDefs.get(assocDefUri);
        if (assocDef == null) {
            throw new RuntimeException("Schema violation: association definition \"" +
                assocDefUri + "\" not found in " + this);
        }
        return assocDef;
    }

    public void addAssocDef(AssociationDefinition assocDef) {
        String assocDefUri = assocDef.getUri();
        // error check 1
        if (!getDataTypeUri().equals("dm3.core.composite")) {
            throw new RuntimeException("Association definitions can only be added to composite topic types. " +
                "Topic type \"" + getUri() + " is of data type \"" + getDataTypeUri() + "\". (" + assocDef + ")");
        }
        // error check 2
        AssociationDefinition existing = assocDefs.get(assocDefUri);
        if (existing != null) {
            throw new RuntimeException("Schema ambiguity: topic type \"" + uri + "\" has more than one " +
                "association definitions with uri \"" + assocDefUri + "\" -- Use distinct role types at position 2");
        }
        //
        assocDefs.put(assocDefUri, assocDef);
    }

    // ---

    public ViewConfiguration getViewConfig() {
        return viewConfig;
    }

    // FIXME: server-side operations on the view config settings possibly suggest they are not acually
    // view config settings but part of the topic type model. Possibly this method should be dropped.
    public Object getViewConfig(String typeUri, String settingUri) {
        return viewConfig.getSetting(typeUri, settingUri);
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            //
            o.put("data_type_uri", dataTypeUri);
            IndexMode.toJSON(indexModes, o);
            AssociationDefinition.toJSON(assocDefs.values(), o);
            viewConfig.toJSON(o);
            //
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    @Override
    public String toString() {
        return "topic type model (id=" + id + ", uri=\"" + uri + "\", value=" + value + ", typeUri=\"" + typeUri +
            "\", dataTypeUri=\"" + dataTypeUri + "\", indexModes=" + indexModes + ", assocDefs=" + assocDefs +
            ",\ntopic type " + viewConfig + ")";
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void parseAssocDefs(JSONObject topicTypeModel) throws Exception {
        JSONArray assocDefs = topicTypeModel.optJSONArray("assoc_defs");
        if (assocDefs != null) {
            for (int i = 0; i < assocDefs.length(); i++) {
                addAssocDef(new AssociationDefinition(assocDefs.getJSONObject(i), this.uri));
            }
        }
    }
}
