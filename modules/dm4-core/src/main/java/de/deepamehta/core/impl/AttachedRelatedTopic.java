package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.model.RelatedTopicModel;



/**
 * A Topic-Association pair that is attached to the {@link DeepaMehtaService}.
 */
class AttachedRelatedTopic extends AttachedTopic implements RelatedTopic {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Association assoc;      // Attached object cache

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedRelatedTopic(RelatedTopicModel model, EmbeddedService dms) {
        super(model, dms);
        this.assoc = new AttachedAssociation(model.getAssociationModel(), dms);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Association getAssociation() {
        return assoc;
    }

    @Override
    public RelatedTopicModel getModel() {
        return (RelatedTopicModel) super.getModel();
    }
}
