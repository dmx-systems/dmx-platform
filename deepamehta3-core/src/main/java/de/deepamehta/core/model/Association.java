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
 * A relation between 2 {@link Topic}s.
 * A relation has an ID, a type, and a set of properties.
 * <p>
 * Instances of this class are used to pass data around (<i>data transfer object</i>).
 * <p>
 * Note: instances of this class are not backed by a database.
 * That is, direct changes to a Relation object (e.g. by {@link #setProperty}) are not persistent.
 * To make persistent changes use the methods of the DeepaMehta core service
 * ({@link de.deepamehta.core.service.CoreService}).
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class Association {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private long id;
    private String typeUri;

    private Set<Role> roles;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public Association(long id, String typeUri, Set<Role> roles) {
        this.id = id;
        this.typeUri = typeUri;
        this.roles = roles;
    }

    public Association(JSONObject assoc) {
        try {
            this.id = -1;
            this.typeUri = assoc.getString("assoc_type");
            JSONArray topics = assoc.getJSONArray("topics");
            for (int i = 0; i < topics.length(); i++) {
                roles.add(new Role(topics.getJSONObject(i)));
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing " + this + " failed", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String getTypeUri() {
        return typeUri;
    }

    public Set<Role> getRoles() {
        return roles;
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
            throw new RuntimeException("Serializing " + this + " failed", e);
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
        return "association " + id + " (typeUri=\"" + typeUri + "\", roles=" + roles + ")";
    }
}
