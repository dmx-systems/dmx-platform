package de.deepamehta.core.impl;

import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.RoleModel;

import org.codehaus.jettison.json.JSONObject;



abstract class RoleModelImpl implements RoleModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected long playerId;        // id of the player (a topic, or an association)
    protected String roleTypeUri;   // is never null

    protected PersistenceLayer pl;

    // ---------------------------------------------------------------------------------------------------- Constructors

    // ### TODO: drop this?
    RoleModelImpl() {
    }

    RoleModelImpl(long playerId, String roleTypeUri, PersistenceLayer pl) {
        setPlayerId(playerId);
        setRoleTypeUri(roleTypeUri);
        this.pl = pl;
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

    // ----------------------------------------------------------------------------------------- Package Private Methods

    DeepaMehtaObjectModel getPlayer() {
        throw new UnsupportedOperationException();
    }
}
