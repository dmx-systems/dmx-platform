package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicData;
import de.deepamehta.core.model.TopicValue;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;



/**
 * A topic that is attached to a {@link CoreService} implementation.
 */
class AttachedTopic extends TopicData implements Topic {

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
        dms.setTopicValue(this, value);
    }

    // TODO: override setComposite()

    // ---

    @Override
    public TopicValue getChildTopicValue(String assocDefUri) {
        return dms.getChildTopicValue(this, assocDefUri);
    }

    @Override
    public void setChildTopicValue(String assocDefUri, TopicValue value) {
        dms.setChildTopicValue(this, assocDefUri, value);
    }

    public Set<Topic> getRelatedTopics(String assocTypeUri) {
        return dms.getRelatedTopics(getId(), assocTypeUri);
    }

    @Override
    public Topic getRelatedTopic(String assocTypeUri, String myRoleType, String othersRoleType) {
        return dms.getRelatedTopic(getId(), assocTypeUri, myRoleType, othersRoleType);
    }

    @Override
    public Set<Topic> getRelatedTopics(String assocTypeUri, String myRoleType, String othersRoleType,
                                                                               boolean includeComposite) {
        return dms.getRelatedTopics(getId(), assocTypeUri, myRoleType, othersRoleType, includeComposite);
    }

    @Override
    public Set<Association> getAssociations(String myRoleType) {
        return dms.getAssociations(getId(), myRoleType);
    }
}
