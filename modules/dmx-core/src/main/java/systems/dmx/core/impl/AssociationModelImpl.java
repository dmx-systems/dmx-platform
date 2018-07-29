package systems.dmx.core.impl;

import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.DMXObjectModel;
import systems.dmx.core.model.IndexMode;
import systems.dmx.core.model.RoleModel;
import systems.dmx.core.model.TopicRoleModel;
import systems.dmx.core.model.TypeModel;
import systems.dmx.core.service.DMXEvent;
import systems.dmx.core.service.Directive;

import org.codehaus.jettison.json.JSONObject;

import java.util.List;



/**
 * Collection of the data that makes up an {@link Association}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
class AssociationModelImpl extends DMXObjectModelImpl implements AssociationModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private RoleModelImpl roleModel1;   // may be null in models used for an update operation
    private RoleModelImpl roleModel2;   // may be null in models used for an update operation

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssociationModelImpl(DMXObjectModelImpl object, RoleModelImpl roleModel1, RoleModelImpl roleModel2) {
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
    public RoleModelImpl getRoleModel(String roleTypeUri) {
        boolean rm1 = roleModel1.getRoleTypeUri().equals(roleTypeUri);
        boolean rm2 = roleModel2.getRoleTypeUri().equals(roleTypeUri);
        if (rm1 && rm2) {
            throw new RuntimeException("Ambiguous getRoleModel() call: both players occupy role \"" + roleTypeUri +
                "\" (" + this + ")");
        }
        return rm1 ? roleModel1 : rm2 ? roleModel2 : null;
    }

    @Override
    public boolean hasSameRoleTypeUris() {
        return roleModel1.getRoleTypeUri().equals(roleModel2.getRoleTypeUri());
    }

    @Override
    public boolean matches(String roleTypeUri1, long playerId1, String roleTypeUri2, long playerId2) {
        if (roleTypeUri1.equals(roleTypeUri2)) {
            throw new IllegalArgumentException("matches() was called with 2 identical role type URIs (\"" +
                roleTypeUri1 + "\")");
        }
        if (!hasSameRoleTypeUris()) {
            RoleModel r1 = getRoleModel(roleTypeUri1);
            RoleModel r2 = getRoleModel(roleTypeUri2);
            if (r1 != null && r1.getPlayerId() == playerId1 &&
                r2 != null && r2.getPlayerId() == playerId2) {
                return true;
            }
        }
        return false;
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
                .put("role1", roleModel1 != null ? roleModel1.toJSON() : null)
                .put("role2", roleModel2 != null ? roleModel2.toJSON() : null);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
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

    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    String className() {
        return "association";
    }

    @Override
    AssociationImpl instantiate() {
        return new AssociationImpl(this, pl);
    }

    @Override
    AssociationModelImpl createModelWithChildTopics(ChildTopicsModel childTopics) {
        return mf.newAssociationModel(typeUri, childTopics);
    }

    // ---

    @Override
    final AssociationTypeModelImpl getType() {
        return pl.typeStorage.getAssociationType(typeUri);
    }

    @Override
    final List<AssociationModelImpl> getAssociations() {
        return pl.fetchAssociationAssociations(id);
    }

    // ---

    @Override
    final RelatedTopicModelImpl getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                           String othersTopicTypeUri) {
        return pl.fetchAssociationRelatedTopic(id, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri);
    }

    @Override
    final List<RelatedTopicModelImpl> getRelatedTopics(String assocTypeUri, String myRoleTypeUri,
                                                                                           String othersRoleTypeUri,
                                                                                           String othersTopicTypeUri) {
        return pl.fetchAssociationRelatedTopics(id, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri);
    }

    @Override
    final List<RelatedTopicModelImpl> getRelatedTopics(List assocTypeUris, String myRoleTypeUri,
                                                                                           String othersRoleTypeUri,
                                                                                           String othersTopicTypeUri) {
        return pl.fetchAssociationRelatedTopics(id, assocTypeUris, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri);
    }

    // ---

    @Override
    final void storeUri() {
        pl.storeAssociationUri(id, uri);
    }

    @Override
    final void storeTypeUri() {
        reassignInstantiation();
        pl.storeAssociationTypeUri(id, typeUri);
    }

    @Override
    final void storeSimpleValue() {
        TypeModel type = getType();
        pl.storeAssociationValue(id, value, type.getIndexModes(), type.getUri(), getIndexValue());
    }

    @Override
    final void indexSimpleValue(IndexMode indexMode) {
        pl.indexAssociationValue(id, indexMode, typeUri, getIndexValue());
    }

    @Override
    final void storeProperty(String propUri, Object propValue, boolean addToIndex) {
        pl.storeAssociationProperty(id, propUri, propValue, addToIndex);
    }

    @Override
    final void removeProperty(String propUri) {
        pl.removeAssociationProperty(id, propUri);
    }

    // ---

    @Override
    final void _delete() {
        pl._deleteAssociation(id);
    }

    // ---

    @Override
    final void checkReadAccess() {
        pl.checkAssociationReadAccess(id);
    }

    @Override
    final void checkWriteAccess() {
        pl.checkAssociationWriteAccess(id);
    }

    // ---

    @Override
    final DMXEvent getPreUpdateEvent() {
        return CoreEvent.PRE_UPDATE_ASSOCIATION;
    }

    @Override
    final DMXEvent getPostUpdateEvent() {
        return CoreEvent.POST_UPDATE_ASSOCIATION;
    }

    @Override
    final DMXEvent getPreDeleteEvent() {
        return CoreEvent.PRE_DELETE_ASSOCIATION;
    }

    @Override
    final DMXEvent getPostDeleteEvent() {
        return CoreEvent.POST_DELETE_ASSOCIATION;
    }

    // ---

    @Override
    final Directive getUpdateDirective() {
        return Directive.UPDATE_ASSOCIATION;
    }

    @Override
    final Directive getDeleteDirective() {
        return Directive.DELETE_ASSOCIATION;
    }



    // === Core Internal Hooks ===

    @Override
    void preCreate() {
        duplicateCheck();
    }

    @Override
    void postUpdate(DMXObjectModel updateModel, DMXObjectModel oldObject) {
        // update association specific parts: the 2 roles
        updateRoles((AssociationModel) updateModel);
        //
        duplicateCheck();
        //
        // Type Editor Support
        if (isAssocDef(this)) {
            if (isAssocDef((AssociationModel) oldObject)) {
                updateAssocDef((AssociationModel) oldObject);
            } else {
                createAssocDef();
            }
        } else if (isAssocDef((AssociationModel) oldObject)) {
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
     * @return  this association's player which plays the given role.
     *          If there is no such player, null is returned.
     *          <p>
     *          If there are 2 such players an exception is thrown.
     */
    DMXObjectModelImpl getPlayer(String roleTypeUri) {
        RoleModelImpl role = getRoleModel(roleTypeUri);
        return role != null ? role.getPlayer(this) : null;
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
        // Compare to DMXUtils.associationAutoTyping()
        if (!(roleModel1 instanceof TopicRoleModel) || ((TopicRoleModel) roleModel1).topicIdentifiedByUri() ||
            !(roleModel2 instanceof TopicRoleModel) || ((TopicRoleModel) roleModel2).topicIdentifiedByUri()) {
            return;
        }
        // Note: only readable assocs (access control) are considered
        for (AssociationModelImpl assoc : pl._getAssociations(typeUri, roleModel1.playerId, roleModel2.playerId,
               roleModel1.roleTypeUri, roleModel2.roleTypeUri)) {
            if (assoc.id != id && assoc.value.equals(value)) {
                throw new RuntimeException("Duplicate: such an association exists already (ID=" + assoc.id +
                    ", typeUri=\"" + typeUri + "\", value=\"" + value + "\")");
            }
        }
    }



    // === Update (memory + DB) ===

    /**
     * @param   updateModel     The data to update.
     *                          If role 1 is <code>null</code> it is not updated.
     *                          If role 2 is <code>null</code> it is not updated.
     */
    private void updateRoles(AssociationModel updateModel) {
        updateRole(updateModel.getRoleModel1(), 1);
        updateRole(updateModel.getRoleModel2(), 2);
    }

    /**
     * @param   nr      used only for logging
     */
    private void updateRole(RoleModel updateModel, int nr) {
        if (updateModel != null) {     // abort if no update is requested
            // Note: We must lookup the roles individually.
            // The role order (getRole1(), getRole2()) is undeterministic and not fix.
            RoleModelImpl role = getRole(updateModel);
            String newRoleTypeUri = updateModel.getRoleTypeUri();   // new value
            String roleTypeUri = role.getRoleTypeUri();             // current value
            if (!roleTypeUri.equals(newRoleTypeUri)) {              // has changed?
                logger.info("### Changing role type " + nr + ": \"" + roleTypeUri + "\" -> \"" + newRoleTypeUri + "\"");
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
        throw new RuntimeException(roleModel + " is not part of " + this);
    }

    // ---

    private TopicModelImpl filterTopic(RoleModelImpl role, String topicTypeUri) {
        if (role instanceof TopicRoleModel) {
            TopicModelImpl topic = ((TopicRoleModelImpl) role).getPlayer(this);
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
    }

    private void updateAssocDef(AssociationModel oldAssoc) {
        TypeModelImpl parentType = fetchParentType();
        logger.info("##### Updating association definition " + id + " of type \"" + parentType.getUri() + "\"");
        //
        parentType._updateAssocDef(this, oldAssoc);
    }

    private void removeAssocDef() {
        TypeModelImpl parentType = fetchParentType();
        logger.info("##### Removing association definition " + id + " from type \"" + parentType.getUri() + "\"");
        //
        parentType._removeAssocDefFromMemoryAndRebuildSequence(this);
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

    private TypeModelImpl fetchParentType() {
        return pl.typeStorage.fetchParentType(this);
    }
}
