package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    private String parentTypeUri;                   // derived, not serialized
    private String childTypeUri;                    // derived, not serialized

    private String parentCardinalityUri;
    private String childCardinalityUri;

    private ViewConfigurationModel viewConfigModel; // is never null

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationDefinitionModel(String typeUri, String parentTypeUri, String childTypeUri,
                                                      String parentCardinalityUri, String childCardinalityUri) {
        this(-1, null, typeUri, parentTypeUri, childTypeUri, parentCardinalityUri, childCardinalityUri, null);
    }

    public AssociationDefinitionModel(long id, String uri, String typeUri, String parentTypeUri, String childTypeUri,
                                                           String parentCardinalityUri, String childCardinalityUri,
                                                           ViewConfigurationModel viewConfigModel) {
        super(id, uri, typeUri, parentRoleModel(parentTypeUri), childRoleModel(childTypeUri));
        //
        this.instanceLevelAssocTypeUri = instanceLevelAssocTypeUri(typeUri);
        //
        this.parentTypeUri = parentTypeUri;
        this.childTypeUri = childTypeUri;
        //
        this.parentCardinalityUri = parentCardinalityUri;
        this.childCardinalityUri = childCardinalityUri;
        //
        this.viewConfigModel = viewConfigModel != null ? viewConfigModel : new ViewConfigurationModel();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String getInstanceLevelAssocTypeUri() {
        return instanceLevelAssocTypeUri;
    }

    public String getParentTypeUri() {
        return parentTypeUri;
    }

    public String getChildTypeUri() {
        return childTypeUri;
    }

    public String getParentCardinalityUri() {
        return parentCardinalityUri;
    }

    public String getChildCardinalityUri() {
        return childCardinalityUri;
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

    public void setParentCardinalityUri(String parentCardinalityUri) {
        this.parentCardinalityUri = parentCardinalityUri;
    }

    public void setChildCardinalityUri(String childCardinalityUri) {
        this.childCardinalityUri = childCardinalityUri;
    }

    public void setViewConfigModel(ViewConfigurationModel viewConfigModel) {
        this.viewConfigModel = viewConfigModel;
    }

    // ---

    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            o.put("parent_cardinality_uri", parentCardinalityUri);
            o.put("child_cardinality_uri", childCardinalityUri);
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
            ",\n        parent cardinality=\"" + parentCardinalityUri +
            "\",\n        child cardinality=\"" + childCardinalityUri +
            "\",\n        " + viewConfigModel + ")\n";
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    static AssociationDefinitionModel fromJSON(JSONObject assocDef, String parentTypeUri) {
        try {
            long id             = assocDef.optLong("id", -1);
            String uri          = null;
            String typeUri      = assocDef.getString("assoc_type_uri");
            String childTypeUri = assocDef.getString("child_type_uri");
            //
            if (!assocDef.has("parent_cardinality_uri") && !typeUri.equals("dm4.core.composition_def")) {
                throw new RuntimeException("\"parent_cardinality_uri\" is missing");
            }
            String parentCardinalityUri = assocDef.optString("parent_cardinality_uri", "dm4.core.one");
            String childCardinalityUri  = assocDef.getString("child_cardinality_uri");
            //
            ViewConfigurationModel viewConfigModel = new ViewConfigurationModel(assocDef);
            //
            return new AssociationDefinitionModel(id, uri, typeUri, parentTypeUri, childTypeUri,
                parentCardinalityUri, childCardinalityUri, viewConfigModel);
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

    private static TopicRoleModel parentRoleModel(String parentTypeUri) {
        return new TopicRoleModel(parentTypeUri, "dm4.core.parent_type");
    }

    private static TopicRoleModel childRoleModel(String childTypeUri) {
        return new TopicRoleModel(childTypeUri,  "dm4.core.child_type");
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
