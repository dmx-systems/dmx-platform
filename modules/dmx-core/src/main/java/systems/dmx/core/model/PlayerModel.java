package systems.dmx.core.model;

import systems.dmx.core.JSONEnabled;



public interface PlayerModel extends JSONEnabled, Cloneable {

    long getId();

    String getRoleTypeUri();

    // ---

    // ### TODO: to be dropped?
    void setId(long id);

    void setRoleTypeUri(String roleTypeUri);

    // ---

    /**
     * Checks whether the given role model refers to the same object as this role model.
     * In case of a topic role model the topic IDs resp. URIs are compared.
     * In case of an association role model the association IDs are compared.
     * Note: the role types are not compared.
     *
     * @return  true if the given role model refers to the same object as this role model.
     */
    boolean refsSameObject(PlayerModel model);

    // ---

    PlayerModel clone();
}
