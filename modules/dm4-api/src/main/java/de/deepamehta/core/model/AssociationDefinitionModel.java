package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;



/**
 * Definition of an association between 2 topic types -- part of DeepaMehta's type system,
 * like an association in a class diagram. Used to represent both, aggregations and compositions.
 * ### FIXDOC: also assoc types have assoc defs
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class AssociationDefinitionModel extends AssociationModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String instanceLevelAssocTypeUri;

    private String wholeTopicTypeUri;   // FIXME: wording "wholeTypeUri"
    private String partTopicTypeUri;    // FIXME: wording "partTypeUri"

    private String wholeRoleTypeUri;    // fixed: "dm4.core.whole"
    private String partRoleTypeUri;     // fixed: "dm4.core.part"

    private String wholeCardinalityUri;
    private String partCardinalityUri;

    private ViewConfigurationModel viewConfigModel;   // is never null

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationDefinitionModel(String typeUri, String wholeTopicTypeUri, String partTopicTypeUri,
                                                      String wholeCardinalityUri, String partCardinalityUri) {
        this(-1, typeUri, wholeTopicTypeUri, partTopicTypeUri, wholeCardinalityUri, partCardinalityUri, null);
    }

    public AssociationDefinitionModel(long id, String typeUri, String wholeTopicTypeUri, String partTopicTypeUri,
                                                               String wholeCardinalityUri, String partCardinalityUri,
                                                               ViewConfigurationModel viewConfigModel) {
        super(id, typeUri);
        //
        this.wholeTopicTypeUri = wholeTopicTypeUri;
        this.partTopicTypeUri = partTopicTypeUri;
        // set default role types
        this.wholeRoleTypeUri = "dm4.core.whole";// ### wholeRoleTypeUri != null ? wholeRoleTypeUri : wholeTopicTypeUri;
        this.partRoleTypeUri = "dm4.core.part";  // ### partRoleTypeUri != null ? partRoleTypeUri : partTopicTypeUri;
        //
        this.wholeCardinalityUri = wholeCardinalityUri;
        this.partCardinalityUri = partCardinalityUri;
        // derive uri
        this.uri = partTopicTypeUri;             // ### partRoleTypeUri;
        //
        this.viewConfigModel = viewConfigModel != null ? viewConfigModel : new ViewConfigurationModel();
        //
        initAssociationModel();
        initInstanceLevelAssocTypeUri();
    }

    public AssociationDefinitionModel(JSONObject assocDef, String wholeTopicTypeUri) {
        super(-1, null);
        try {
            this.id = assocDef.optLong("id", -1);
            this.typeUri = assocDef.getString("assoc_type_uri");
            //
            this.wholeTopicTypeUri = wholeTopicTypeUri;
            this.partTopicTypeUri = assocDef.getString("part_topic_type_uri");
            //
            this.wholeRoleTypeUri = "dm4.core.whole";// ## assocDef.optString("whole_role_type_uri", wholeTopicTypeUri);
            this.partRoleTypeUri = "dm4.core.part";  // ## assocDef.optString("part_role_type_uri", partTopicTypeUri);
            //
            this.uri = partTopicTypeUri;             // ### partRoleTypeUri;
            //
            if (!assocDef.has("whole_cardinality_uri") && !typeUri.equals("dm4.core.composition_def")) {
                throw new RuntimeException("\"whole_cardinality_uri\" is missing");
            }
            this.wholeCardinalityUri = assocDef.optString("whole_cardinality_uri", "dm4.core.one");
            this.partCardinalityUri = assocDef.getString("part_cardinality_uri");
            //
            this.viewConfigModel = new ViewConfigurationModel(assocDef);
            //
            initAssociationModel();
            initInstanceLevelAssocTypeUri();
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationDefinitionModel failed (JSONObject=" + assocDef + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String getInstanceLevelAssocTypeUri() {
        return instanceLevelAssocTypeUri;
    }

    public String getWholeTopicTypeUri() {
        return wholeTopicTypeUri;
    }

    public String getPartTopicTypeUri() {
        return partTopicTypeUri;
    }

    public String getWholeRoleTypeUri() {
        return wholeRoleTypeUri;
    }

    public String getPartRoleTypeUri() {
        return partRoleTypeUri;
    }

    public String getWholeCardinalityUri() {
        return wholeCardinalityUri;
    }

    public String getPartCardinalityUri() {
        return partCardinalityUri;
    }

    public ViewConfigurationModel getViewConfigModel() {
        return viewConfigModel;
    }

    // ---

    @Override
    public void setTypeUri(String typeUri) {
        super.setTypeUri(typeUri);
        initInstanceLevelAssocTypeUri();
    }

    public void setWholeCardinalityUri(String wholeCardinalityUri) {
        this.wholeCardinalityUri = wholeCardinalityUri;
    }

    public void setPartCardinalityUri(String partCardinalityUri) {
        this.partCardinalityUri = partCardinalityUri;
    }

    public void setViewConfigModel(ViewConfigurationModel viewConfigModel) {
        this.viewConfigModel = viewConfigModel;
    }

    // ---

    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            // o.put("id", id);                 // ### FIXME: drop this
            // o.put("uri", uri);               // ### FIXME: drop this
            o.put("assoc_type_uri", typeUri);   // ### FIXME: drop this
            o.put("whole_topic_type_uri", wholeTopicTypeUri);
            o.put("part_topic_type_uri", partTopicTypeUri);
            o.put("whole_role_type_uri", wholeRoleTypeUri);
            o.put("part_role_type_uri", partRoleTypeUri);
            o.put("whole_cardinality_uri", wholeCardinalityUri);
            o.put("part_cardinality_uri", partCardinalityUri);
            viewConfigModel.toJSON(o);
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // ---

    @Override
    public String toString() {
        return "\n    association definition (" + super.toString() +
            ")\n        pos 1: (type=\"" + wholeTopicTypeUri + "\", role=\"" + wholeRoleTypeUri +
            "\", cardinality=\"" + wholeCardinalityUri +
            "\")\n        pos 2: (type=\"" + partTopicTypeUri + "\", role=\"" + partRoleTypeUri +
            "\", cardinality=\"" + partCardinalityUri +
            "\")\n        association definition " + viewConfigModel;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    static void toJSON(Collection<AssociationDefinitionModel> assocDefs, JSONObject o) throws Exception {
        List<JSONObject> assocDefList = new ArrayList<JSONObject>();
        for (AssociationDefinitionModel assocDef : assocDefs) {
            assocDefList.add(assocDef.toJSON());
        }
        o.put("assoc_defs", assocDefList);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void initAssociationModel() {
        setRoleModel1(new TopicRoleModel(wholeTopicTypeUri, "dm4.core.whole_type"));
        setRoleModel2(new TopicRoleModel(partTopicTypeUri,  "dm4.core.part_type"));
    }

    private void initInstanceLevelAssocTypeUri() {
        if (typeUri.equals("dm4.core.aggregation_def")) {
            this.instanceLevelAssocTypeUri = "dm4.core.aggregation";
        } else if (typeUri.equals("dm4.core.composition_def")) {
            this.instanceLevelAssocTypeUri = "dm4.core.composition";
        } else {
            throw new RuntimeException("Unexpected association type URI: \"" + typeUri + "\"");
        }
    }
}
