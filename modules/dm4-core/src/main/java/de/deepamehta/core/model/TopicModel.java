package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;



public class TopicModel extends DeepaMehtaObjectModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicModel(ChildTopicsModel childTopics) {
        super(childTopics);
    }

    public TopicModel(String typeUri) {
        super(typeUri);
    }

    public TopicModel(String typeUri, SimpleValue value) {
        super(typeUri, value);
    }

    public TopicModel(String typeUri, ChildTopicsModel childTopics) {
        super(typeUri, childTopics);
    }

    public TopicModel(String uri, String typeUri) {
        super(uri, typeUri);
    }

    public TopicModel(String uri, String typeUri, SimpleValue value) {
        super(uri, typeUri, value);
    }

    public TopicModel(String uri, String typeUri, ChildTopicsModel childTopics) {
        super(uri, typeUri, childTopics);
    }

    public TopicModel(long id) {
        super(id);
    }

    public TopicModel(long id, String typeUri) {
        super(id, typeUri);
    }

    public TopicModel(long id, ChildTopicsModel childTopics) {
        super(id, childTopics);
    }

    /**
     * @param   uri         If <code>null</code> an empty string is set. This is OK.
     * @param   typeUri     Mandatory. Note: only the internal meta type topic (ID 0) has no type URI (null).
     * @param   value       If <code>null</code> an empty string value is set. This is OK.
     * @param   childTopics If <code>null</code> an empty composite is set. This is OK.
     */
    public TopicModel(long id, String uri, String typeUri, SimpleValue value, ChildTopicsModel childTopics) {
        super(id, uri, typeUri, value, childTopics);
    }

    public TopicModel(TopicModel topic) {
        super(topic);
    }

    public TopicModel(JSONObject topic) {
        super(topic);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Implementation of the abstract methods ===

    @Override
    public RoleModel createRoleModel(String roleTypeUri) {
        return new TopicRoleModel(getId(), roleTypeUri);
    }



    // === Java API ===

    @Override
    public TopicModel clone() {
        try {
            return (TopicModel) super.clone();
        } catch (Exception e) {
            throw new RuntimeException("Cloning a TopicModel failed", e);
        }
    }

    @Override
    public String toString() {
        return "topic (" + super.toString() + ")";
    }
}
