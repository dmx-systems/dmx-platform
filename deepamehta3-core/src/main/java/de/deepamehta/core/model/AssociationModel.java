package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;



/**
 * Collection of the data that makes up an {@link Association}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class AssociationModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private long id;
    private String typeUri;
    private Role role1;
    private Role role2;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationModel(String typeUri) {
        this(-1, typeUri);
    }

    public AssociationModel(String typeUri, Role role1, Role role2) {
        this(-1, typeUri, role1, role2);
    }

    public AssociationModel(long id, String typeUri) {
        this(id, typeUri, null, null);
    }

    public AssociationModel(long id, String typeUri, Role role1, Role role2) {
        this.id = id;
        this.typeUri = typeUri;
        this.role1 = role1;
        this.role2 = role2;
    }

    public AssociationModel(Association assoc) {
        this(assoc.getId(), assoc.getTypeUri(), assoc.getRole1(), assoc.getRole2());
    }

    public AssociationModel(JSONObject assocModel) {
        try {
            this.id = -1;
            this.typeUri = assocModel.getString("type_uri");
            this.role1 = parseRole(assocModel.getJSONObject("role_1"));
            this.role2 = parseRole(assocModel.getJSONObject("role_2"));
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationModel failed (JSONObject=" + assocModel + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public long getId() {
        return id;
    }

    public String getTypeUri() {
        return typeUri;
    }

    // ---

    public Role getRole1() {
        return role1;
    }

    public Role getRole2() {
        return role2;
    }

    // ---

    public void setRole1(Role role) {
        this.role1 = role;
    }

    public void setRole2(Role role) {
        this.role2 = role;
    }

    // === Serialization ===

    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("type_uri", typeUri);
            o.put("role_1", role1.toJSON());
            o.put("role_2", role2.toJSON());
            return o;
        } catch (JSONException e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    public static JSONArray associationsToJSON(Iterable<Association> assocs) {
        JSONArray array = new JSONArray();
        for (Association assoc : assocs) {
            array.put(assoc.toJSON());
        }
        return array;
    }

    // === Java API ===

    @Override
    public boolean equals(Object o) {
        return ((AssociationModel) o).id == id;
    }

    @Override
    public int hashCode() {
        return ((Long) id).hashCode();
    }

    @Override
    public String toString() {
        return "association model (id=" + id + ", typeUri=\"" + typeUri +
            "\", role1=" + role1 + ", role2=" + role2 + ")";
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private Role parseRole(JSONObject role) {
        if (role.has("topic_id") || role.has("topic_uri")) {
            return new TopicRole(role);
        } else if (role.has("assoc_id")) {
            return new AssociationRole(role);
        } else {
            throw new RuntimeException("Parsing TopicRole/AssociationRole failed (JSONObject=" + role + ")");
        }
    }
}
