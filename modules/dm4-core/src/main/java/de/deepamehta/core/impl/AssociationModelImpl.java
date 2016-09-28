package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.service.DeepaMehtaEvent;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;

import org.codehaus.jettison.json.JSONObject;

import java.util.List;



/**
 * Collection of the data that makes up an {@link Association}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
class AssociationModelImpl extends DeepaMehtaObjectModelImpl implements AssociationModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private RoleModelImpl roleModel1;   // may be null in models used for an update operation
    private RoleModelImpl roleModel2;   // may be null in models used for an update operation

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssociationModelImpl(DeepaMehtaObjectModelImpl object, RoleModelImpl roleModel1, RoleModelImpl roleModel2) {
        super(object);
        this.roleModel1 = roleModel1;
        this.roleModel2 = roleModel2;
    }

    AssociationModelImpl(AssociationModelImpl assoc) {
        super(assoc);
        this.roleModel1 = assoc.roleModel1;
        this.roleModel2 = assoc.roleModel2;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public RoleModelImpl getRoleModel1() {
        return roleModel1;
    }

    @Override
    public RoleModelImpl getRoleModel2() {
        return roleModel2;
    }

    // ---

    @Override
    public void setRoleModel1(RoleModel roleModel1) {
        this.roleModel1 = (RoleModelImpl) roleModel1;
    }

    @Override
    public void setRoleModel2(RoleModel roleModel2) {
        this.roleModel2 = (RoleModelImpl) roleModel2;
    }

    // --- Convenience Methods ---

    @Override
    public RoleModel getRoleModel(String roleTypeUri) {
        boolean rm1 = roleModel1.getRoleTypeUri().equals(roleTypeUri);
        boolean rm2 = roleModel2.getRoleTypeUri().equals(roleTypeUri);
        if (rm1 && rm2) {
            throw new RuntimeException("Ambiguous getRoleModel() call: both players occupy role \"" + roleTypeUri +
                "\" (" + this + ")");
        }
        return rm1 ? roleModel1 : rm2 ? roleModel2 : null;
    }

    @Override
    public long getOtherPlayerId(long id) {
        long id1 = roleModel1.getPlayerId();
        long id2 = roleModel2.getPlayerId();
        if (id1 == id) {
            return id2;
        } else if (id2 == id) {
            return id1;
        } else {
            throw new IllegalArgumentException("ID " + id + " doesn't refer to a player in " + this);
        }
    }

    @Override
    public boolean hasSameRoleTypeUris() {
        return roleModel1.getRoleTypeUri().equals(roleModel2.getRoleTypeUri());
    }



    // === Implementation of the abstract methods ===

    @Override
    public RoleModel createRoleModel(String roleTypeUri) {
        return mf.newAssociationRoleModel(id, roleTypeUri);
    }



    // === Serialization ===

    @Override
    public JSONObject toJSON() {
        try {
            return super.toJSON()
                .put("role_1", roleModel1.toJSON())
                .put("role_2", roleModel2.toJSON());
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }



    // === Java API ===

    @Override
    public AssociationModel clone() {
        try {
            AssociationModel model = (AssociationModel) super.clone();
            model.setRoleModel1(roleModel1.clone());
            model.setRoleModel2(roleModel2.clone());
            return model;
        } catch (Exception e) {
            throw new RuntimeException("Cloning an AssociationModel failed", e);
        }
    }

    @Override
    public String toString() {
        return "association (" + super.toString() + ", " + roleModel1 + ", " + roleModel2 + ")";
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    String className() {
        return "association";
    }

    @Override
    AssociationImpl instantiate() {
        return new AssociationImpl(this, pl);
    }

    // ---

    @Override
    AssociationTypeModel getType() {
        return pl.typeStorage.getAssociationType(typeUri);
    }

    @Override
    List<AssociationModelImpl> getAssociations() {
        return pl.fetchAssociationAssociations(id);
    }

    // ---

    @Override
    RelatedTopicModelImpl getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                     String othersTopicTypeUri) {
        return pl.fetchAssociationRelatedTopic(id, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri);
    }

    @Override
    List<RelatedTopicModelImpl> getRelatedTopics(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                            String othersTopicTypeUri) {
        return pl.fetchAssociationRelatedTopics(id, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri);
    }

    @Override
    List<RelatedTopicModelImpl> getRelatedTopics(List assocTypeUris, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                           String othersTopicTypeUri) {
        return pl.fetchAssociationRelatedTopics(id, assocTypeUris, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri);
    }

    // ---

    @Override
    void storeUri() {
        pl.storeAssociationUri(id, uri);
    }

    @Override
    void storeTypeUri() {
        reassignInstantiation();
        pl.storeAssociationTypeUri(id, typeUri);
    }

    @Override
    void storeSimpleValue() {
        TypeModel type = getType();
        pl.storeAssociationValue(id, value, type.getIndexModes(), type.getUri(), getIndexValue());
    }

    @Override
    void indexSimpleValue(IndexMode indexMode) {
        pl.indexAssociationValue(id, indexMode, typeUri, getIndexValue());
    }

    // ---

    @Override
    void updateChildTopics(ChildTopicsModel childTopics) {
        update(mf.newAssociationModel(childTopics));
    }

    @Override
    void _delete() {
        pl._deleteAssociation(id);
    }

    // ---

    @Override
    DeepaMehtaEvent getReadAccessEvent() {
        return CoreEvent.CHECK_ASSOCIATION_READ_ACCESS;
    }

    @Override
    DeepaMehtaEvent getPreUpdateEvent() {
        return CoreEvent.PRE_UPDATE_ASSOCIATION;
    }

    @Override
    DeepaMehtaEvent getPostUpdateEvent() {
        return CoreEvent.POST_UPDATE_ASSOCIATION;
    }

    @Override
    DeepaMehtaEvent getPreDeleteEvent() {
        return CoreEvent.PRE_DELETE_ASSOCIATION;
    }

    @Override
    DeepaMehtaEvent getPostDeleteEvent() {
        return CoreEvent.POST_DELETE_ASSOCIATION;
    }

    // ---

    @Override
    Directive getUpdateDirective() {
        return Directive.UPDATE_ASSOCIATION;
    }

    @Override
    Directive getDeleteDirective() {
        return Directive.DELETE_ASSOCIATION;
    }



    // === Core Internal Hooks ===

    @Override
    void preCreate() {
        duplicateCheck();
    }

    @Override
    void postUpdate(DeepaMehtaObjectModel newModel, DeepaMehtaObjectModel oldModel) {
        // update association specific parts: the 2 roles
        updateRoles((AssociationModel) newModel);
        //
        duplicateCheck();
        //
        // Type Editor Support
        if (isAssocDef(this)) {
            if (isAssocDef((AssociationModel) oldModel)) {
                updateAssocDef();
            } else {
                createAssocDef();
            }
        } else if (isAssocDef((AssociationModel) oldModel)) {
            removeAssocDef();
        }
    }

    @Override
    void preDelete() {
        // Type Editor Support
        if (isAssocDef(this)) {
            // Note: we listen to the PRE event here, not the POST event. At POST time the assocdef sequence might be
            // interrupted, which would result in a corrupted sequence once rebuild. (Due to the interruption, while
            // rebuilding not all segments would be catched for deletion and recreated redundantly -> ambiguity.)
            // ### FIXDOC
            removeAssocDef();
        }
    }



    // ===

    /**
     * @teturn  this association's topic which plays the given role.
     *          If there is no such topic, null is returned.
     *          <p>
     *          If there are 2 such topics an exception is thrown.
     */
    TopicModelImpl getTopic(String roleTypeUri) {
        RoleModel role = getRoleModel(roleTypeUri);
        return role instanceof TopicRoleModel ? ((TopicRoleModelImpl) role).getPlayer() : null;
    }

    TopicModelImpl getTopicByType(String topicTypeUri) {
        TopicModelImpl topic1 = filterTopic(roleModel1, topicTypeUri);
        TopicModelImpl topic2 = filterTopic(roleModel2, topicTypeUri);
        if (topic1 != null && topic2 != null) {
            throw new RuntimeException("Ambiguous getTopicByType() call: both topics are of type \"" + topicTypeUri +
                "\" (" + this + ")");
        }
        return topic1 != null ? topic1 : topic2 != null ? topic2 : null;
    }

    // ---

    void updateRoleTypeUri(RoleModelImpl role, String roleTypeUri) {
        role.setRoleTypeUri(roleTypeUri);                           // update memory
        pl.storeRoleTypeUri(id, role.playerId, role.roleTypeUri);   // update DB
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    private void duplicateCheck() {
        // ### FIXME: the duplicate check is supported only for topic players, and if they are identified by-ID.
        // Note: we can't call roleModel.getPlayer() as this would build an entire object model, but its "value"
        // is not yet available in case this association is part of the player's composite structure.
        // Compare to DeepaMehtaUtils.associationAutoTyping()
        if (!(roleModel1 instanceof TopicRoleModel) || ((TopicRoleModel) roleModel1).topicIdentifiedByUri() ||
            !(roleModel2 instanceof TopicRoleModel) || ((TopicRoleModel) roleModel2).topicIdentifiedByUri()) {
            return;
        }
        // ### FIXME: respect only readable assocs (access control)?
        for (AssociationModelImpl assoc : pl.fetchAssociations(typeUri, roleModel1.playerId, roleModel2.playerId,
               roleModel1.roleTypeUri, roleModel2.roleTypeUri)) {
            if (assoc.id != id) {
                throw new RuntimeException("Duplicate: such an association exists already (ID=" + assoc.id +
                    ", typeUri=\"" + typeUri + "\")");
            }
        }
    }



    // === Update (memory + DB) ===

    /**
     * @param   newModel    The data to update.
     *                      If role 1 is <code>null</code> it is not updated.
     *                      If role 2 is <code>null</code> it is not updated.
     */
    private void updateRoles(AssociationModel newModel) {
        updateRole(newModel.getRoleModel1(), 1);
        updateRole(newModel.getRoleModel2(), 2);
    }

    /**
     * @param   nr      used only for logging
     */
    private void updateRole(RoleModel newModel, int nr) {
        if (newModel != null) {     // abort if no update is requested
            // Note: We must lookup the roles individually.
            // The role order (getRole1(), getRole2()) is undeterministic and not fix.
            RoleModelImpl role = getRole(newModel);
            String newRoleTypeUri = newModel.getRoleTypeUri();  // new value
            String roleTypeUri = role.getRoleTypeUri();         // current value
            if (!roleTypeUri.equals(newRoleTypeUri)) {          // has changed?
                logger.info("### Changing role type " + nr + " from \"" + roleTypeUri + "\" -> \"" + newRoleTypeUri +
                    "\"");
                updateRoleTypeUri(role, newRoleTypeUri);
            }
        }
    }



    // === Roles (memory access) ===

    /**
     * Returns this association's role which refers to the same object as the given role model.
     * The role returned is found by comparing topic IDs, topic URIs, or association IDs.
     * The role types are <i>not</i> compared.
     * <p>
     * If the object refered by the given role model is not a player in this association an exception is thrown.
     */
    private RoleModelImpl getRole(RoleModel roleModel) {
        if (roleModel1.refsSameObject(roleModel)) {
            return roleModel1;
        } else if (roleModel2.refsSameObject(roleModel)) {
            return roleModel2;
        }
        throw new RuntimeException("Role is not part of association (role=" + roleModel + ", association=" + this);
    }

    // ---

    private TopicModelImpl filterTopic(RoleModelImpl role, String topicTypeUri) {
        if (role instanceof TopicRoleModel) {
            TopicModelImpl topic = ((TopicRoleModelImpl) role).getPlayer();
            if (topic.getTypeUri().equals(topicTypeUri)) {
                return topic;
            }
        }
        return null;
    }



    // === Store (DB only) ===

    private void reassignInstantiation() {
        // remove current assignment
        fetchInstantiation().delete();
        // create new assignment
        pl.createAssociationInstantiation(id, typeUri);
    }

    private AssociationModelImpl fetchInstantiation() {
        RelatedTopicModelImpl assocType = getRelatedTopic("dm4.core.instantiation", "dm4.core.instance",
            "dm4.core.type", "dm4.core.assoc_type");
        //
        if (assocType == null) {
            throw new RuntimeException("Association " + id + " is not associated to an association type");
        }
        //
        return assocType.getRelatingAssociation();
    }



    // === Type Editor Support ===

    // ### TODO: explain

    private void createAssocDef() {
        TypeModelImpl parentType = fetchParentType();
        logger.info("##### Adding association definition " + id + " to type \"" + parentType.getUri() + "\"");
        //
        parentType._addAssocDef(this);
        //
        addUpdateTypeDirective(parentType);
    }

    private void updateAssocDef() {
        TypeModelImpl parentType = fetchParentType();
        logger.info("##### Updating association definition " + id + " of type \"" + parentType.getUri() + "\"");
        //
        parentType._updateAssocDef(this);
        //
        addUpdateTypeDirective(parentType);
    }

    private void removeAssocDef() {
        TypeModelImpl parentType = fetchParentType();
        logger.info("##### Removing association definition " + id + " from type \"" + parentType.getUri() + "\"");
        //
        parentType._removeAssocDefFromMemoryAndRebuildSequence(this);
        //
        addUpdateTypeDirective(parentType);
    }

    // ---

    private boolean isAssocDef(AssociationModel assoc) {
        String typeUri = assoc.getTypeUri();
        if (!typeUri.equals("dm4.core.aggregation_def") &&
            !typeUri.equals("dm4.core.composition_def")) {
            return false;
        }
        //
        if (assoc.hasSameRoleTypeUris()) {
            return false;
        }
        if (assoc.getRoleModel("dm4.core.parent_type") == null ||
            assoc.getRoleModel("dm4.core.child_type") == null)  {
            return false;
        }
        //
        return true;
    }

    // ### TODO: adding the UPDATE directive should be the responsibility of a type. The DeepaMehtaType interface's
    // ### addAssocDef(), updateAssocDef(), and removeAssocDef() methods should have a "directives" parameter ### FIXDOC
    private void addUpdateTypeDirective(TypeModelImpl type) {
        if (type.getTypeUri().equals("dm4.core.topic_type")) {
            Directives.get().add(Directive.UPDATE_TOPIC_TYPE, type.instantiate());
        } else if (type.getTypeUri().equals("dm4.core.assoc_type")) {
            Directives.get().add(Directive.UPDATE_ASSOCIATION_TYPE, type.instantiate());
        }
        // Note: no else here as error check already performed in fetchParentType()
    }

    private TypeModelImpl fetchParentType() {
        return pl.typeStorage.fetchParentType(this);
    }
}
