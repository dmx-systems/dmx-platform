package de.deepamehta.core.storage.impl;

import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.model.impl.TopicBase;



class HGTopic extends TopicBase {

    // ---------------------------------------------------------------------------------------------------- Constructors

    HGTopic(Topic topic) {
        super(((TopicBase) topic).getModel());
    }

    HGTopic(long id, String uri, TopicValue value, String typeUri, Composite composite) {
        super(new TopicModel(id, uri, value, typeUri, composite));
    }
}
