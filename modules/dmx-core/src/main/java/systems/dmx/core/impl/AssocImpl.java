package systems.dmx.core.impl;

import systems.dmx.core.Assoc;
import systems.dmx.core.DMXObject;
import systems.dmx.core.Player;
import systems.dmx.core.RelatedAssoc;
import systems.dmx.core.RelatedObject;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicPlayer;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.TopicPlayerModel;

import java.util.List;

import java.util.logging.Logger;



/**
 * An association model that is attached to the DB.
 */
class AssocImpl extends DMXObjectImpl implements Assoc {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssocImpl(AssocModelImpl model, PersistenceLayer pl) {
        super(model, pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *************
    // *** Assoc ***
    // *************



    @Override
    public final Player getRole1() {
        return getModel().getRoleModel1().instantiate(getModel());
    }

    @Override
    public final Player getRole2() {
        return getModel().getRoleModel2().instantiate(getModel());
    }

    // ---

    @Override
    public final DMXObject getPlayer1() {
        return getRole1().getPlayer();
    }

    @Override
    public final DMXObject getPlayer2() {
        return getRole2().getPlayer();
    }

    // --- Convenience Methods ---

    @Override
    public final Player getRole(String roleTypeUri) {
        return getModel().getRoleModel(roleTypeUri).instantiate(getModel());
    }

    @Override
    public final boolean hasSameRoleTypeUris() {
        return getModel().hasSameRoleTypeUris();
    }

    @Override
    public final boolean matches(String roleTypeUri1, long playerId1, String roleTypeUri2, long playerId2) {
        return getModel().matches(roleTypeUri1, playerId1, roleTypeUri2, playerId2);
    }

    @Override
    public final long getOtherPlayerId(long id) {
        return getModel().getOtherPlayerId(id);
    }

    // ---

    @Override
    public final RelatedObject getPlayer(String roleTypeUri) {
        DMXObjectModelImpl object = getModel().getPlayer(roleTypeUri);
        return object != null ? (RelatedObject) object.instantiate() : null;    // ### TODO: permission check?
    }

    @Override
    public final Topic getTopicByType(String topicTypeUri) {
        TopicModelImpl topic = getModel().getTopicByType(topicTypeUri);
        return topic != null ? topic.instantiate() : null;                      // ### TODO: permission check?
    }

    // ---

    // ### TODO: make use of model's getRole()
    @Override
    public final Player getRole(PlayerModel roleModel) {
        if (getRole1().getModel().refsSameObject(roleModel)) {
            return getRole1();
        } else if (getRole2().getModel().refsSameObject(roleModel)) {
            return getRole2();
        }
        throw new RuntimeException("Player is not part of association (role=" + roleModel + ", association=" + this);
    }

    @Override
    public final boolean isPlayer(TopicPlayerModel roleModel) {
        return filterRole(getRole1(), roleModel) != null || filterRole(getRole2(), roleModel) != null;
    }

    // ---

    @Override
    public final void update(AssocModel updateModel) {
        pl.updateAssociation(getModel(), (AssocModelImpl) updateModel);
    }

    @Override
    public final void delete() {
        pl.deleteAssociation(getModel());
    }

    // ---

    @Override
    public final Assoc loadChildTopics() {
        super.loadChildTopics();
        return this;
    }

    @Override
    public final Assoc loadChildTopics(String compDefUri) {
        super.loadChildTopics(compDefUri);
        return this;
    }

    // ---

    // Note: overridden by RelatedAssocImpl
    @Override
    public AssocModelImpl getModel() {
        return (AssocModelImpl) model;
    }



    // *****************
    // *** DMXObject ***
    // *****************



    // === Traversal ===

    // ### TODO: consider adding model convenience, would require model renamings (get -> fetch)

    // --- Assoc Retrieval ---

    @Override
    public final RelatedAssoc getRelatedAssociation(String assocTypeUri, String myRoleTypeUri,
                                                    String othersRoleTypeUri, String othersAssocTypeUri) {
        RelatedAssociationModelImpl assoc = pl.getAssociationRelatedAssociation(getId(), assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersAssocTypeUri);
        return assoc != null ? assoc.instantiate() : null;
    }

    @Override
    public final List<RelatedAssoc> getRelatedAssociations(String assocTypeUri, String myRoleTypeUri,
                                                           String othersRoleTypeUri, String othersAssocTypeUri) {
        return pl.instantiate(pl.getAssociationRelatedAssociations(getId(), assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersAssocTypeUri));
    }

    // ---

    @Override
    public final Assoc getAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                 long othersTopicId) {
        return pl.getAssociationBetweenTopicAndAssociation(assocTypeUri, othersTopicId, getId(), othersRoleTypeUri,
            myRoleTypeUri);
    }

    @Override
    public final List<Assoc> getAssociations() {
        return pl.instantiate(pl.getAssociationAssociations(getId()));
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    // ### TODO: move to model
    private final TopicPlayer filterRole(Player role, TopicPlayerModel roleModel) {
        return role instanceof TopicPlayer && role.getRoleTypeUri().equals(roleModel.getRoleTypeUri()) &&
            role.getPlayerId() == roleModel.getPlayerId() ? (TopicPlayer) role : null;
    }
}
