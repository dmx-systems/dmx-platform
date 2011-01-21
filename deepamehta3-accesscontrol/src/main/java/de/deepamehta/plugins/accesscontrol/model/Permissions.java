package de.deepamehta.plugins.accesscontrol.model;

import de.deepamehta.core.util.JSONHelper;

import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;



public class Permissions extends HashMap {

    // private Map<String, Boolean> permissions = new HashMap();

    // ---------------------------------------------------------------------------------------------------- Constructors

    public Permissions() {
    }

    public Permissions(JSONObject permissions) {
        JSONHelper.toMap(permissions, this);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public void add(Permission permission, boolean value) {
        put(permission.s(), value);
    }
}
