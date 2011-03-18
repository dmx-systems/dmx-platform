package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.model.impl.BaseTopic;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;



class AttachedTopic extends BaseTopic {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedTopic(Topic topic, EmbeddedService dms) {
        super(topic);
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void setValue(TopicValue value) {
        // update memory
        super.setValue(value);
        // update DB
        dms.setTopicValue(getId(), value);
    }

    // ---

    @Override
    public Object getValue(String assocDefUri) {
        return dms.getTopicValue(this, assocDefUri);
    }

    @Override
    public void setValue(String assocDefUri, Object value) {
        dms.setTopicValue(this, assocDefUri, value);
    }

    @Override
    public Topic getRelatedTopic(String assocTypeUri, String myRoleType, String othersRoleType) {
        return dms.getRelatedTopic(getId(), assocTypeUri, myRoleType, othersRoleType);
    }
}
