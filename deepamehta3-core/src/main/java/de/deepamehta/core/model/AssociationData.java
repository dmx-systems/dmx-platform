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

    private Set<TopicRole>       topicRoles;
    private Set<AssociationRole> assocRoles;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationData(String typeUri) {
        this(-1, typeUri);
    }

    public AssociationData(long id, String typeUri) {
        this(id, typeUri, new HashSet(), new HashSet());
    }

    public AssociationData(long id, String typeUri, Set<TopicRole> topicRoles, Set<AssociationRole> assocRoles) {
        this.id = id;
        this.typeUri = typeUri;
        this.topicRoles = topicRoles;
        this.assocRoles = assocRoles;
    }

    public AssociationData(Association assoc) {
        this(assoc.getId(), assoc.getTypeUri(), assoc.getTopicRoles(), assoc.getAssociationRoles());
    }

    public AssociationData(JSONObject assocData) {
        try {
            this.id = -1;
            this.typeUri = assocData.getString("type_uri");
            this.topicRoles = new HashSet();
            JSONArray topicRoles = assocData.getJSONArray("topic_roles");
            for (int i = 0; i < topicRoles.length(); i++) {
                this.topicRoles.add(new TopicRole(topicRoles.getJSONObject(i)));
            }
            // FIXME: init assoc roles
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationData failed (JSONObject=" + assocData + ")", e);
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

    public Set<TopicRole> getTopicRoles() {
        return topicRoles;
    }

    public Set<AssociationRole> getAssociationRoles() {
        return assocRoles;
    }

    // ---

    public Topic getTopic(String roleTypeUri) {
        throw new RuntimeException("Method not implemented (" + getClass() + ")");
    }

    public Set<Topic> getTopics(String roleTypeUri) {
        throw new RuntimeException("Method not implemented (" + getClass() + ")");
    }

    // ---

    public void addTopicRole(TopicRole topicRole) {
        topicRoles.add(topicRole);
    }

    public void addAssociationRole(AssociationRole assocRole) {
        assocRoles.add(assocRole);
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
        return "association data (id=" + id + ", typeUri=\"" + typeUri +
            "\", topicRoles=" + topicRoles + ", assocRoles=" + assocRoles + ")";
    }
}
