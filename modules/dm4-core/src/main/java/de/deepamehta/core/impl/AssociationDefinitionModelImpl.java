package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.TopicDeletionModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.ViewConfigurationModel;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;



/**
 * Definition of an association between 2 topic types -- part of DeepaMehta's type system,
 * like an association in a class diagram. Used to represent both, aggregations and compositions.
 * ### FIXDOC: also assoc types have assoc defs
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
class AssociationDefinitionModelImpl extends AssociationModelImpl implements AssociationDefinitionModel {

    // ------------------------------------------------------------------------------------------------------- Constants

    // ### TODO: copy in ChildTopicsModel
    private static final String DEL_URI_PREFIX = "del_uri:";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String parentCardinalityUri;
    private String childCardinalityUri;

    private ViewConfigurationModel viewConfigModel; // is never null

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * ### TODO: add include-in-label parameter?
     *
     * @param   customAssocTypeUri      if null no custom association type will be set.
     */
    AssociationDefinitionModelImpl(AssociationModel assoc, String parentCardinalityUri, String childCardinalityUri,
                                                                               ViewConfigurationModel viewConfigModel) {
        super(assoc);
        this.parentCardinalityUri = parentCardinalityUri;
        this.childCardinalityUri  = childCardinalityUri;
        this.viewConfigModel = viewConfigModel;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String getAssocDefUri() {
        String customAssocTypeUri = getCustomAssocTypeUriOrNull();
        return getChildTypeUri() + (customAssocTypeUri !=null ? "#" + customAssocTypeUri : "");
    }

    @Override
    public RelatedTopicModel getCustomAssocType() {
        RelatedTopicModel customAssocType = getChildTopicsModel().getTopic(
            "dm4.core.assoc_type#dm4.core.custom_assoc_type", null);
        // Note: we can't do this sanity check because a type model would not even deserialize.
        // The type model JSON constructor repeatedly calls addAssocDef() which hashes by assoc def URI.
        /* if (customAssocType instanceof TopicDeletionModel) {
            throw new RuntimeException("Tried to get an assoc def's custom assoc type when it is a deletion " +
                "reference (" + this + ")");
        } */
        return customAssocType;
    }

    @Override
    public String getCustomAssocTypeUri() {
        TopicModel customAssocType = getCustomAssocType();
        return customAssocType != null ? customAssocType.getUri() : null;
    }

    /**
     * The type to be used to create an association instance based on this association definition.
     */
    @Override
    public String getInstanceLevelAssocTypeUri() {
        String customAssocTypeUri = getCustomAssocTypeUri();
        return customAssocTypeUri !=null ? customAssocTypeUri : defaultInstanceLevelAssocTypeUri();
    }

    @Override
    public String getParentTypeUri() {
        return ((TopicRoleModel) getRoleModel("dm4.core.parent_type")).getTopicUri();
    }

    @Override
    public String getChildTypeUri() {
        return ((TopicRoleModel) getRoleModel("dm4.core.child_type")).getTopicUri();
    }

    @Override
    public String getParentCardinalityUri() {
        return parentCardinalityUri;
    }

    @Override
    public String getChildCardinalityUri() {
        return childCardinalityUri;
    }

    @Override
    public ViewConfigurationModel getViewConfigModel() {
        return viewConfigModel;
    }

    // ---

    @Override
    public void setParentCardinalityUri(String parentCardinalityUri) {
        this.parentCardinalityUri = parentCardinalityUri;
    }

    @Override
    public void setChildCardinalityUri(String childCardinalityUri) {
        this.childCardinalityUri = childCardinalityUri;
    }

    @Override
    public void setViewConfigModel(ViewConfigurationModel viewConfigModel) {
        this.viewConfigModel = viewConfigModel;
    }

    // ---

    // ### TODO: remove from public API
    @Override
    public boolean hasSameCustomAssocType(AssociationDefinitionModel assocDef) {
        String _customAssocTypeUri = getCustomAssocTypeUri();
        String customAssocTypeUri = assocDef.getCustomAssocTypeUriOrNull();
        if (customAssocTypeUri == null) {
            // compare our value to null if his value is a deletion ref or null
            return _customAssocTypeUri == null;
        } else {
            // his value is neither a deletion ref nor null, compare it to our value (which may be null)
            return customAssocTypeUri.equals(_customAssocTypeUri);
        }
    }

    /**
     * @return  <code>null</code> if this assoc def's custom assoc type model is null or represents a deletion ref.
     *          Otherwise returns the custom assoc type URI.
     *
     *  ### TODO: remove from public API
     */
    @Override
    public String getCustomAssocTypeUriOrNull() {
        return getCustomAssocType() instanceof TopicDeletionModel ? null : getCustomAssocTypeUri();
    }

    // ---

    @Override
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
