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
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.ResultList;

import java.util.List;

import java.util.logging.Logger;



/**
 * An association model that is attached to the DB.
 */
class AssociationImpl extends DeepaMehtaObjectImpl implements Association {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Role role1;     // attached object cache
    private Role role2;     // attached object cache

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssociationImpl(AssociationModel model, EmbeddedService dms) {
        super(model, dms);
        // init attached object cache
        this.role1 = createAttachedRole(model.getRoleModel1());
        this.role2 = createAttachedRole(model.getRoleModel2());
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************
    // *** DeepaMehtaObjectImpl Overrides ***
    // **************************************



    // === Updating ===

    /**
     * @param   model   The data to update.
     *                  If the type URI is <code>null</code> it is not updated.
     *                  If role 1 is <code>null</code> it is not updated.
     *                  If role 2 is <code>null</code> it is not updated.
     */
    @Override
    public void update(AssociationModel model) {
        // Note: there is no possible POST_UPDATE_ASSOCIATION_REQUEST event to fire here (compare to
        // TopicImpl update()). It would be equivalent to POST_UPDATE_ASSOCIATION.
        // Per request exactly one association is updated. Its childs are always topics (never associations).
        logger.info("Updating association " + getId() + " (typeUri=\"" + getTypeUri() + "\")");
        //
        dms.fireEvent(CoreEvent.PRE_UPDATE_ASSOCIATION, this, model);
        //
        AssociationModel oldModel = getModel().clone();
        super.update(model);
        updateRole(model.getRoleModel1(), 1);
        updateRole(model.getRoleModel2(), 2);
        //
        dms.fireEvent(CoreEvent.POST_UPDATE_ASSOCIATION, this, oldModel);
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
        Topic topic1 = filterTopic(getRole1(), roleTypeUri);
        Topic topic2 = filterTopic(getRole2(), roleTypeUri);
        if (topic1 != null && topic2 != null) {
            throw new RuntimeException("Ambiguity in association: both topics have role type \"" + roleTypeUri +
                "\" (" + this + ")");
        }
        return topic1 != null ? topic1 : topic2 != null ? topic2 : null;
    }

    @Override
    public Topic getTopicByType(String topicTypeUri) {
        Topic topic1 = filterTopic(getPlayer1(), topicTypeUri);
        Topic topic2 = filterTopic(getPlayer2(), topicTypeUri);
        if (topic1 != null && topic2 != null) {
            throw new RuntimeException("Ambiguity in association: both topics are of type \"" + topicTypeUri +
                "\" (" + this + ")");
        }
        return topic1 != null ? topic1 : topic2 != null ? topic2 : null;
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
        return filterRole(getRole1(), roleModel) != null || filterRole(getRole2(), roleModel) != null;
    }

    // ---

    @Override
    public Association loadChildTopics() {
        return (Association) super.loadChildTopics();
    }

    @Override
    public Association loadChildTopics(String childTypeUri) {
        return (Association) super.loadChildTopics(childTypeUri);
    }

    // ---

    @Override
    public AssociationModel getModel() {
        return (AssociationModel) super.getModel();
    }



    // ***************************************
    // *** DeepaMehtaObject Implementation ***
    // ***************************************



    // === Traversal ===

    // --- Topic Retrieval ---

    @Override
    public ResultList<RelatedTopic> getRelatedTopics(List assocTypeUris, String myRoleTypeUri, String othersRoleTypeUri,
                                                     String othersTopicTypeUri) {
        ResultList<RelatedTopicModel> topics = pl.fetchAssociationRelatedTopics(getId(),
            assocTypeUris, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
        return dms.instantiateRelatedTopics(topics);
    }

    // --- Association Retrieval ---

    @Override
    public RelatedAssociation getRelatedAssociation(String assocTypeUri, String myRoleTypeUri,
                                                    String othersRoleTypeUri, String othersAssocTypeUri) {
        RelatedAssociationModel assoc = pl.fetchAssociationRelatedAssociation(getId(),
            assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersAssocTypeUri);
        return assoc != null ? dms.instantiateRelatedAssociation(assoc) : null;
    }

    @Override
    public ResultList<RelatedAssociation> getRelatedAssociations(String assocTypeUri, String myRoleTypeUri,
                                                                 String othersRoleTypeUri, String othersAssocTypeUri) {
        ResultList<RelatedAssociationModel> assocs = pl.fetchAssociationRelatedAssociations(getId(),
            assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersAssocTypeUri);
        return dms.instantiateRelatedAssociations(assocs);
    }

    // ---

    @Override
    public Association getAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                   long othersTopicId) {
        AssociationModel assoc = pl.fetchAssociationBetweenTopicAndAssociation(assocTypeUri,
            othersTopicId, getId(), othersRoleTypeUri, myRoleTypeUri);
        return assoc != null ? dms.instantiateAssociation(assoc) : null;
    }

    @Override
    public List<Association> getAssociations() {
        return dms.instantiateAssociations(pl.fetchAssociationAssociations(getId()));
    }



    // === Properties ===

    @Override
    public void setProperty(String propUri, Object propValue, boolean addToIndex) {
        pl.storeAssociationProperty(getId(), propUri, propValue, addToIndex);
    }

    @Override
    public void removeProperty(String propUri) {
        pl.removeAssociationProperty(getId(), propUri);
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Implementation of the abstract methods ===

    @Override
    final String className() {
        return "association";
    }

    @Override
    void updateChildTopics(ChildTopicsModel childTopics) {
        update(mf.newAssociationModel(childTopics));
    }

    @Override
    Directive getUpdateDirective() {
        return Directive.UPDATE_ASSOCIATION;
    }

    @Override
    final void storeTypeUri() {
        reassignInstantiation();
        pl.storeAssociationTypeUri(getId(), getTypeUri());
    }

    // ---

    @Override
    final RelatedTopicModel fetchRelatedTopic(String assocTypeUri, String myRoleTypeUri,
                                              String othersRoleTypeUri, String othersTopicTypeUri) {
        return pl.fetchAssociationRelatedTopic(getId(), assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
    }

    @Override
    final ResultList<RelatedTopicModel> fetchRelatedTopics(String assocTypeUri, String myRoleTypeUri,
                                                           String othersRoleTypeUri, String othersTopicTypeUri) {
        return pl.fetchAssociationRelatedTopics(getId(), assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
    }

    // ---

    @Override
    AssociationType getType() {
        return dms.getAssociationType(getTypeUri());
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
            return new TopicRoleImpl((TopicRoleModel) model, this, dms);
        } else if (model instanceof AssociationRoleModel) {
            return new AssociationRoleImpl((AssociationRoleModel) model, this, dms);
        } else {
            throw new RuntimeException("Unexpected RoleModel object (" + model + ")");
        }
    }

    // ---

    private Topic filterTopic(Role role, String roleTypeUri) {
        return role instanceof TopicRole && role.getRoleTypeUri().equals(roleTypeUri) ? ((TopicRole) role).getTopic()
            : null;
    }

    private Topic filterTopic(DeepaMehtaObject object, String topicTypeUri) {
        return object instanceof Topic && object.getTypeUri().equals(topicTypeUri) ? (Topic) object : null;
    }

    // ---

    private TopicRole filterRole(Role role, TopicRoleModel roleModel) {
        return role instanceof TopicRole && role.getRoleTypeUri().equals(roleModel.getRoleTypeUri()) &&
            role.getPlayerId() == roleModel.getPlayerId() ? (TopicRole) role : null;
    }

    // ---

    private void reassignInstantiation() {
        // remove current assignment
        fetchInstantiation().delete();
        // create new assignment
        pl.createAssociationInstantiation(getId(), getTypeUri());
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
