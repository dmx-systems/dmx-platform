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

    private Set<IndexMode> indexModes;
    private Map<String, AssociationDefinitionModel> assocDefModels;   // is never null, may be empty

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicTypeModel(String uri, String value, String dataTypeUri) {
        this(uri, value, "dm3.core.topic_type", dataTypeUri);
    }

    public TopicTypeModel(String uri, String value, String topicTypeUri, String dataTypeUri) {
        super(uri, new TopicValue(value), topicTypeUri, dataTypeUri);
        this.indexModes = new HashSet();
        this.assocDefModels = new LinkedHashMap();
    }

    public TopicTypeModel(TopicModel model, String dataTypeUri, Set<IndexMode> indexModes,
                                                                ViewConfigurationModel viewConfigModel) {
        super(model, dataTypeUri, viewConfigModel);
        this.indexModes = indexModes;
        this.assocDefModels = new LinkedHashMap();
    }

    public TopicTypeModel(TopicTypeModel model) {
        super(model);
        this.indexModes = model.getIndexModes();
        this.assocDefModels = model.getAssocDefs();
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
            this.indexModes = IndexMode.parse(topicTypeModel);
            this.assocDefModels = new LinkedHashMap();
            parseAssocDefs(topicTypeModel);
        } catch (Exception e) {
            throw new RuntimeException("Parsing TopicTypeModel failed (JSONObject=" + topicTypeModel + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public Set<IndexMode> getIndexModes() {
        return indexModes;
    }

    public void setIndexModes(Set<IndexMode> indexModes) {
        this.indexModes = indexModes;
    }

    // ---

    public Map<String, AssociationDefinitionModel> getAssocDefs() {
        return assocDefModels;
    }

    public AssociationDefinitionModel getAssocDef(String assocDefUri) {
        AssociationDefinitionModel model = assocDefModels.get(assocDefUri);
        if (model == null) {
            throw new RuntimeException("Schema violation: association definition \"" +
                assocDefUri + "\" not found in " + this);
        }
        return model;
    }

    public void addAssocDef(AssociationDefinitionModel assocDef) {
        String assocDefUri = assocDef.getUri();
        // sanity check ### FIXME: drop this check or provide proper feedback to the type editor user
        if (!getDataTypeUri().equals("dm3.core.composite")) {
            throw new RuntimeException("Association definitions can only be added to composite topic types. " +
                "Topic type \"" + getUri() + "\" is of data type \"" + getDataTypeUri() + "\". (" + assocDef + ")");
        }
        // error check
        AssociationDefinitionModel existing = assocDefModels.get(assocDefUri);
        if (existing != null) {
            throw new RuntimeException("Schema ambiguity: topic type \"" + uri + "\" has more than one " +
                "association definitions with uri \"" + assocDefUri + "\" -- Use distinct role types at position 2");
        }
        //
        updateAssocDef(assocDef);
    }

    public void updateAssocDef(AssociationDefinitionModel assocDef) {
        assocDefModels.put(assocDef.getUri(), assocDef);
    }

    public AssociationDefinitionModel removeAssocDef(String assocDefUri) {
        // error check
        getAssocDef(assocDefUri);
        //
        return assocDefModels.remove(assocDefUri);
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            //
            o.put("data_type_uri", getDataTypeUri());
            IndexMode.toJSON(indexModes, o);
            AssociationDefinitionModel.toJSON(assocDefModels.values(), o);
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
            "\", dataTypeUri=\"" + getDataTypeUri() + "\", indexModes=" + indexModes + ", assocDefs=" + assocDefModels +
            ",\n    topic type " + getViewConfigModel() + ")";
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void parseAssocDefs(JSONObject topicTypeModel) throws Exception {
        JSONArray models = topicTypeModel.optJSONArray("assoc_defs");
        if (models != null) {
            for (int i = 0; i < models.length(); i++) {
                addAssocDef(new AssociationDefinitionModel(models.getJSONObject(i), this.uri));
            }
        }
    }
}
