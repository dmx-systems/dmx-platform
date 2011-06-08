package de.deepamehta.core.model;

import de.deepamehta.core.Topic;

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
public class TopicTypeModel extends TypeModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String dataTypeUri;
    private Set<IndexMode> indexModes;
    private Map<String, AssociationDefinition> assocDefs;   // is never null, may be empty

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicTypeModel(String uri, String value, String dataTypeUri) {
        this(uri, value, "dm3.core.topic_type", dataTypeUri);
    }

    public TopicTypeModel(String uri, String value, String topicTypeUri, String dataTypeUri) {
        super(uri, new TopicValue(value), topicTypeUri);
        this.dataTypeUri = dataTypeUri;
        this.indexModes = new HashSet();
        this.assocDefs = new LinkedHashMap();
    }

    public TopicTypeModel(Topic typeTopic, String dataTypeUri, Set<IndexMode> indexModes,
                                                               ViewConfigurationModel viewConfigModel) {
        super(typeTopic, viewConfigModel);
        this.dataTypeUri = dataTypeUri;
        this.indexModes = indexModes;
        this.assocDefs = new LinkedHashMap();
    }

    public TopicTypeModel(TopicTypeModel model) {
        super(model);
        this.dataTypeUri = model.getDataTypeUri();
        this.indexModes = model.getIndexModes();
        this.assocDefs = model.getAssocDefs();
    }

    public TopicTypeModel(JSONObject topicTypeModel) {
        super(topicTypeModel);
        try {
            this.id = topicTypeModel.optLong("id", -1);
            this.uri = topicTypeModel.getString("uri");
            this.value = new TopicValue(topicTypeModel.get("value"));
            this.typeUri = "dm3.core.topic_type";
            this.composite = new Composite();
            //
            this.dataTypeUri = topicTypeModel.getString("data_type_uri");
            this.indexModes = IndexMode.parse(topicTypeModel);
            this.assocDefs = new LinkedHashMap();
            parseAssocDefs(topicTypeModel);
        } catch (Exception e) {
            throw new RuntimeException("Parsing TopicTypeModel failed (JSONObject=" + topicTypeModel + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String getDataTypeUri() {
        return dataTypeUri;
    }

    public void setDataTypeUri(String dataTypeUri) {
        this.dataTypeUri = dataTypeUri;
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

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            //
            o.put("data_type_uri", dataTypeUri);
            IndexMode.toJSON(indexModes, o);
            AssociationDefinition.toJSON(assocDefs.values(), o);
            getViewConfigModel().toJSON(o);
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
            ",\ntopic type " + getViewConfigModel() + ")";
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
