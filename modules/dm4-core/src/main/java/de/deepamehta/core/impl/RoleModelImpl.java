package de.deepamehta.core.impl;

import de.deepamehta.core.Role;
import de.deepamehta.core.model.RoleModel;



abstract class RoleModelImpl implements RoleModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    long playerId;                  // id of the player (a topic, or an association)
    String roleTypeUri;             // is never null

    PersistenceLayer pl;
    ModelFactoryImpl mf;

    // ---------------------------------------------------------------------------------------------------- Constructors

    // ### TODO: drop this?
    RoleModelImpl() {
    }

    RoleModelImpl(long playerId, String roleTypeUri, PersistenceLayer pl) {
        setPlayerId(playerId);
        setRoleTypeUri(roleTypeUri);
        this.pl = pl;
        this.mf = pl.mf;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public long getPlayerId() {
        return playerId;
    }

    @Override
    public final String getRoleTypeUri() {
        return roleTypeUri;
    }

    // ---

    // ### TODO: to be dropped?
    @Override
    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    @Override
    public final void setRoleTypeUri(String roleTypeUri) {
        if (roleTypeUri == null) {
            throw new IllegalArgumentException("\"roleTypeUri\" must not be null");
        }
        //
        this.roleTypeUri = roleTypeUri;
    }

    // ---

    // Note: refsSameObject() remain abstract



    // === Java API ===

    @Override
    public RoleModel clone() {
        try {
            return (RoleModel) super.clone();
        } catch (Exception e) {
            throw new RuntimeException("Cloning a RoleModel failed", e);
        }
    }

    // TODO: copy in DeepaMehtaObjectModelImpl
    // Can we use Java 8 and put this in the JSONEnabled interface?
    @Override
    public String toString() {
        try {
            return getClass().getSimpleName() + " " + toJSON().toString(4);
        } catch (Exception e) {
            throw new RuntimeException("Prettyprinting failed", e);
        }
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * @param   assoc   the association this role is involved in
     */
    abstract Role instantiate(AssociationModelImpl assoc);

    /**
     * @param   assoc   the association this role is involved in
     */
    abstract DeepaMehtaObjectModelImpl getPlayer(AssociationModelImpl assoc);
}
