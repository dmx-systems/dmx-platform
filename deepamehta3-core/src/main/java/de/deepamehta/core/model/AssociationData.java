package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

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
public class AssociationData {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private long id;
    private String typeUri;
    private Set<Role> roles;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationData(String typeUri) {
        this(-1, typeUri);
    }

    public AssociationData(long id, String typeUri) {
        this(id, typeUri, new HashSet());
    }

    public AssociationData(long id, String typeUri, Set<Role> roles) {
        this.id = id;
        this.typeUri = typeUri;
        this.roles = roles;
    }

    public AssociationData(JSONObject assoc) {
        try {
            this.id = -1;
            this.typeUri = assoc.getString("type_uri");
            this.roles = new HashSet();
            JSONArray topics = assoc.getJSONArray("topics");
            for (int i = 0; i < topics.length(); i++) {
                roles.add(new Role(topics.getJSONObject(i)));
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationData failed (JSONObject=" + assoc + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public long getId() {
        return id;
    }

    public String getTypeUri() {
        return typeUri;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    // ---

    public void addRole(Role role) {
        roles.add(role);
    }

    /* public PropValue getProperty(String key) {
        PropValue value = properties.get(key);
        if (value == null) {
            throw new RuntimeException("Property \"" + key + "\" of " + this + " is not initialized. " +
                "Remember: relations obtained by getRelatedTopics() provide no properties. " +
                "Use the providePropertiesHook() to initialize the properties you need.");
        }
        return value;
    }

    // ---

    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("type_id", typeId);
            o.put("src_topic_id", srcTopicId);
            o.put("dst_topic_id", dstTopicId);
            o.put("properties", properties.toJSON());
            return o;
        } catch (JSONException e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    public static JSONArray relationsToJson(List<Relation> relations) {
        JSONArray array = new JSONArray();
        for (Relation relation : relations) {
            array.put(relation.toJSON());
        }
        return array;
    } */

    // ---

    @Override
    public String toString() {
        return "association data (id=" + id + ", typeUri=\"" + typeUri + "\", roles=" + roles + ")";
    }
}
