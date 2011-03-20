package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicTypeData;
import de.deepamehta.core.model.TopicValue;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;



class AttachedTopicType extends TopicTypeData {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedTopicType(TopicTypeData topicTypeData, EmbeddedService dms) {
        super(topicTypeData);
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

}
