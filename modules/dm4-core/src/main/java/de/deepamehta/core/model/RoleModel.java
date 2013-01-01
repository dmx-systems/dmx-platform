package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;



public abstract class RoleModel implements Cloneable {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected long playerId;        // id of the player (a topic, or an association)
    protected String roleTypeUri;   // is never null

    // ---------------------------------------------------------------------------------------------------- Constructors

    // ### TODO: drop this?
    protected RoleModel() {
    }

    protected RoleModel(long playerId, String roleTypeUri) {
        this.playerId = playerId;
        setRoleTypeUri(roleTypeUri);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public long getPlayerId() {
        return playerId;
    }

    public final String getRoleTypeUri() {
        return roleTypeUri;
    }

    // ---

    public final void setRoleTypeUri(String roleTypeUri) {
        if (roleTypeUri == null) {
            throw new IllegalArgumentException("\"roleTypeUri\" must not be null");
        }
        //
        this.roleTypeUri = roleTypeUri;
    }

    // ---

    public abstract boolean refsSameObject(RoleModel model);

    public abstract JSONObject toJSON();

    // === Java API ===

    @Override
    public RoleModel clone() {
        try {
            return (RoleModel) super.clone();
        } catch (Exception e) {
            throw new RuntimeException("Cloning a RoleModel failed", e);
        }
    }
}
