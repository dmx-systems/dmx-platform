package de.deepamehta.plugins.accesscontrol.model;

import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONObject;

import com.sun.jersey.core.util.Base64;



/**
 * A pair of username and SHA256 encoded password.
 */
public class Credentials {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String ENCODED_PASSWORD_PREFIX = "-SHA256-";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    public String username;
    public String password;     // encoded

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * @param   password    as plain text
     */
    public Credentials(String username, String password) {
        this.username = username;
        this.password = encodePassword(password);
    }

    /**
     * Note: invoked from JAX-RS message body reader (see Webservice's ObjectProvider.java).
     *
     * @param   cred    A JSON object with 2 properties: "username" and "password".
     *                  The password is expected to be SHA256 encoded.
     */
    public Credentials(JSONObject cred) {
        try {
            this.username = cred.getString("username");
            this.password = cred.getString("password");
        } catch (Exception e) {
            throw new IllegalArgumentException("Illegal JSON argument " + cred, e);
        }
    }

    public Credentials(String authHeader) {
        authHeader = authHeader.substring("Basic ".length());
        String[] values = new String(Base64.base64Decode(authHeader)).split(":");
        // Note: values.length is 0 if neither a username nor a password is entered
        //       values.length is 1 if no password is entered
        this.username = values.length > 0 ? values[0] : "";
        this.password = encodePassword(values.length > 1 ? values[1] : "");
        // Note: credentials obtained through Basic authorization are always plain text
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String toString() {
        return "username=\"" + username + "\", password=\""+ password + "\"";
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private String encodePassword(String password) {
        return ENCODED_PASSWORD_PREFIX + JavaUtils.encodeSHA256(password);
    }
}
