package de.deepamehta.core.storage.impl;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.RelatedTopic;
import de.deepamehta.core.model.Topic;



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
