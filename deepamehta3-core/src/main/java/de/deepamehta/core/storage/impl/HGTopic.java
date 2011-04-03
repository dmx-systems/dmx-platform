package de.deepamehta.core.storage.impl;

import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicData;
import de.deepamehta.core.model.TopicValue;



class HGTopic extends TopicData implements Topic {

    // ---------------------------------------------------------------------------------------------------- Constructors

    HGTopic(long id, String uri, TopicValue value, String typeUri, Composite composite) {
        super(id, uri, value, typeUri, composite);
    }
}
