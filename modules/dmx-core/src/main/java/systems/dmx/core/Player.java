package systems.dmx.core;

import systems.dmx.core.model.PlayerModel;



/**
 * A <code>Player</code> represents one of the 2 {@link Assoc} ends.
 * <p>
 * A <code>Player</code> has a {@link DMXObject} and a role type. The role type expresses the role the DMXObject
 * plays in the association.
 */
public interface Player extends JSONEnabled {

    long getId();

    String getRoleTypeUri();

    DMXObject getDMXObject();

    // ---

    void setRoleTypeUri(String roleTypeUri);

    // ---

    PlayerModel getModel();
}
