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
        setPlayerId(playerId);
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

    // ### TODO: to be dropped?
    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public final void setRoleTypeUri(String roleTypeUri) {
        if (roleTypeUri == null) {
            throw new IllegalArgumentException("\"roleTypeUri\" must not be null");
        }
        //
        this.roleTypeUri = roleTypeUri;
    }

    // ---

    /**
     * Checks weather the given role model refers to the same object as this role model.
     * In case of a topic role model the topic IDs resp. URIs are compared.
     * In case of an association role model the association IDs are compared.
     * Note: the role types are not compared.
     *
     * @return  true if the given role model refers to the same object as this role model.
     */
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
