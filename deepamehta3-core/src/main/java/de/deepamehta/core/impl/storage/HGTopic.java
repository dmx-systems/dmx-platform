package de.deepamehta.core.impl.storage;

import de.deepamehta.core.Topic;
import de.deepamehta.core.impl.model.TopicBase;
import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicValue;



class HGTopic extends TopicBase {

    // ---------------------------------------------------------------------------------------------------- Constructors

    // Called from subclass constructor HGRelatedTopic
    HGTopic(Topic topic) {
        super(((TopicBase) topic).getModel());
    }

    HGTopic(long id, String uri, TopicValue value, String typeUri, Composite composite) {
        super(new TopicModel(id, uri, value, typeUri, composite));
    }
}
