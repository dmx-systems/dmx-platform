package systems.dmx.core.service.accesscontrol;

import systems.dmx.core.util.JavaUtils;

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
    public String password;             // SHA256 encoded
    public String plaintextPassword;    // possibly uninitialized
    public String methodName;           // possibly uninitialized

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Used to create an user account programmatically (via migration).
     *
     * @param   password    as plain text
     */
    public Credentials(String username, String password) {
        this.username = username;
        this.password = encodePassword(password);
    }

    /**
     * Used to create an user account programmatically (via Webclient).
     *
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
            throw new IllegalArgumentException("Illegal credentials: " + cred, e);
        }
    }

    /**
     * Used to authorize a request.
     */
    public Credentials(String authHeader) {
        String[] splitted = authHeader.split("\\s+");
        if (splitted.length != 2) {
            throw new IllegalArgumentException("Illegal Authorization header: \"" + authHeader + "\"");
        }
        String method = splitted[0];
        String userAndPassword = splitted[1];
        String[] values = new String(Base64.base64Decode(userAndPassword)).split(":");
        // Note: for the browser's own login dialog:
        //   values.length is 0 if neither a username nor a password is entered
        //   values.length is 1 if no password is entered
        String username = values.length > 0 ? values[0] : "";
        String password = values.length > 1 ? values[1] : "";
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
