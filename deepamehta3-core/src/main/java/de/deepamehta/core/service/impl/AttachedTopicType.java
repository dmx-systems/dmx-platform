package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.model.TopicTypeData;
import de.deepamehta.core.model.TopicValue;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;



class AttachedTopicType extends TopicTypeData implements TopicType {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private EmbeddedService dms;

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedTopicType(TopicTypeData topicTypeData, EmbeddedService dms) {
        super(topicTypeData);
        this.dms = dms;
    }

    AttachedTopicType(AttachedTopicType topicType) {
        super(topicType);
        this.dms = topicType.dms;
    }
}
