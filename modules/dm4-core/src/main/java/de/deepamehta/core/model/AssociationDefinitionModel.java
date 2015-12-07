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

    private String parentCardinalityUri;
    private String childCardinalityUri;

    private ViewConfigurationModel viewConfigModel; // is never null

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationDefinitionModel(String assocTypeUri, String parentTypeUri, String childTypeUri,
                                                           String parentCardinalityUri, String childCardinalityUri) {
        this(assocTypeUri, null, parentTypeUri, childTypeUri, parentCardinalityUri, childCardinalityUri);
    }

    public AssociationDefinitionModel(String assocTypeUri, String customAssocTypeUri,
                                                           String parentTypeUri, String childTypeUri,
                                                           String parentCardinalityUri, String childCardinalityUri) {
        this(-1, null, assocTypeUri, customAssocTypeUri, parentTypeUri, childTypeUri, parentCardinalityUri,
            childCardinalityUri, null);
    }

    /**
     * @param   customAssocTypeUri      if null no custom association type will be set.
     */
    public AssociationDefinitionModel(long id, String uri, String assocTypeUri, String customAssocTypeUri,
                                                           String parentTypeUri, String childTypeUri,
                                                           String parentCardinalityUri, String childCardinalityUri,
                                                           ViewConfigurationModel viewConfigModel) {
        super(id, uri, assocTypeUri, parentRole(parentTypeUri), childRole(childTypeUri), null,
            childTopics(customAssocTypeUri));
        //
        this.parentCardinalityUri = parentCardinalityUri;
        this.childCardinalityUri = childCardinalityUri;
        //
        this.viewConfigModel = viewConfigModel != null ? viewConfigModel : new ViewConfigurationModel();
    }

    /**
     * @param   assoc   the underlying association.
     *                  IMPORTANT: the association must identify its players <i>by URI</i> (not by ID).
     */
    public AssociationDefinitionModel(AssociationModel assoc, String parentCardinalityUri, String childCardinalityUri,
                                                              ViewConfigurationModel viewConfigModel) {
        super(assoc);
        //
        this.parentCardinalityUri = parentCardinalityUri;
        this.childCardinalityUri = childCardinalityUri;
        //
        this.viewConfigModel = viewConfigModel != null ? viewConfigModel : new ViewConfigurationModel();
    }

    /**
     * Note: the AssociationDefinitionModel constructed by this constructor remains partially uninitialized,
     * which is OK for an update assoc def operation. It can not be used for a create operation.
     */
    public AssociationDefinitionModel(AssociationModel assoc) {
        // ### FIXME: the assoc must identify its players **by URI**
        super(assoc);
    }

    AssociationDefinitionModel(JSONObject assocDef) throws JSONException {
        super(assocDef.optLong("id", -1), null, assocDef.getString("assoc_type_uri"), parentRole(assocDef),
            childRole(assocDef), null, childTopics(assocDef));
        try {
            if (!assocDef.has("parent_cardinality_uri") && !typeUri.equals("dm4.core.composition_def")) {
                throw new RuntimeException("\"parent_cardinality_uri\" is missing");
            }
            this.parentCardinalityUri = assocDef.optString("parent_cardinality_uri", "dm4.core.one");
            this.childCardinalityUri  = assocDef.getString("child_cardinality_uri");
            //
            this.viewConfigModel = new ViewConfigurationModel(assocDef);
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationDefinitionModel failed (JSONObject=" + assocDef + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String getAssocDefUri() {
        String customAssocTypeUri = getCustomAssocTypeUri();
        return getChildTypeUri() + (customAssocTypeUri !=null ? "#" + customAssocTypeUri : "");
    }

    public String getCustomAssocTypeUri() {
        TopicModel typeTopic = getCustomAssocType();
        return typeTopic != null ? typeTopic.getUri() : null;
    }

    public RelatedTopicModel getCustomAssocType() {
        return getChildTopicsModel().getTopic("dm4.core.assoc_type#dm4.core.custom_assoc_type", null);
    }

    /**
     * The type to be used to create an association instance based on this association definition.
     */
    public String getInstanceLevelAssocTypeUri() {
        String customAssocTypeUri = getCustomAssocTypeUri();
        return customAssocTypeUri !=null ? customAssocTypeUri : defaultInstanceLevelAssocTypeUri();
    }

    public String getParentTypeUri() {
        return ((TopicRoleModel) getRoleModel("dm4.core.parent_type")).getTopicUri();
    }

    public String getChildTypeUri() {
        return ((TopicRoleModel) getRoleModel("dm4.core.child_type")).getTopicUri();
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
            JSONObject o = super.toJSON()
                .put("parent_cardinality_uri", parentCardinalityUri)
                .put("child_cardinality_uri", childCardinalityUri);
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

    static void toJSON(Collection<AssociationDefinitionModel> assocDefs, JSONObject o) throws Exception {
        List assocDefList = new ArrayList();
        for (AssociationDefinitionModel assocDef : assocDefs) {
            assocDefList.add(assocDef.toJSON());
        }
        o.put("assoc_defs", assocDefList);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private static TopicRoleModel parentRole(JSONObject assocDef) throws JSONException {
        return parentRole(assocDef.getString("parent_type_uri"));
    }

    private static TopicRoleModel parentRole(String parentTypeUri) {
        return new TopicRoleModel(parentTypeUri, "dm4.core.parent_type");
    }

    // ---

    private static TopicRoleModel childRole(JSONObject assocDef) throws JSONException {
        return childRole(assocDef.getString("child_type_uri"));
    }

    private static TopicRoleModel childRole(String childTypeUri) {
        return new TopicRoleModel(childTypeUri, "dm4.core.child_type");
    }

    // ---

    private static ChildTopicsModel childTopics(JSONObject assocDef) throws JSONException {
        // Note: getString() called on a key with JSON null value would return the string "null"
        return childTopics(assocDef.isNull("custom_assoc_type_uri") ? null :
            assocDef.getString("custom_assoc_type_uri"));
    }

    private static ChildTopicsModel childTopics(String customAssocTypeUri) {
        if (customAssocTypeUri != null) {
            // ### TODO: is putRef() correct here? What about put()?
            // Is the assoc def model being used for create/update or for retrieve?
            return new ChildTopicsModel().put("dm4.core.assoc_type#dm4.core.custom_assoc_type", new RelatedTopicModel(
                new TopicModel(customAssocTypeUri, "dm4.core.assoc_type"),
                new AssociationModel(null,      /* ### TODO */
                    new TopicRoleModel(-1, ""),
                    new TopicRoleModel(-1, ""))
            ));
        } else {
            return null;
        }
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
