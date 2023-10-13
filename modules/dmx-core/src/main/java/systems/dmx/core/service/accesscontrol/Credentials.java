package systems.dmx.core.service.accesscontrol;

import org.codehaus.jettison.json.JSONObject;

import com.sun.jersey.core.util.Base64;



/**
 * A pair of username and password (plain text).
 */
public class Credentials {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    public String username;
    public String password;         // plain text
    public String methodName;       // possibly uninitialized

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Standard constructor.
     *
     * Used e.g. when creating an user account programmatically (via migration or plugin).
     *
     * @param   password    as plain text
     */
    public Credentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Constructs Credentials from a JSON object.
     * Note: invoked from JAX-RS message body reader (see Webservice's ObjectProvider.java).
     *
     * Used e.g. when an user account is created interactively (via Webclient).
     *
     * @param   cred    A JSON object with 2 properties: "username" and "password".
     *                  The password is expected to be plain text.
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
     * Constructs Credentials from a request's Authorization header.
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
        this.password = password;
        this.methodName = method;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String toString() {
        return "username=\"" + username + "\", password=\"" + password + "\"";
    }
}
