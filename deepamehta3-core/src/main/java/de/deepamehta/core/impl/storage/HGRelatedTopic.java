package de.deepamehta.core.impl.storage;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;



class HGRelatedTopic extends HGTopic implements RelatedTopic {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Association association;

    // ---------------------------------------------------------------------------------------------------- Constructors

    HGRelatedTopic(Topic topic) {
        super(topic);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Association getAssociation() {
        return association;
    }

    @Override
    public void setAssociation(Association association) {
        this.association = association;
    }
}
