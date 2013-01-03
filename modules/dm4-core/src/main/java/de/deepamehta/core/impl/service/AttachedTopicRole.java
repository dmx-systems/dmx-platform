package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicRole;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicRoleModel;

import java.util.logging.Logger;



/**
 * A topic role that is attached to the {@link DeepaMehtaService}.
 */
class AttachedTopicRole extends AttachedRole implements TopicRole {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedTopicRole(TopicRoleModel model, Association assoc, EmbeddedService dms) {
        super(model, assoc, dms);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === TopicRole Implementation ===

    @Override
    public String getTopicUri() {
        return getModel().getTopicUri();
    }

    @Override
    public boolean topicIdentifiedByUri() {
        return getModel().topicIdentifiedByUri();
    }

    // ---

    @Override
    public Topic getTopic() {
        if (topicIdentifiedByUri()) {
            return dms.getTopic("uri", new SimpleValue(getTopicUri()), false, null);    // fetchComposite=false
        } else {
            return dms.getTopic(getPlayerId(), false, null);    // fetchComposite=false, clientState=null
        }
    }



    // === Implementation of the abstract methods ===

    @Override
    void storeRoleTypeUri() {
        dms.storage.storeRoleTypeUri(getAssociation().getId(), getPlayerId(), getRoleTypeUri());
    }



    // === AttachedRole Overrides ===

    @Override
    public TopicRoleModel getModel() {
        return (TopicRoleModel) super.getModel();
    }
}
