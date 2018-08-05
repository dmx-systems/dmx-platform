package systems.dmx.core.impl;

import systems.dmx.core.model.AssociationDefinitionModel;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.DMXObjectModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicRoleModel;
import systems.dmx.core.model.ViewConfigurationModel;

import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;



class AssociationDefinitionModelImpl extends AssociationModelImpl implements AssociationDefinitionModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String childCardinalityUri;

    private ViewConfigurationModelImpl viewConfig;     // is never null

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Remains partially uninitialzed. Only usable as an update-model (not as a create-model).
     */
    AssociationDefinitionModelImpl(AssociationModelImpl assoc) {
        this(assoc, null, null);
    }

    /**
     * @param   assoc   the underlying association.
     */
    AssociationDefinitionModelImpl(AssociationModelImpl assoc, String childCardinalityUri,
                                                               ViewConfigurationModelImpl viewConfig) {
        super(assoc);
        this.childCardinalityUri  = childCardinalityUri;
        this.viewConfig = viewConfig != null ? viewConfig : mf.newViewConfigurationModel();
        // ### TODO: why null check? Compare to TypeModelImpl constructor -> see previous constructor
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String getAssocDefUri() {
        String customAssocTypeUri = getCustomAssocTypeUri();
        return getChildTypeUri() + (customAssocTypeUri !=null ? "#" + customAssocTypeUri : "");
    }

    @Override
    public String getCustomAssocTypeUri() {
        TopicModel customAssocType = getCustomAssocType();
        return customAssocType != null ? customAssocType.getUri() : null;
    }

    @Override
    public String getInstanceLevelAssocTypeUri() {
        String customAssocTypeUri = getCustomAssocTypeUri();
        return customAssocTypeUri !=null ? customAssocTypeUri : defaultInstanceLevelAssocTypeUri();
    }

    @Override
    public String getParentTypeUri() {
        return ((TopicRoleModel) getRoleModel("dmx.core.parent_type")).getTopicUri();
    }

    @Override
    public String getChildTypeUri() {
        return ((TopicRoleModel) getRoleModel("dmx.core.child_type")).getTopicUri();
    }

    @Override
    public String getChildCardinalityUri() {
        return childCardinalityUri;
    }

    @Override
    public ViewConfigurationModelImpl getViewConfig() {
        return viewConfig;
    }

    // ---

    @Override
    public void setChildCardinalityUri(String childCardinalityUri) {
        this.childCardinalityUri = childCardinalityUri;
    }

    @Override
    public void setViewConfig(ViewConfigurationModel viewConfig) {
        this.viewConfig = (ViewConfigurationModelImpl) viewConfig;
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            return super.toJSON()
                .put("childCardinalityUri", childCardinalityUri)
                .put("viewConfigTopics", viewConfig.toJSONArray());
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    @Override
    String className() {
        return "association definition";
    }

    @Override
    AssociationDefinitionImpl instantiate() {
        return new AssociationDefinitionImpl(this, pl);
    }

    @Override
    AssociationDefinitionModelImpl createModelWithChildTopics(ChildTopicsModel childTopics) {
        return mf.newAssociationDefinitionModel(childTopics);
    }



    // === Core Internal Hooks ===

    /**
     * 2 assoc def specific tasks must be performed once the underlying association is updated:
     *   - Update the assoc def's cardinality (in type cache + DB). Cardinality is technically not part of the type
     *     model. So, it is not handled by the generic (model-driven) object update procedure.
     *   - Possibly rehash the assoc def in type cache. Rehashing is required if the custom assoc type has changed.
     * <p>
     * Pre condition: these 3 assoc def parts are already up-to-date through the generic (model-driven) object update
     * procedure:
     *   - Assoc Def type (type URI).
     *   - Custom assoc type (child topics).
     *   - "Include in Label" flag (child topics).
     * <p>
     * Called when update() is called on an AssociationDefinitionModel object. This is in 2 cases:
     *   - Edit a type interactively (a type topic is selected).
     *   - Programmatically call getChildTopics().set() on an AssociationDefinitionModel object, e.g. from a migration.
     * <p>
     * <i>Not</i> called when an association which also acts as an assoc def is edited interactively (an association is
     * selected). In this case:
     *   - Cardinality doesn't need to be updated as Cardinality can't be edited interactively through an association.
     *   - Rehashing is already performed in TypeModelImpl#_updateAssocDef (called from AssociationModelImpl#postUpdate)
     *
     * @param   updateModel
     *              the update data/instructions.
     *              Note: on post-update time updateModel and this (assoc def) model may differ at least because
     *                a) updateModel might contain only certain assoc def parts; this is called a "partial update"
     *                b) updateModel might contain refs and deletion-refs; this model never contains refs
     */
    @Override
    void postUpdate(DMXObjectModel updateModel, DMXObjectModel oldObject) {
        super.postUpdate(updateModel, oldObject);
        //
        updateCardinality((AssociationDefinitionModel) updateModel);
    }



    // === Update (memory + DB) ===

    void updateChildCardinalityUri(String childCardinalityUri) {
        setChildCardinalityUri(childCardinalityUri);                        // update memory
        pl.typeStorage.storeChildCardinalityUri(id, childCardinalityUri);   // update DB
    }



    // === Identity Configuration ===

    final boolean isIdentityAttr() {
        TopicModel isIdentityAttr = getChildTopicsModel().getTopicOrNull("dmx.core.identity_attr");
        if (isIdentityAttr == null) {
            // ### TODO: should a isIdentityAttr topic always exist?
            // throw new RuntimeException("Assoc def \"" + getAssocDefUri() + "\" has no \"Identity Attribute\" topic");
            return false;
        }
        return isIdentityAttr.getSimpleValue().booleanValue();
    }



    // === Label Configuration ===

    final boolean includeInLabel() {
        TopicModel includeInLabel = getChildTopicsModel().getTopicOrNull("dmx.core.include_in_label");
        if (includeInLabel == null) {
            // ### TODO: should a includeInLabel topic always exist?
            // throw new RuntimeException("Assoc def \"" + getAssocDefUri() + "\" has no \"Include in Label\" topic");
            return false;
        }
        return includeInLabel.getSimpleValue().booleanValue();
    }



    // === Access Control ===

    boolean isReadable() {
        try {
            // 1) check assoc def
            if (!pl.hasReadAccess(this)) {
                logger.info("### Assoc def \"" + getAssocDefUri() + "\" not READable");
                return false;
            }
            // Note: there is no need to explicitly check READability for the assoc def's child type.
            // If the child type is not READable the entire assoc def is not READable as well.
            //
            // 2) check custom assoc type, if set
            TopicModelImpl assocType = getCustomAssocType();
            if (assocType != null && !pl.hasReadAccess(assocType)) {
                logger.info("### Assoc def \"" + getAssocDefUri() + "\" not READable (custom assoc type not READable)");
                return false;
            }
            //
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Checking assoc def READability failed (" + this + ")", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Update ===

    private void updateCardinality(AssociationDefinitionModel newAssocDef) {
        updateChildCardinality(newAssocDef.getChildCardinalityUri());
    }

    private void updateChildCardinality(String newChildCardinalityUri) {
        // abort if no update is requested
        if (newChildCardinalityUri == null) {
            return;
        }
        //
        String childCardinalityUri = getChildCardinalityUri();
        if (!childCardinalityUri.equals(newChildCardinalityUri)) {
            logger.info("### Changing child cardinality URI: \"" + childCardinalityUri + "\" -> \"" +
                newChildCardinalityUri + "\"");
            updateChildCardinalityUri(newChildCardinalityUri);
        }
    }



    // ===

    private TopicModelImpl getCustomAssocType() {
        return getChildTopicsModel().getTopicOrNull("dmx.core.assoc_type#dmx.core.custom_assoc_type");
    }

    private String defaultInstanceLevelAssocTypeUri() {
        if (typeUri.equals("dmx.core.composition_def")) {
            return "dmx.core.composition";
        } else {
            throw new RuntimeException("Unexpected association type URI: \"" + typeUri + "\"");
        }
    }
}
