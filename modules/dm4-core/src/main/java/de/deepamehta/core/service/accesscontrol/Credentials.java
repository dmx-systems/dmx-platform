package de.deepamehta.core.service.accesscontrol;

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
    public String plaintextPassword;
    public String methodName;

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * @param   password    as plain text
     */
    public Credentials(String username, String password) {
        this.username = username;
        this.password = encodePassword(password);
        this.plaintextPassword = password;
        this.methodName = "";
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
            this.plaintextPassword = ""; // TODO
            this.methodName = "";
        } catch (Exception e) {
            throw new IllegalArgumentException("Illegal JSON argument " + cred, e);
        }
    }

    public Credentials(String authHeader) {
        String[] splitted = authHeader.split("\\s+");
        if (splitted.length < 2) { // != 2 ?
            throw new IllegalArgumentException("Illegal AuthHeader argument " + authHeader);
        }
        String method = splitted[0];

        String userAndPasswordString = splitted[1];
        String[] userAndPasswordArray = new String(Base64.base64Decode(userAndPasswordString)).split(":");
        String username = userAndPasswordArray.length > 0 ? userAndPasswordArray[0] : "";
        String password = (userAndPasswordArray.length > 1 ? userAndPasswordArray[1] : "");
        this.username = username;
        this.password = encodePassword(password);
        this.plaintextPassword = password;
        this.methodName = method;
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
