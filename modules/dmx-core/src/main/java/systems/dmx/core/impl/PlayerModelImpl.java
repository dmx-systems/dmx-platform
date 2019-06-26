package systems.dmx.core.impl;

import systems.dmx.core.Player;
import systems.dmx.core.model.PlayerModel;



abstract class PlayerModelImpl implements PlayerModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    long id = -1;           // id of the player (a topic, or an association)
    String roleTypeUri;     // is never null

    AccessLayer al;
    ModelFactoryImpl mf;

    // ---------------------------------------------------------------------------------------------------- Constructors

    // ### TODO: drop this?
    PlayerModelImpl() {
    }

    PlayerModelImpl(long playerId, String roleTypeUri, AccessLayer al) {
        this.id = playerId;
        setRoleTypeUri(roleTypeUri);
        this.al = al;
        this.mf = al.mf;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public long getId() {
        if (id == -1) {
            throw new IllegalStateException("No player ID set in " + this);
        }
        return id;
    }

    @Override
    public final String getRoleTypeUri() {
        return roleTypeUri;
    }

    // ---

    @Override
    public final void setRoleTypeUri(String roleTypeUri) {
        if (roleTypeUri == null) {
            throw new IllegalArgumentException("\"roleTypeUri\" must not be null");
        }
        //
        this.roleTypeUri = roleTypeUri;
    }



    // === Java API ===

    @Override
    public PlayerModel clone() {
        try {
            return (PlayerModel) super.clone();
        } catch (Exception e) {
            throw new RuntimeException("Cloning a PlayerModel failed", e);
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
     * @param   assoc   the association this player is involved in
     */
    abstract Player instantiate(AssocModelImpl assoc);

    /**
     * @param   assoc   the association this player is involved in
     */
    abstract DMXObjectModelImpl getDMXObject(AssocModelImpl assoc);
}
