package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.model.RelatedTopicModel;



/**
 * A Topic-Association pair that is attached to the {@link DeepaMehtaService}.
 */
class RelatedTopicImpl extends TopicImpl implements RelatedTopic {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Association relatingAssoc;      // Attached object cache

    // ---------------------------------------------------------------------------------------------------- Constructors

    RelatedTopicImpl(RelatedTopicModel model, PersistenceLayer pl) {
        super(model, pl);
        this.relatingAssoc = new AssociationImpl(model.getRelatingAssociation(), pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Association getRelatingAssociation() {
        return relatingAssoc;
    }

    @Override
    public RelatedTopicModelImpl getModel() {
        return (RelatedTopicModelImpl) super.getModel();
    }
}
