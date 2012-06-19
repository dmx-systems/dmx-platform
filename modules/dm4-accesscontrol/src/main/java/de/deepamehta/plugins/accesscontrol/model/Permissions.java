package de.deepamehta.plugins.accesscontrol.model;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.util.JSONHelper;

import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;



/**
 * A mapping from operations to booleans.
 * <p>
 * Permissions objects are used for both situations:
 * - as part of static ACL entries
 * - to represent calculated permissions for the current user
 */
public class Permissions extends HashMap<String, Boolean> implements JSONEnabled {

    // private Map<String, Boolean> permissions = new HashMap();

    // ---------------------------------------------------------------------------------------------------- Constructors

    public Permissions() {
    }

    public Permissions(JSONObject permissions) {
        JSONHelper.toMap(permissions, this);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public void add(Operation operation, boolean allowed) {
        put(operation.uri, allowed);
    }

    public List<TopicModel> asTopics() {
        List<TopicModel> permissions = new ArrayList();
        for (String operationUri : keySet()) {
            TopicModel permission = new TopicModel("dm4.accesscontrol.permission", new CompositeValue()
                .put_ref("dm4.accesscontrol.operation", operationUri)
                .put("dm4.accesscontrol.allowed", get(operationUri))
            );
            permissions.add(permission);
        }
        return permissions;
    }

    @Override
    public JSONObject toJSON() {
        return new JSONObject(this);
    }
}
