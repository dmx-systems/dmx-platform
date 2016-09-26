package de.deepamehta.core.impl;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicRole;



/**
 * A topic role that is attached to the {@link PersistenceLayer}.
 */
class TopicRoleImpl extends RoleImpl implements TopicRole {

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicRoleImpl(TopicRoleModelImpl model, AssociationModelImpl assoc, PersistenceLayer pl) {
        super(model, assoc, pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Role Implementation ===

    @Override
    public DeepaMehtaObject getPlayer() {
        return getModel().getPlayer().instantiate();   // ### TODO: permission check?
    }



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
        return (Topic) getPlayer();
    }



    // === RoleImpl Overrides ===

    @Override
    public TopicRoleModelImpl getModel() {
        return (TopicRoleModelImpl) model;
    }
}
