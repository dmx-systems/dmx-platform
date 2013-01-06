package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



/**
 * Definition of an association between 2 topic types -- part of DeepaMehta's type system,
 * like an association in a class diagram. Used to represent both, aggregations and compositions.
 * ### FIXDOC: also assoc types have assoc defs
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class AssociationDefinitionModel extends AssociationModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String instanceLevelAssocTypeUri;       // derived, not serialized

    private String wholeTypeUri;                    // derived, not serialized
    private String partTypeUri;                     // derived, not serialized

    private String wholeCardinalityUri;
    private String partCardinalityUri;

    private ViewConfigurationModel viewConfigModel; // is never null

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationDefinitionModel(String typeUri, String wholeTypeUri, String partTypeUri,
                                                      String wholeCardinalityUri, String partCardinalityUri) {
        this(-1, null, typeUri, wholeTypeUri, partTypeUri, wholeCardinalityUri, partCardinalityUri, null);
    }

    public AssociationDefinitionModel(long id, String uri, String typeUri, String wholeTypeUri, String partTypeUri,
                                                           String wholeCardinalityUri, String partCardinalityUri,
                                                           ViewConfigurationModel viewConfigModel) {
        super(id, uri, typeUri, wholeRoleModel(wholeTypeUri), partRoleModel(partTypeUri));
        //
        this.instanceLevelAssocTypeUri = instanceLevelAssocTypeUri(typeUri);
        //
        this.wholeTypeUri = wholeTypeUri;
        this.partTypeUri = partTypeUri;
        //
        this.wholeCardinalityUri = wholeCardinalityUri;
        this.partCardinalityUri = partCardinalityUri;
        //
        this.viewConfigModel = viewConfigModel != null ? viewConfigModel : new ViewConfigurationModel();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String getInstanceLevelAssocTypeUri() {
        return instanceLevelAssocTypeUri;
    }

    public String getWholeTypeUri() {
        return wholeTypeUri;
    }

    public String getPartTypeUri() {
        return partTypeUri;
    }

    public String getWholeRoleTypeUri() {
        return "dm4.core.whole";    // *Instance level* role type. Not the role type of the underlying association.
    }

    public String getPartRoleTypeUri() {
        return "dm4.core.part";     // *Instance level* role type. Not the role type of the underlying association.
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
        this.instanceLevelAssocTypeUri = instanceLevelAssocTypeUri(typeUri);
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
            ",\n        whole cardinality=\"" + wholeCardinalityUri +
            "\",\n        part cardinality=\"" + partCardinalityUri +
            "\",\n        " + viewConfigModel + ")\n";
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    static AssociationDefinitionModel fromJSON(JSONObject assocDef, String wholeTypeUri) {
        try {
            long id            = assocDef.optLong("id", -1);
            String uri         = null;
            String typeUri     = assocDef.getString("assoc_type_uri");
            String partTypeUri = assocDef.getString("part_type_uri");
            //
            if (!assocDef.has("whole_cardinality_uri") && !typeUri.equals("dm4.core.composition_def")) {
                throw new RuntimeException("\"whole_cardinality_uri\" is missing");
            }
            String wholeCardinalityUri = assocDef.optString("whole_cardinality_uri", "dm4.core.one");
            String partCardinalityUri  = assocDef.getString("part_cardinality_uri");
            //
            ViewConfigurationModel viewConfigModel = new ViewConfigurationModel(assocDef);
            //
            return new AssociationDefinitionModel(id, uri, typeUri, wholeTypeUri, partTypeUri,
                wholeCardinalityUri, partCardinalityUri, viewConfigModel);
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationDefinitionModel failed (JSONObject=" + assocDef + ")", e);
        }
    }

    static void toJSON(Collection<AssociationDefinitionModel> assocDefs, JSONObject o) throws Exception {
        List assocDefList = new ArrayList();
        for (AssociationDefinitionModel assocDef : assocDefs) {
            assocDefList.add(assocDef.toJSON());
        }
        o.put("assoc_defs", assocDefList);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private static TopicRoleModel wholeRoleModel(String wholeTypeUri) {
        return new TopicRoleModel(wholeTypeUri, "dm4.core.whole_type");
    }

    private static TopicRoleModel partRoleModel(String partTypeUri) {
        return new TopicRoleModel(partTypeUri,  "dm4.core.part_type");
    }

    // ---

    private String instanceLevelAssocTypeUri(String typeUri) {
        if (typeUri.equals("dm4.core.aggregation_def")) {
            return "dm4.core.aggregation";
        } else if (typeUri.equals("dm4.core.composition_def")) {
            return "dm4.core.composition";
        } else {
            throw new RuntimeException("Unexpected association type URI: \"" + typeUri + "\"");
        }
    }
}
