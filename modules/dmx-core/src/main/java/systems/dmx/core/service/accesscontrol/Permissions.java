package systems.dmx.core.service.accesscontrol;

import systems.dmx.core.JSONEnabled;

import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;



/**
 * Calculated permissions for the current user.
 * <p>
 * A mapping from operations to booleans.
 */
public class Permissions extends HashMap<String, Boolean> implements JSONEnabled {

    // -------------------------------------------------------------------------------------------------- Public Methods

    public Permissions add(Operation operation, boolean allowed) {
        put(operation.uri, allowed);
        return this;
    }

    @Override
    public JSONObject toJSON() {
        return new JSONObject(this);
    }
}
