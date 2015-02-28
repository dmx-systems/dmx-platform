package de.deepamehta.core.model;

import de.deepamehta.core.util.DeepaMehtaUtils;
import de.deepamehta.core.util.SequencedHashMap;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;



public abstract class TypeModel extends TopicModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String dataTypeUri;
    private List<IndexMode> indexModes;
    private SequencedHashMap<String, AssociationDefinitionModel> assocDefs; // is never null, may be empty
    private List<String> labelConfig;                                       // is never null, may be empty
    private ViewConfigurationModel viewConfig;                              // is never null

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TypeModel(String uri, String topicTypeUri, SimpleValue value, String dataTypeUri) {
        super(uri, topicTypeUri, value);
        this.dataTypeUri = dataTypeUri;
        this.indexModes = new ArrayList();
        this.assocDefs = new SequencedHashMap();
        this.labelConfig = new ArrayList();
        this.viewConfig = new ViewConfigurationModel();
    }

    public TypeModel(TopicModel topic, String dataTypeUri, List<IndexMode> indexModes,
                     List<AssociationDefinitionModel> assocDefs, List<String> labelConfig,
                     ViewConfigurationModel viewConfig) {
        super(topic);
        this.dataTypeUri = dataTypeUri;
        this.indexModes = indexModes;
        this.assocDefs = new SequencedHashMap();
        for (AssociationDefinitionModel assocDef : assocDefs) {
            addAssocDef(assocDef);
        }
        this.labelConfig = labelConfig;
        this.viewConfig = viewConfig;
    }

    public TypeModel(JSONObject typeModel, String typeUri) {
        super(typeModel, typeUri);
        try {
            this.dataTypeUri = typeModel.getString("data_type_uri");
            this.indexModes = IndexMode.parse(typeModel);
            this.assocDefs = new SequencedHashMap();
            this.labelConfig = parseLabelConfig(typeModel);
            this.viewConfig = new ViewConfigurationModel(typeModel);
            parseAssocDefs(typeModel);
        } catch (Exception e) {
            throw new RuntimeException("Parsing TypeModel failed (JSONObject=" + typeModel + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // === Data Type ===

    public String getDataTypeUri() {
        return dataTypeUri;
    }

    public void setDataTypeUri(String dataTypeUri) {
        this.dataTypeUri = dataTypeUri;
    }

    // === Index Modes ===

    public List<IndexMode> getIndexModes() {
        return indexModes;
    }

    public void addIndexMode(IndexMode indexMode) {
        indexModes.add(indexMode);
    }

    // === Association Definitions ===

    public Collection<AssociationDefinitionModel> getAssocDefs() {
        return assocDefs.values();
    }

    public AssociationDefinitionModel getAssocDef(String childTypeUri) {
        AssociationDefinitionModel assocDef = assocDefs.get(childTypeUri);
        if (assocDef == null) {
            throw new RuntimeException("Schema violation: association definition \"" +
                childTypeUri + "\" not found in " + this);
        }
        return assocDef;
    }

    public TypeModel addAssocDef(AssociationDefinitionModel assocDef) {
        return addAssocDefBefore(assocDef, null);   // beforeChildTypeUri=null
    }

    /**
     * @param   beforeChildTypeUri  the assoc def <i>before</i> the assoc def is inserted into the sequence.
     *                              If <code>null</code> the assoc def is appended at the end.
     */
    public TypeModel addAssocDefBefore(AssociationDefinitionModel assocDef, String beforeChildTypeUri) {
        // error check
        String childTypeUri = assocDef.getChildTypeUri();
        AssociationDefinitionModel existing = assocDefs.get(childTypeUri);
        if (existing != null) {
            throw new RuntimeException("Schema ambiguity: topic type \"" + uri +
                "\" has more than one association definitions with uri \"" + childTypeUri + "\"");
        }
        //
        assocDefs.putBefore(assocDef.getChildTypeUri(), assocDef, beforeChildTypeUri);
        return this;
    }

    public void updateAssocDef(AssociationDefinitionModel assocDef) {
        assocDefs.put(assocDef.getChildTypeUri(), assocDef);
    }

    public AssociationDefinitionModel removeAssocDef(String childTypeUri) {
        // error check
        getAssocDef(childTypeUri);
        //
        return assocDefs.remove(childTypeUri);
    }

    public void removeAllAssocDefs() {
        assocDefs.clear();
    }

    // === Label Configuration ===

    public List<String> getLabelConfig() {
        return labelConfig;
    }

    public void setLabelConfig(List<String> labelConfig) {
        this.labelConfig = labelConfig;
    }

    // === View Configuration ===

    public ViewConfigurationModel getViewConfigModel() {
        return viewConfig;
    }

    // FIXME: server-side operations on the view config settings possibly suggest they are not acually
    // view config settings but part of the topic type model. Possibly this method should be dropped.
    public Object getViewConfig(String typeUri, String settingUri) {
        return viewConfig.getSetting(typeUri, settingUri);
    }

    public void setViewConfig(ViewConfigurationModel viewConfig) {
        this.viewConfig = viewConfig;
    }



    // ****************************
    // *** TopicModel Overrides ***
    // ****************************



    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            //
            o.put("data_type_uri", getDataTypeUri());
            IndexMode.toJSON(indexModes, o);
            AssociationDefinitionModel.toJSON(assocDefs.values(), o);
            o.put("label_config", DeepaMehtaUtils.stringsToJson(getLabelConfig()));
            getViewConfigModel().toJSON(o);
            //
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }



    // ****************
    // *** Java API ***
    // ****************



    @Override
    public String toString() {
        return "id=" + id + ", uri=\"" + uri + "\", value=\"" + value + "\", typeUri=\"" + typeUri +
            "\", dataTypeUri=\"" + getDataTypeUri() + "\", indexModes=" + getIndexModes() + ", assocDefs=" +
            getAssocDefs() + ", labelConfig=" + getLabelConfig() + ", " + getViewConfigModel();
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private List<String> parseLabelConfig(JSONObject typeModel) throws Exception {
        if (typeModel.has("label_config")) {
            return DeepaMehtaUtils.toList(typeModel.getJSONArray("label_config"));
        }
        return new ArrayList();
    }

    private void parseAssocDefs(JSONObject typeModel) throws Exception {
        JSONArray assocDefs = typeModel.optJSONArray("assoc_defs");
        if (assocDefs != null) {
            for (int i = 0; i < assocDefs.length(); i++) {
                JSONObject assocDef = assocDefs.getJSONObject(i);
                addAssocDef(AssociationDefinitionModel.fromJSON(assocDef, this.uri));
            }
        }
    }
}
