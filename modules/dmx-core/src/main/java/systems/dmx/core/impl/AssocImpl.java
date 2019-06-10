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
    public final Player getPlayer1() {
        return getModel().getPlayer1().instantiate(getModel());
    }

    @Override
    public final Player getPlayer2() {
        return getModel().getPlayer2().instantiate(getModel());
    }

    // --- Convenience Methods ---

    @Override
    public final DMXObject getDMXObject1() {
        return getPlayer1().getDMXObject();
    }

    @Override
    public final DMXObject getDMXObject2() {
        return getPlayer2().getDMXObject();
    }

    // ---

    @Override
    public final RelatedObject getDMXObjectByRole(String roleTypeUri) {
        DMXObjectModelImpl object = getModel().getDMXObjectByRole(roleTypeUri);
        return object != null ? (RelatedObject) object.instantiate() : null;    // ### TODO: permission check?
    }

    @Override
    public final DMXObject getDMXObjectByType(String topicTypeUri) {
        DMXObjectModelImpl object = getModel().getDMXObjectByType(topicTypeUri);
        return object != null ? object.instantiate() : null;                    // ### TODO: permission check?
    }

    // ---

    @Override
    public final Player getPlayerByRole(String roleTypeUri) {
        return getModel().getPlayerByRole(roleTypeUri).instantiate(getModel());
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
    public final void update(AssocModel updateModel) {
        pl.updateAssoc(getModel(), (AssocModelImpl) updateModel);
    }

    @Override
    public final void delete() {
        pl.deleteAssoc(getModel());
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
    public final RelatedAssoc getRelatedAssoc(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                              String othersAssocTypeUri) {
        RelatedAssocModelImpl assoc = pl.getAssocRelatedAssoc(getId(), assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri);
        return assoc != null ? assoc.instantiate() : null;
    }

    @Override
    public final List<RelatedAssoc> getRelatedAssocs(String assocTypeUri, String myRoleTypeUri,
                                                     String othersRoleTypeUri, String othersAssocTypeUri) {
        return pl.instantiate(pl.getAssocRelatedAssocs(getId(), assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri));
    }

    // ---

    @Override
    public final Assoc getAssoc(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                long othersTopicId) {
        return pl.getAssocBetweenTopicAndAssoc(assocTypeUri, othersTopicId, getId(), othersRoleTypeUri, myRoleTypeUri);
    }

    @Override
    public final List<Assoc> getAssocs() {
        return pl.instantiate(pl.getAssocAssocs(getId()));
    }
}
