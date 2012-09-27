package de.deepamehta.core.service;

import java.util.HashMap;
import java.util.Map;



public class ClientState {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, String> values = new HashMap<String, String>();

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
      * Converts a "Cookie" header value (String) to a map (key=String, value=String).
      * E.g. "user=jri; workspace_id=123" => {"user"="jri", "workspace_id"="123"}
      * <p>
      * Called by JAX-RS container to create a ClientState from a "Cookie" @HeaderParam
      */
    public ClientState(String cookie) {
        if (cookie != null) {
            for (String value : cookie.split("; ")) {
                String[] val = value.split("=");
                values.put(val[0], val[1]);
            }
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String get(String key) {
        return values.get(key);
    }

    /**
     * Convenience method.
     */
    public long getLong(String key) {
        return Long.parseLong(get(key));
    }

    // ---

    @Override
    public String toString() {
        return values.toString();
    }
}
