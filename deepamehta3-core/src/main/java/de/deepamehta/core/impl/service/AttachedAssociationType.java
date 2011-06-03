package de.deepamehta.core.impl.service;

import de.deepamehta.core.AssociationType;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicValue;



/**
 * An association type that is attached to the {@link DeepaMehtaService}.
 */
class AttachedAssociationType extends AttachedTopic implements AssociationType {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedAssociationType(EmbeddedService dms) {
        super((TopicModel) null, dms);  // the model remains uninitialized.
                                        // It is initialued later on through fetch().
    }

    AttachedAssociationType(AssociationTypeModel model, EmbeddedService dms) {
        super(model, dms);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void fetch(String assocTypeUri) {
        AttachedTopic typeTopic = dms.getTopic("uri", new TopicValue(assocTypeUri), false); // fetchComposite=false
        // error check
        if (typeTopic == null) {
            throw new RuntimeException("Association type \"" + assocTypeUri + "\" not found");
        }
        //
        setModel(typeTopic.getModel());
    }

    void store() {
        setId(dms.storage.createTopic(getModel()).getId());
        dms.associateWithTopicType(this);
        setValue(getValue());
    }
}
