package de.deepamehta.core.impl;

import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicRole;



/**
 * A topic role that is attached to the {@link PersistenceLayer}.
 */
class TopicRoleImpl extends RoleImpl implements TopicRole {

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicRoleImpl(TopicRoleModelImpl model, AssociationModelImpl assoc) {
        super(model, assoc);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === TopicRole Implementation ===

    @Override
    public Topic getTopic() {
        return (Topic) getPlayer();
    }

    // ---

    @Override
    public String getTopicUri() {
        return getModel().getTopicUri();
    }

    @Override
    public boolean topicIdentifiedByUri() {
        return getModel().topicIdentifiedByUri();
    }



    // === RoleImpl Overrides ===

    @Override
    public TopicRoleModelImpl getModel() {
        return (TopicRoleModelImpl) model;
    }
}
