package de.deepamehta.core.model;

import java.util.HashMap;
import java.util.Map;



public class ClientContext {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, String> values = new HashMap();

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
      * Converts a "Cookie" header value (String) to a map (key=String, value=String).
      * E.g. "user=jri; workspace_id=123" => {"user"="jri", "workspace_id"="123"}
      */
    public ClientContext(String cookie) {
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

    @Override
    public String toString() {
        return values.toString();
    }
}
