package de.deepamehta.core.impl.model;

import de.deepamehta.core.TopicRole;
import de.deepamehta.core.model.TopicRoleModel;

import org.codehaus.jettison.json.JSONObject;



public class TopicRoleBase extends RoleBase implements TopicRole {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    // ---------------------------------------------------------------------------------------------------- Constructors

    protected TopicRoleBase(TopicRoleModel model) {
        super(model);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === TopicRole Implementation ===

    @Override
    public long getTopicId() {
        return getModel().getTopicId();
    }

    @Override
    public String getTopicUri() {
        return getModel().getTopicUri();
    }

    @Override
    public boolean topicIdentifiedByUri() {
        return getModel().topicIdentifiedByUri();
    }



    // === Role Implementation ===

    @Override
    public JSONObject toJSON() {
        return getModel().toJSON();
    }

    // ----------------------------------------------------------------------------------------------- Protected Methods



    // === RoleBase Overrides ===

    @Override
    protected TopicRoleModel getModel() {
        return (TopicRoleModel) super.getModel();
    }
}
