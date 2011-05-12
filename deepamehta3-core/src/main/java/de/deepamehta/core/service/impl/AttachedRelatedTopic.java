package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.RelatedTopic;
import de.deepamehta.core.model.Topic;



/**
 * A topic that is attached to the {@link CoreService}.
 */
class AttachedRelatedTopic extends AttachedTopic implements RelatedTopic {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Association association;

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedRelatedTopic(Topic topic, EmbeddedService dms) {
        super(topic, dms);
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
