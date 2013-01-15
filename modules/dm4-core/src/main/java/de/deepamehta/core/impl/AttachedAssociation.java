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



    // === Implementation of the abstract methods ===

    @Override
    protected String className() {
        return "association";
    }

    @Override
    protected void storeUri() {
        dms.storage.storeAssociationUri(getId(), getUri());
    }

    @Override
    protected void storeTypeUri() {
        // remove current assignment
        long assocId = dms.objectFactory.fetchAssociationTypeTopic(getId()).getRelatingAssociation().getId();
        dms.deleteAssociation(assocId, null);  // clientState=null
        // create new assignment
        dms.objectFactory.associateWithAssociationType(getId(), getTypeUri());
    }

    @Override
    protected void storeSimpleValue(Set<IndexMode> indexModes, String indexKey) {
        dms.storage.storeAssociationValue(getId(), getSimpleValue(), indexModes, indexKey, getIndexValue());
    }

    @Override
    protected Type getType() {
        return dms.getAssociationType(getTypeUri(), null);    // FIXME: clientState=null
    }

    @Override
    protected RoleModel createRoleModel(String roleTypeUri) {
        return new AssociationRoleModel(getId(), roleTypeUri);
    }



    // === Deletion ===

    @Override
    public void delete(Directives directives) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            // delete sub-topics and associations
            super.delete(directives);
            // delete association itself
            logger.info("Deleting " + this);
            dms.storage.deleteAssociation(getId());
            directives.add(Directive.DELETE_ASSOCIATION, this);
            //
            tx.success();
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
            assocTypeUri, myRoleTypeUri, othersRoleTypeUri, null);      // othersAssocTypeUri=null
        return assoc != null ? dms.attach(assoc, false, false) : null;  // fetchComposite=false,
                                                                        // fetchRelatingComposite=false
    }



    // ***************************************
    // *** DeepaMehtaObject Implementation ***
    // ***************************************



    // === Traversal ===

    // --- Topic Retrieval ---

    @Override
    public AttachedRelatedTopic getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                String othersTopicTypeUri, boolean fetchComposite,
                                                boolean fetchRelatingComposite, ClientState clientState) {
        RelatedTopicModel topic = dms.storage.fetchAssociationRelatedTopic(getId(), assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
        return topic != null ? dms.attach(topic, fetchComposite, fetchRelatingComposite, clientState) : null;
    }

    @Override
    public ResultSet<RelatedTopic> getRelatedTopics(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                    String othersTopicTypeUri, boolean fetchComposite, boolean fetchRelatingComposite,
                                    int maxResultSize, ClientState clientState) {
        ResultSet<RelatedTopicModel> topics = dms.storage.fetchAssociationRelatedTopics(getId(), assocTypeUri,
            myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri, maxResultSize);
        return dms.attach(topics, fetchComposite, fetchRelatingComposite, clientState);
    }

    @Override
    public ResultSet<RelatedTopic> getRelatedTopics(List assocTypeUris, String myRoleTypeUri, String othersRoleTypeUri,
                                    String othersTopicTypeUri, boolean fetchComposite, boolean fetchRelatingComposite,
                                    int maxResultSize, ClientState clientState) {
        ResultSet<RelatedTopicModel> topics = dms.storage.fetchAssociationRelatedTopics(getId(), assocTypeUris,
            myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri, maxResultSize);
        return dms.attach(topics, fetchComposite, fetchRelatingComposite, clientState);
    }

    // --- Association Retrieval ---

    @Override
    public Association getAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                   long othersTopicId) {
        AssociationModel assoc = dms.storage.fetchAssociationBetweenTopicAndAssociation(assocTypeUri,
            othersTopicId, getId(), othersRoleTypeUri, myRoleTypeUri);
        return assoc != null ? dms.attach(assoc, false) : null;                         // fetchComposite=false
    }

    @Override
    public Set<Association> getAssociations() {
        return dms.attach(dms.storage.fetchAssociationAssociations(getId()), false);    // fetchComposite=false
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * Convenience method.
     */
    AssociationType getAssociationType() {
        return (AssociationType) getType();
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
}
