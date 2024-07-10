package systems.dmx.accountmanagement;



public final class Constants {

    private Constants() {}

    // Topic Types
    public static final String USER_ACCOUNT   = "dmx.accesscontrol.user_account";
    public static final String USERNAME       = "dmx.accesscontrol.username";
    public static final String PASSWORD       = "dmx.accesscontrol.password";

    // Props (topic/assoc)
    public static final String SALT           = "dmx.accesscontrol.salt";           // for Password topics

    static final String ACCOUNT_MANAGER_NAME = "dmx.accountmanager.name";
}
