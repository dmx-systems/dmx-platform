package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONException;
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

    private String customAssocTypeUri;

    private String parentTypeUri;   // derived, not serialized
    private String childTypeUri;    // derived, not serialized

    private String parentCardinalityUri;
    private String childCardinalityUri;

    private ViewConfigurationModel viewConfigModel; // is never null

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationDefinitionModel(String typeUri, String parentTypeUri, String childTypeUri,
                                                      String parentCardinalityUri, String childCardinalityUri) {
        this(-1, null, typeUri, null, parentTypeUri, childTypeUri, parentCardinalityUri, childCardinalityUri, null);
    }

    /**
     * @param   customAssocTypeUri      if null no custom association type will be set.
     */
    public AssociationDefinitionModel(long id, String uri, String typeUri, String customAssocTypeUri,
                                                           String parentTypeUri, String childTypeUri,
                                                           String parentCardinalityUri, String childCardinalityUri,
                                                           ViewConfigurationModel viewConfigModel) {
        super(id, uri, typeUri, parentRoleModel(parentTypeUri), childRoleModel(childTypeUri));
        //
        this.customAssocTypeUri = customAssocTypeUri;
        //
        this.parentTypeUri = parentTypeUri;
        this.childTypeUri = childTypeUri;
        //
        this.parentCardinalityUri = parentCardinalityUri;
        this.childCardinalityUri = childCardinalityUri;
        //
        this.viewConfigModel = viewConfigModel != null ? viewConfigModel : new ViewConfigurationModel();
    }

    AssociationDefinitionModel(JSONObject assocDef) throws JSONException {
        super(assocDef.optLong("id", -1), null, assocDef.getString("assoc_type_uri"), parentRoleModel(assocDef),
                                                                                      childRoleModel(assocDef));
        // Note: getString() called on a key with JSON null value would return the string "null"
        this.customAssocTypeUri = assocDef.isNull("custom_assoc_type_uri") ? null :
            assocDef.getString("custom_assoc_type_uri");
        //
        this.parentTypeUri = parentTypeUri();
        this.childTypeUri = childTypeUri();
        //
        if (!assocDef.has("parent_cardinality_uri") && !typeUri.equals("dm4.core.composition_def")) {
            throw new RuntimeException("\"parent_cardinality_uri\" is missing");
        }
        this.parentCardinalityUri = assocDef.optString("parent_cardinality_uri", "dm4.core.one");
        this.childCardinalityUri  = assocDef.getString("child_cardinality_uri");
        //
        this.viewConfigModel = new ViewConfigurationModel(assocDef);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String getCustomAssocTypeUri() {
        return customAssocTypeUri;
    }

    /**
     * The type to be used to create an association instance based on this association definition.
     */
    public String getInstanceLevelAssocTypeUri() {
        return customAssocTypeUri !=null ? customAssocTypeUri : defaultInstanceLevelAssocTypeUri();
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

    public void setCustomAssocTypeUri(String customAssocTypeUri) {
        this.customAssocTypeUri = customAssocTypeUri;
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
            o.put("custom_assoc_type_uri", customAssocTypeUri != null ? customAssocTypeUri : JSONObject.NULL);
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
            "\",\n        custom association type=\"" + customAssocTypeUri +
            "\",\n        " + viewConfigModel + ")\n";
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    static void toJSON(Collection<AssociationDefinitionModel> assocDefs, JSONObject o) throws Exception {
        List assocDefList = new ArrayList();
        for (AssociationDefinitionModel assocDef : assocDefs) {
            assocDefList.add(assocDef.toJSON());
        }
        o.put("assoc_defs", assocDefList);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private static TopicRoleModel parentRoleModel(JSONObject assocDef) throws JSONException {
        return parentRoleModel(assocDef.getString("parent_type_uri"));
    }

    private static TopicRoleModel childRoleModel(JSONObject assocDef) throws JSONException {
        return childRoleModel(assocDef.getString("child_type_uri"));
    }

    // ---

    private static TopicRoleModel parentRoleModel(String parentTypeUri) {
        return new TopicRoleModel(parentTypeUri, "dm4.core.parent_type");
    }

    private static TopicRoleModel childRoleModel(String childTypeUri) {
        return new TopicRoleModel(childTypeUri, "dm4.core.child_type");
    }

    // ---

    private String parentTypeUri() {
        return ((TopicRoleModel) getRoleModel("dm4.core.parent_type")).getTopicUri();
    }

    private String childTypeUri() {
        return ((TopicRoleModel) getRoleModel("dm4.core.child_type")).getTopicUri();
    }

    // ---

    private String defaultInstanceLevelAssocTypeUri() {
        if (typeUri.equals("dm4.core.aggregation_def")) {
            return "dm4.core.aggregation";
        } else if (typeUri.equals("dm4.core.composition_def")) {
            return "dm4.core.composition";
        } else {
            throw new RuntimeException("Unexpected association type URI: \"" + typeUri + "\"");
        }
    }
}
