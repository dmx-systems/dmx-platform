package de.deepamehta.core.service;

import java.util.HashMap;
import java.util.Map;



/**
 * Cookies sent by a client.
 */
public class ClientState {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, String> values = new HashMap();

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
      * Converts a "Cookie" header value (String) to a map (key=String, value=String).
      * E.g. "dm4_workspace_id=123; dm4_topicmap_id=234" => {"dm4_workspace_id"="123", "dm4_topicmap_id"="234"}
      * <p>
      * Called by JAX-RS container to create a ClientState from a "Cookie" @HeaderParam
      */
    public ClientState(String cookie) {
        if (cookie != null) {
            for (String value : cookie.split("; ")) {
                String[] val = value.split("=", 2);     // Limit 2 ensures 2nd element in case of empty value
                values.put(val[0], val[1]);
            }
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    /**
     * Returns the value of the cookie for the given name, or throws an exception if no such cookie exists.
     */
    public String get(String name) {
        String value = values.get(name);
        //
        if (value == null) {
            throw new RuntimeException("Missing \"" + name + "\" cookie (clientState=" + this + ")");
        }
        //
        return value;
    }

    /**
     * Convenience method to access a long value of the cookie for the given name, or throws an exception
     * if no such cookie exists.
     */
    public long getLong(String name) {
        try {
            return Long.parseLong(get(name));
        } catch (Exception e) {
            throw new RuntimeException("Getting a long value for the \"" + name + "\" cookie failed", e);
        }
    }

    // ---

    /**
     * Checks if there is a cookie with the given name.
     */
    public boolean has(String name) {
        return values.get(name) != null;
    }

    // ---

    @Override
    public String toString() {
        return values.toString();
    }
}
