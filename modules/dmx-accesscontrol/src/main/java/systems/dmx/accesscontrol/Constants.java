package systems.dmx.accesscontrol;



public class Constants {

    // Topic Types
    public static final String USER_ACCOUNT   = "dmx.accesscontrol.user_account";
    public static final String USERNAME       = "dmx.accesscontrol.username";
    public static final String PASSWORD       = "dmx.accesscontrol.password";
    public static final String LOGIN_ENABLED  = "dmx.accesscontrol.login_enabled";  // config type

    // Assoc Types
    public static final String MEMBERSHIP     = "dmx.accesscontrol.membership";

    // Props (topic/assoc)
    public static final String SALT           = "dmx.accesscontrol.salt";           // for Password topics
    public static final String OWNER          = "dmx.accesscontrol.owner";          // for Workspace topics
    public static final String CREATOR        = "dmx.accesscontrol.creator";
    public static final String MODIFIER       = "dmx.accesscontrol.modifier";

    // Misc
    public static final String ADMIN_USERNAME = "admin";
}
