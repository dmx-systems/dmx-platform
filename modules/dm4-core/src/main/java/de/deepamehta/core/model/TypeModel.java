package de.deepamehta.core.model;

import de.deepamehta.core.util.DeepaMehtaUtils;
import de.deepamehta.core.util.SequencedHashMap;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



public abstract class TypeModel extends TopicModel implements Iterable<String> {

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

    public TypeModel(JSONObject typeModel) {
        super(typeModel);
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

    public AssociationDefinitionModel getAssocDef(String assocDefUri) {
        return getAssocDefOrThrow(assocDefUri);
    }

    /**
     * @param   assocDef    the assoc def to add.
     *                      Note: its ID might be uninitialized (-1).
     */
    public TypeModel addAssocDef(AssociationDefinitionModel assocDef) {
        return addAssocDefBefore(assocDef, null);   // beforeAssocDefUri=null
    }

    /**
     * @param   assocDef            the assoc def to add.
     *                              Note: its ID might be uninitialized (-1).
     * @param   beforeAssocDefUri   the URI of the assoc def <i>before</i> the given assoc def is inserted.
     *                              If <code>null</code> the assoc def is appended at the end.
     */
    public TypeModel addAssocDefBefore(AssociationDefinitionModel assocDef, String beforeAssocDefUri) {
        try {
            // error check
            String assocDefUri = assocDef.getAssocDefUri();
            AssociationDefinitionModel existing = _getAssocDef(assocDefUri);
            if (existing != null) {
                throw new RuntimeException("Schema ambiguity: type \"" + getUri() + "\" has more than one \"" +
                    assocDefUri + "\" association definitions");
            }
            //
            assocDefs.putBefore(assocDefUri, assocDef, beforeAssocDefUri);
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Adding an association definition to type \"" + getUri() + "\" before \"" +
                beforeAssocDefUri + "\" failed" + assocDef, e);
        }
    }

    public AssociationDefinitionModel removeAssocDef(String assocDefUri) {
        try {
            AssociationDefinitionModel assocDef = assocDefs.remove(assocDefUri);
            if (assocDef == null) {
                throw new RuntimeException("Schema violation: association definition \"" + assocDefUri +
                    "\" not found in " + assocDefs.keySet());
            }
            return assocDef;
        } catch (Exception e) {
            throw new RuntimeException("Removing association definition \"" + assocDefUri + "\" from type \"" +
                getUri() + "\" failed", e);
        }
    }

    public void removeAllAssocDefs() {
        assocDefs.clear();
    }

    // ---

    /**
     * Finds an assoc def by ID and returns its URI (at index 0). Returns the URI of the next-in-sequence
     * assoc def as well (at index 1), or null if the found assoc def is the last one.
     */
    public String[] findAssocDefUris(long assocDefId) {
        if (assocDefId == -1) {
            throw new IllegalArgumentException("findAssocDefUris() called with assocDefId=-1");
        }
        String[] assocDefUris = new String[2];
        Iterator<String> i = iterator();
        while (i.hasNext()) {
            String assocDefUri = i.next();
            long _assocDefId = _getAssocDef(assocDefUri).getId();
            if (_assocDefId == -1) {
                throw new IllegalStateException("findAssocDefUris() called when assoc defs are not yet saved");
            }
            if (_assocDefId == assocDefId) {
                assocDefUris[0] = assocDefUri;
                if (i.hasNext()) {
                    assocDefUris[1] = i.next();
                }
                break;
            }
        }
        if (assocDefUris[0] == null) {
            throw new RuntimeException("Assoc def with ID " + assocDefId + " not found in assoc defs of type \"" +
                getUri() + "\" (" + assocDefs.keySet() + ")");
        }
        return assocDefUris;
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



    // === Iterable Implementation ===

    /**
     * Returns an interator which iterates this TypeModel's assoc def URIs.
     */
    @Override
    public Iterator<String> iterator() {
        return assocDefs.keySet().iterator();
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
            o.put("label_config", new JSONArray(getLabelConfig()));
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

    private List<String> parseLabelConfig(JSONObject typeModel) throws JSONException {
        if (typeModel.has("label_config")) {
            return DeepaMehtaUtils.toList(typeModel.getJSONArray("label_config"));
        }
        return new ArrayList();
    }

    private void parseAssocDefs(JSONObject typeModel) throws JSONException {
        JSONArray assocDefs = typeModel.optJSONArray("assoc_defs");
        if (assocDefs != null) {
            for (int i = 0; i < assocDefs.length(); i++) {
                JSONObject assocDef = assocDefs.getJSONObject(i);
                assocDef.put("parent_type_uri", this.uri);
                addAssocDef(new AssociationDefinitionModel(assocDef));
            }
        }
    }

    // ---

    private AssociationDefinitionModel getAssocDefOrThrow(String assocDefUri) {
        AssociationDefinitionModel assocDef = _getAssocDef(assocDefUri);
        if (assocDef == null) {
            throw new RuntimeException("Schema violation: association definition \"" + assocDefUri +
                "\" not found in " + assocDefs.keySet());
        }
        return assocDef;
    }

    private AssociationDefinitionModel _getAssocDef(String assocDefUri) {
        return assocDefs.get(assocDefUri);
    }
}
