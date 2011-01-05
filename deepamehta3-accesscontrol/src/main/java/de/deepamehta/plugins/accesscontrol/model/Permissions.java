package de.deepamehta.plugins.accesscontrol.model;

import de.deepamehta.plugins.accesscontrol.AccessControlPlugin.Permission;
import de.deepamehta.core.util.JSONHelper;

import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;



public class Permissions extends HashMap {

    // private Map<String, Boolean> permissions = new HashMap();

    public Permissions() {
    }

    public Permissions(JSONObject permissions) {
        JSONHelper.toMap(permissions, this);
    }

    public void add(Permission permission, boolean value) {
        put(permission.s(), value);
    }
}
