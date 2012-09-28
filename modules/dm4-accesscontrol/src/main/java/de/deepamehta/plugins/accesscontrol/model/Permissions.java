package de.deepamehta.plugins.accesscontrol.model;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;



/**
 * A mapping from operations to booleans.
 * <p>
 * Permissions objects are used to represent both:
 * - a part of a static ACL entry
 * - calculated permissions for the current user
 */
public class Permissions extends HashMap<String, Boolean> implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------------- Constructors

    public Permissions() {
    }

    public Permissions(JSONObject permissions) {
        DeepaMehtaUtils.toMap(permissions, this);
    }

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
