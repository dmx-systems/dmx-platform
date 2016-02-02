package de.deepamehta.core.impl;

import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;

import org.codehaus.jettison.json.JSONObject;



class TopicModelImpl extends DeepaMehtaObjectModelImpl implements TopicModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicModelImpl(ChildTopicsModel childTopics) {
        super(childTopics);
    }

    TopicModelImpl(String typeUri) {
        super(typeUri);
    }

    TopicModelImpl(String typeUri, SimpleValue value) {
        super(typeUri, value);
    }

    TopicModelImpl(String typeUri, ChildTopicsModel childTopics) {
        super(typeUri, childTopics);
    }

    TopicModelImpl(String uri, String typeUri) {
        super(uri, typeUri);
    }

    TopicModelImpl(String uri, String typeUri, SimpleValue value) {
        super(uri, typeUri, value);
    }

    TopicModelImpl(String uri, String typeUri, ChildTopicsModel childTopics) {
        super(uri, typeUri, childTopics);
    }

    TopicModelImpl(long id) {
        super(id);
    }

    TopicModelImpl(long id, String typeUri) {
        super(id, typeUri);
    }

    TopicModelImpl(long id, ChildTopicsModel childTopics) {
        super(id, childTopics);
    }

    /**
     * @param   uri         If <code>null</code> an empty string is set. This is OK.
     * @param   typeUri     Mandatory. Note: only the internal meta type topic (ID 0) has no type URI (null).
     * @param   value       If <code>null</code> an empty string value is set. This is OK.
     * @param   childTopics If <code>null</code> an empty composite is set. This is OK.
     */
    TopicModelImpl(long id, String uri, String typeUri, SimpleValue value, ChildTopicsModel childTopics) {
        super(id, uri, typeUri, value, childTopics);
    }

    TopicModelImpl(TopicModel topic) {
        super(topic);
    }

    TopicModelImpl(JSONObject topic) {
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
