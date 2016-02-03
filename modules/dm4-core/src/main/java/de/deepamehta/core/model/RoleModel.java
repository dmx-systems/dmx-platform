package de.deepamehta.core.model;

import de.deepamehta.core.JSONEnabled;



public interface RoleModel extends JSONEnabled, Cloneable {

    long getPlayerId();

    String getRoleTypeUri();

    // ---

    // ### TODO: to be dropped?
    void setPlayerId(long playerId);

    void setRoleTypeUri(String roleTypeUri);

    // ---

    /**
     * Checks weather the given role model refers to the same object as this role model.
     * In case of a topic role model the topic IDs resp. URIs are compared.
     * In case of an association role model the association IDs are compared.
     * Note: the role types are not compared.
     *
     * @return  true if the given role model refers to the same object as this role model.
     */
    boolean refsSameObject(RoleModel model);
}
