package systems.dmx.core.impl;

import systems.dmx.core.Role;
import systems.dmx.core.model.RoleModel;



abstract class RoleModelImpl implements RoleModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    long playerId = -1;             // id of the player (a topic, or an association)
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
        if (playerId == -1) {
            throw new IllegalStateException("Player ID is not set in " + this);
        }
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

    @Override
    public boolean refsSameObject(RoleModel model) {
        return getPlayerId() == model.getPlayerId();
    }



    // === Java API ===

    @Override
    public RoleModel clone() {
        try {
            return (RoleModel) super.clone();
        } catch (Exception e) {
            throw new RuntimeException("Cloning a RoleModel failed", e);
        }
    }

    // TODO: copy in DMXObjectModelImpl
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
    abstract Role instantiate(AssocModelImpl assoc);

    /**
     * @param   assoc   the association this role is involved in
     */
    abstract DMXObjectModelImpl getPlayer(AssocModelImpl assoc);
}
