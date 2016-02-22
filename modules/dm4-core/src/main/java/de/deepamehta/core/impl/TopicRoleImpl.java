package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicRole;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicRoleModel;

import java.util.logging.Logger;



/**
 * A topic role that is attached to the {@link DeepaMehtaService}.
 */
class TopicRoleImpl extends RoleImpl implements TopicRole {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicRoleImpl(TopicRoleModel model, Association assoc, PersistenceLayer pl) {
        super(model, assoc, pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Role Implementation ===

    @Override
    public DeepaMehtaObject getPlayer() {
        if (topicIdentifiedByUri()) {
            return dms.getTopic("uri", new SimpleValue(getTopicUri()));
        } else {
            return dms.getTopic(getPlayerId());
        }
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
    public TopicRoleModel getModel() {
        return (TopicRoleModel) super.getModel();
    }
}
