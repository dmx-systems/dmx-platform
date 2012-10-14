package de.deepamehta.core.service.accesscontrol;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import static java.util.Arrays.asList;



public class AccessControlList {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private JSONObject acl = new JSONObject();

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AccessControlList(ACLEntry... aclEntries) {
        for (ACLEntry aclEntry : aclEntries) {
            addEntry(aclEntry);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public AccessControlList addEntry(ACLEntry aclEntry) {
        try {
            acl.put(aclEntry.getOperation().name(), new JSONArray(asList(aclEntry.getUserRoles())));
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Adding access control entry failed");
        }
    }

    public JSONObject toJSON() {
        return acl;
    }
}
