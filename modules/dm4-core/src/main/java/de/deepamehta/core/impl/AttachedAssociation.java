package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Role;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicRole;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.ResultList;

import java.util.ArrayList;
import java.util.List;

import java.util.logging.Logger;



/**
 * An association model that is attached to the DB.
 */
class AttachedAssociation extends AttachedDeepaMehtaObject implements Association {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Role role1;     // attached object cache
    private Role role2;     // attached object cache

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedAssociation(AssociationModel model, EmbeddedService dms) {
        super(model, dms);
        // init attached object cache
        this.role1 = createAttachedRole(model.getRoleModel1());
        this.role2 = createAttachedRole(model.getRoleModel2());
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ******************************************
    // *** AttachedDeepaMehtaObject Overrides ***
    // ******************************************



    // === Updating ===

    /**
     * @param   model   The data to update.
     *                  If the type URI is <code>null</code> it is not updated.
     *                  If role 1 is <code>null</code> it is not updated.
     *                  If role 2 is <code>null</code> it is not updated.
     */
    @Override
    public void update(AssociationModel model) {
        // ### TODO: there is no possible POST_UPDATE_ASSOCIATION_REQUEST event to fire here (compare to
        // AttachedTopic update()). It would be equivalent to POST_UPDATE_ASSOCIATION.
        // Per request exactly one association is updated. Its childs are topics (never associations).
        logger.info("Updating association " + getId() + " (new " + model + ")");
        //
        dms.fireEvent(CoreEvent.PRE_UPDATE_ASSOCIATION, this, model);
        //
        AssociationModel oldModel = getModel().clone();
        super.update(model);
        updateRole(model.getRoleModel1(), 1);
        updateRole(model.getRoleModel2(), 2);
        //
        addUpdateDirective();
        //
        dms.fireEvent(CoreEvent.POST_UPDATE_ASSOCIATION, this, oldModel);
    }



    // === Deletion ===

    @Override
    public void delete() {
        try {
            dms.fireEvent(CoreEvent.PRE_DELETE_ASSOCIATION, this);
            //
            // delete sub-topics and associations
            super.delete();
            // delete association itself
            logger.info("Deleting " + this);
            Directives.get().add(Directive.DELETE_ASSOCIATION, this);
            dms.storageDecorator.deleteAssociation(getId());
            //
            dms.fireEvent(CoreEvent.POST_DELETE_ASSOCIATION, this);
        } catch (IllegalStateException e) {
            // Note: getAssociations() might throw IllegalStateException and is no problem.
            // This can happen when this object is an association which is already deleted.
            //
            // Consider this particular situation: let A1 and A2 be associations of this object and let A2 point to A1.
            // If A1 gets deleted first (the association set order is non-deterministic), A2 is implicitely deleted
            // with it (because it is a direct association of A1 as well). Then when the loop comes to A2
            // "IllegalStateException: Node[1327] has been deleted in this tx" is thrown because A2 has been deleted
            // already. (The Node appearing in the exception is the middle node of A2.) If, on the other hand, A2
            // gets deleted first no error would occur.
            //
            // This particular situation exists when e.g. a topicmap is deleted while one of its mapcontext
            // associations is also a part of the topicmap itself. This originates e.g. when the user reveals
            // a topicmap's mapcontext association and then deletes the topicmap.
            //
            if (e.getMessage().equals("Node[" + getId() + "] has been deleted in this tx")) {
                logger.info("### Association " + getId() + " has already been deleted in this transaction. This can " +
                    "happen while deleting a topic with associations A1 and A2 while A2 points to A1 (" + this + ")");
            } else {
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("Deleting association failed (" + this + ")", e);
        }
    }



    // **********************************
    // *** Association Implementation ***
    // **********************************



    @Override
    public Role getRole1() {
        return role1;
    }

    @Override
    public Role getRole2() {
        return role2;
    }

    // ---

    @Override
    public DeepaMehtaObject getPlayer1() {
        return getRole1().getPlayer();
    }

    @Override
    public DeepaMehtaObject getPlayer2() {
        return getRole2().getPlayer();
    }

    // ---

    @Override
    public Topic getTopic(String roleTypeUri) {
        List<Topic> topics = getTopics(roleTypeUri);
        switch (topics.size()) {
        case 0:
            return null;
        case 1:
            return topics.get(0);
        default:
            throw new RuntimeException("Ambiguity in association: " + topics.size() + " topics have role type \"" +
                roleTypeUri + "\" (" + this + ")");
        }
    }

    @Override
    public List<Topic> getTopics(String roleTypeUri) {
        List<Topic> topics = new ArrayList();
        filterTopic(getRole1(), roleTypeUri, topics);
        filterTopic(getRole2(), roleTypeUri, topics);
        return topics;
    }

    // ---

    @Override
    public Role getRole(RoleModel roleModel) {
        if (getRole1().getModel().refsSameObject(roleModel)) {
            return getRole1();
        } else if (getRole2().getModel().refsSameObject(roleModel)) {
            return getRole2();
        }
        throw new RuntimeException("Role is not part of association (role=" + roleModel + ", association=" + this);
    }

    @Override
    public boolean isPlayer(TopicRoleModel roleModel) {
        List<Topic> topics = getTopics(roleModel.getRoleTypeUri());
        return topics.size() > 0 && topics.get(0).getId() == roleModel.getPlayerId();
    }

    // ---

    @Override
    public AssociationModel getModel() {
        return (AssociationModel) super.getModel();
    }



    // === Traversal ===

    @Override
    public RelatedAssociation getRelatedAssociation(String assocTypeUri, String myRoleTypeUri,
                                                                         String othersRoleTypeUri) {
        RelatedAssociationModel assoc = dms.storageDecorator.fetchAssociationRelatedAssociation(getId(),
            assocTypeUri, myRoleTypeUri, othersRoleTypeUri, null); // othersAssocTypeUri=null
        return assoc != null ? dms.instantiateRelatedAssociation(assoc) : null; 
    }



    // ***************************************
    // *** DeepaMehtaObject Implementation ***
    // ***************************************



    // === Traversal ===

    // --- Topic Retrieval ---

    @Override
    public ResultList<RelatedTopic> getRelatedTopics(List assocTypeUris, String myRoleTypeUri, String othersRoleTypeUri,
                                                     String othersTopicTypeUri, int maxResultSize) {
        ResultList<RelatedTopicModel> topics = dms.storageDecorator.fetchAssociationRelatedTopics(getId(),
            assocTypeUris, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri, maxResultSize);
        return dms.instantiateRelatedTopics(topics);
    }

    // --- Association Retrieval ---

    @Override
    public Association getAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                   long othersTopicId) {
        AssociationModel assoc = dms.storageDecorator.fetchAssociationBetweenTopicAndAssociation(assocTypeUri,
            othersTopicId, getId(), othersRoleTypeUri, myRoleTypeUri);
        return assoc != null ? dms.instantiateAssociation(assoc) : null;
    }

    @Override
    public List<Association> getAssociations() {
        return dms.instantiateAssociations(dms.storageDecorator.fetchAssociationAssociations(getId()));
    }



    // === Properties ===

    @Override
    public Object getProperty(String propUri) {
        return dms.storageDecorator.fetchAssociationProperty(getId(), propUri);
    }

    @Override
    public void setProperty(String propUri, Object propValue, boolean addToIndex) {
        dms.storageDecorator.storeAssociationProperty(getId(), propUri, propValue, addToIndex);
    }

    @Override
    public boolean hasProperty(String propUri) {
        return dms.storageDecorator.hasAssociationProperty(getId(), propUri);
    }

    @Override
    public void removeProperty(String propUri) {
        dms.storageDecorator.removeAssociationProperty(getId(), propUri);
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * Convenience method.
     */
    AssociationType getAssociationType() {
        return (AssociationType) getType();
    }



    // === Implementation of the abstract methods ===

    @Override
    final String className() {
        return "association";
    }

    @Override
    void addUpdateDirective() {
        Directives.get().add(Directive.UPDATE_ASSOCIATION, this);
    }

    @Override
    final void storeUri() {
        dms.storageDecorator.storeAssociationUri(getId(), getUri());
    }

    @Override
    final void storeTypeUri() {
        reassignInstantiation();
        dms.storageDecorator.storeAssociationTypeUri(getId(), getTypeUri());
    }

    // ---

    @Override
    final RelatedTopicModel fetchRelatedTopic(String assocTypeUri, String myRoleTypeUri,
                                              String othersRoleTypeUri, String othersTopicTypeUri) {
        return dms.storageDecorator.fetchAssociationRelatedTopic(getId(), assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
    }

    @Override
    final ResultList<RelatedTopicModel> fetchRelatedTopics(String assocTypeUri, String myRoleTypeUri,
                                                          String othersRoleTypeUri, String othersTopicTypeUri,
                                                          int maxResultSize) {
        return dms.storageDecorator.fetchAssociationRelatedTopics(getId(), assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri, maxResultSize);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    // --- Update ---

    /**
     * @param   nr      used only for logging
     */
    private void updateRole(RoleModel newModel, int nr) {
        if (newModel != null) {
            // Note: We must lookup the roles individually.
            // The role order (getRole1(), getRole2()) is undeterministic and not fix.
            Role role = getRole(newModel);
            String newRoleTypeUri = newModel.getRoleTypeUri();  // new value
            String roleTypeUri = role.getRoleTypeUri();         // current value
            if (!roleTypeUri.equals(newRoleTypeUri)) {          // has changed?
                logger.info("### Changing role type " + nr + " from \"" + roleTypeUri + "\" -> \"" + newRoleTypeUri +
                    "\"");
                role.setRoleTypeUri(newRoleTypeUri);
            }
        }
    }

    // --- Helper ---

    private Role createAttachedRole(RoleModel model) {
        if (model instanceof TopicRoleModel) {
            return new AttachedTopicRole((TopicRoleModel) model, this, dms);
        } else if (model instanceof AssociationRoleModel) {
            return new AttachedAssociationRole((AssociationRoleModel) model, this, dms);
        } else {
            throw new RuntimeException("Unexpected RoleModel object (" + model + ")");
        }
    }

    private void filterTopic(Role role, String roleTypeUri, List<Topic> topics) {
        if (role instanceof TopicRole && role.getRoleTypeUri().equals(roleTypeUri)) {
            topics.add(((TopicRole) role).getTopic());
        }
    }

    // ---

    private void reassignInstantiation() {
        // remove current assignment
        fetchInstantiation().delete();
        // create new assignment
        dms.createAssociationInstantiation(getId(), getTypeUri());
    }

    private Association fetchInstantiation() {
        RelatedTopic assocType = getRelatedTopic("dm4.core.instantiation", "dm4.core.instance", "dm4.core.type",
            "dm4.core.assoc_type");
        //
        if (assocType == null) {
            throw new RuntimeException("Association " + getId() + " is not associated to an association type");
        }
        //
        return assocType.getRelatingAssociation();
    }
}
