package de.deepamehta.plugins.accesscontrol.model;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import static java.util.Arrays.asList;



public class AccessControlList {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<Operation, UserRole[]> acl = new HashMap<Operation, UserRole[]>();

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AccessControlList(ACLEntry... aclEntries) {
        for (ACLEntry aclEntry : aclEntries) {
            addEntry(aclEntry);
        }
    }

    public AccessControlList(JSONObject acl) {
        try {
            Iterator i = acl.keys();
            while (i.hasNext()) {
                Operation operation = Operation.valueOf((String) i.next());
                JSONArray a = acl.getJSONArray(operation.name());
                int len = a.length();
                UserRole[] userRoles = new UserRole[len];
                for (int j = 0; j < len; j++) {
                    userRoles[j] = UserRole.valueOf(a.getString(j));
                }
                addEntry(new ACLEntry(operation, userRoles));
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing AccessControlList failed (JSONObject=" + acl + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public UserRole[] getUserRoles(Operation operation) {
        UserRole[] userRoles = acl.get(operation);
        return userRoles != null ? userRoles : new UserRole[0];
    }

    public AccessControlList addEntry(ACLEntry aclEntry) {
        acl.put(aclEntry.getOperation(), aclEntry.getUserRoles());
        return this;
    }

    // ---

    // Note: we do not implement JSONEnabled. An AccessControlList is never send through the wire.
    public JSONObject toJSON() {
        try {
            JSONObject json = new JSONObject();
            for (Operation operation : acl.keySet()) {
                json.put(operation.name(), asList(getUserRoles(operation)));
            }
            return json;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }
}
