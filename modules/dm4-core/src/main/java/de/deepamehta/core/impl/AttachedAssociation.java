package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Role;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicRole;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.logging.Logger;



/**
 * An association that is attached to the {@link DeepaMehtaService}.
 */
class AttachedAssociation extends AttachedDeepaMehtaObject implements Association {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Role role1;     // Attached object cache
    private Role role2;     // Attached object cache

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



    // === Deletion ===

    @Override
    public void delete(Directives directives) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            // delete sub-topics and associations
            super.delete(directives);
            // delete association itself
            logger.info("Deleting " + this);
            directives.add(Directive.DELETE_ASSOCIATION, this);
            dms.storage.deleteAssociation(getId());
            //
            tx.success();
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
            if (e.getMessage().matches("Node\\[\\d+\\] has been deleted in this tx")) {
                logger.info("### Association has already been deleted in this transaction. This can happen while " +
                    "deleting a topic with direct associations A1 and A2 while A2 points to A1 (" + this + ")");
                tx.success();
            } else {
                throw e;
            }
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Deleting association failed (" + this + ")", e);
        } finally {
            tx.finish();
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
    public Role getRole(RoleModel model) {
        if (getRole1().getModel().refsSameObject(model)) {
            return getRole1();
        } else if (getRole2().getModel().refsSameObject(model)) {
            return getRole2();
        }
        throw new RuntimeException("Role is not part of association (role=" + model + ", association=" + this);
    }

    // ---

    public AssociationModel getModel() {
        return (AssociationModel) super.getModel();
    }



    // === Updating ===

    /**
     * @param   model   The data to update.
     *                  If the type URI is <code>null</code> it is not updated.
     *                  If role 1 is <code>null</code> it is not updated.
     *                  If role 2 is <code>null</code> it is not updated.
     */
    @Override
    public void update(AssociationModel model, ClientState clientState, Directives directives) {
        logger.info("Updating association " + getId() + " (new " + model + ")");
        //
        dms.fireEvent(CoreEvent.PRE_UPDATE_ASSOCIATION, this, model, directives);
        //
        AssociationModel oldModel = getModel().clone();
        super.update(model, clientState, directives);
        updateRole(model.getRoleModel1(), 1);
        updateRole(model.getRoleModel2(), 2);
        //
        directives.add(Directive.UPDATE_ASSOCIATION, this);
        //
        dms.fireEvent(CoreEvent.POST_UPDATE_ASSOCIATION, this, oldModel, clientState, directives);
    }



    // === Traversal ===

    @Override
    public Topic getTopic(String roleTypeUri) {
        Set<Topic> topics = getTopics(roleTypeUri);
        switch (topics.size()) {
        case 0:
            return null;
        case 1:
            return topics.iterator().next();
        default:
            throw new RuntimeException("Ambiguity in association: " + topics.size() + " topics have role type \"" +
                roleTypeUri + "\" (" + this + ")");
        }
    }

    @Override
    public Set<Topic> getTopics(String roleTypeUri) {
        Set<Topic> topics = new HashSet();
        filterTopic(getRole1(), roleTypeUri, topics);
        filterTopic(getRole2(), roleTypeUri, topics);
        return topics;
    }

    // ---

    @Override
    public RelatedAssociation getRelatedAssociation(String assocTypeUri, String myRoleTypeUri,
                                                                         String othersRoleTypeUri) {
        RelatedAssociationModel assoc = dms.storage.fetchAssociationRelatedAssociation(getId(),
            assocTypeUri, myRoleTypeUri, othersRoleTypeUri, null); // othersAssocTypeUri=null
        return assoc != null ? dms.instantiateRelatedAssociation(assoc, false, false) : null; 
                                                                   // fetchComposite=false, fetchRelatingComposite=false
    }



    // ***************************************
    // *** DeepaMehtaObject Implementation ***
    // ***************************************



    // === Traversal ===

    // --- Topic Retrieval ---

    @Override
    public ResultSet<RelatedTopic> getRelatedTopics(List assocTypeUris, String myRoleTypeUri, String othersRoleTypeUri,
                                    String othersTopicTypeUri, boolean fetchComposite, boolean fetchRelatingComposite,
                                    int maxResultSize) {
        ResultSet<RelatedTopicModel> topics = dms.storage.fetchAssociationRelatedTopics(getId(), assocTypeUris,
            myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri, maxResultSize);
        return dms.instantiateRelatedTopics(topics, fetchComposite, fetchRelatingComposite);
    }

    // --- Association Retrieval ---

    @Override
    public Association getAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                   long othersTopicId) {
        AssociationModel assoc = dms.storage.fetchAssociationBetweenTopicAndAssociation(assocTypeUri,
            othersTopicId, getId(), othersRoleTypeUri, myRoleTypeUri);
        return assoc != null ? dms.instantiateAssociation(assoc, false) : null;     // fetchComposite=false
    }

    @Override
    public Set<Association> getAssociations() {
        return dms.instantiateAssociations(dms.storage.fetchAssociationAssociations(getId()), false);
                                                                                    // fetchComposite=false
    }



    // === Properties ===

    @Override
    public Object getProperty(String propUri) {
        return dms.storage.fetchAssociationProperty(getId(), propUri);
    }

    @Override
    public void setProperty(String propUri, Object propValue, boolean addToIndex) {
        dms.storage.storeAssociationProperty(getId(), propUri, propValue, addToIndex);
    }

    @Override
    public boolean hasProperty(String propUri) {
        return dms.storage.hasAssociationProperty(getId(), propUri);
    }

    @Override
    public void removeProperty(String propUri) {
        dms.storage.removeAssociationProperty(getId(), propUri);
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
    final void storeUri() {
        dms.storage.storeAssociationUri(getId(), getUri());
    }

    @Override
    final void storeTypeUri() {
        reassignInstantiation();
        dms.storage.storeAssociationTypeUri(getId(), getTypeUri());
    }

    // ---

    @Override
    final RelatedTopicModel fetchRelatedTopic(String assocTypeUri, String myRoleTypeUri,
                                              String othersRoleTypeUri, String othersTopicTypeUri) {
        return dms.storage.fetchAssociationRelatedTopic(getId(), assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri);
    }

    @Override
    final ResultSet<RelatedTopicModel> fetchRelatedTopics(String assocTypeUri, String myRoleTypeUri,
                                                          String othersRoleTypeUri, String othersTopicTypeUri,
                                                          int maxResultSize) {
        return dms.storage.fetchAssociationRelatedTopics(getId(), assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri, maxResultSize);
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

    private void filterTopic(Role role, String roleTypeUri, Set<Topic> topics) {
        if (role instanceof TopicRole && role.getRoleTypeUri().equals(roleTypeUri)) {
            topics.add(((TopicRole) role).getTopic());
        }
    }

    // ---

    private void reassignInstantiation() {
        // remove current assignment
        fetchInstantiation().delete(new Directives());      // ### FIXME: receive directives as argument
        // create new assignment
        dms.createAssociationInstantiation(getId(), getTypeUri());
    }

    private Association fetchInstantiation() {
        RelatedTopic assocType = getRelatedTopic("dm4.core.instantiation", "dm4.core.instance", "dm4.core.type",
            "dm4.core.assoc_type", false, false);
        //
        if (assocType == null) {
            throw new RuntimeException("Association " + getId() + " is not associated to an association type");
        }
        //
        return assocType.getRelatingAssociation();
    }
}
