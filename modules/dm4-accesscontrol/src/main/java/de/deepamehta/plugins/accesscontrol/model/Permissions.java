package de.deepamehta.plugins.accesscontrol.model;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;



/**
 * A mapping from operations to booleans.
 * <p>
 * Permissions objects are used to represent both:
 * - a part of a static ACL entry
 * - calculated permissions for the current user
 */
@SuppressWarnings("serial")
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

    public List<TopicModel> asTopics() {
        List<TopicModel> permissions = new ArrayList<TopicModel>();
        for (String operationUri : keySet()) {
            TopicModel permission = new TopicModel("dm4.accesscontrol.permission", new CompositeValue()
                .putRef("dm4.accesscontrol.operation", operationUri)
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
