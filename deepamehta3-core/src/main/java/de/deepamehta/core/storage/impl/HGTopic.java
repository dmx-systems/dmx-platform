package de.deepamehta.core.storage.impl;

import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.model.impl.BaseTopic;



class HGTopic extends BaseTopic {

    // ---------------------------------------------------------------------------------------------------- Constructors

    HGTopic(long id, String uri, TopicValue value, String typeUri, Composite composite) {
        super(id, uri, value, typeUri, composite);
    }
}
