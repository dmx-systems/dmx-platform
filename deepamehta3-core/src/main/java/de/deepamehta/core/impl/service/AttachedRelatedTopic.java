package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;



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
