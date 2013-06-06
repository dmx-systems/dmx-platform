package de.deepamehta.plugins.accesscontrol.model;

import de.deepamehta.core.util.JavaUtils;

import com.sun.jersey.core.util.Base64;



public class Credentials {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String ENCRYPTED_PASSWORD_PREFIX = "-SHA256-";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    public String username;
    public String password;     // encrypted

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * @param   password    as plain text
     */
    public Credentials(String username, String password) {
        this.username = username;
        this.password = encryptPassword(password);
    }

    public Credentials(String authHeader) {
        authHeader = authHeader.substring("Basic ".length());
        String[] values = new String(Base64.base64Decode(authHeader)).split(":");
        // Note: values.length is 0 if neither a username nor a password is entered
        //       values.length is 1 if no password is entered
        this.username = values.length > 0 ? values[0] : "";
        this.password = encryptPassword(values.length > 1 ? values[1] : "");
        // Note: credentials obtained through Basic authorization are always plain text
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String toString() {
        return "username=\"" + username + "\", password=\""+ password + "\"";
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private String encryptPassword(String password) {
        return ENCRYPTED_PASSWORD_PREFIX + JavaUtils.encodeSHA256(password);
    }
}
