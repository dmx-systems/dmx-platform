package de.deepamehta.core.impl.service;

import de.deepamehta.core.AssociationType;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TopicValue;



/**
 * An association type that is attached to the {@link DeepaMehtaService}.
 */
class AttachedAssociationType extends AttachedType implements AssociationType {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedAssociationType(EmbeddedService dms) {
        super(dms);     // The model remains uninitialized.
                        // It is initialized later on through fetch().
    }

    AttachedAssociationType(AssociationTypeModel model, EmbeddedService dms) {
        super(model, dms);
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void fetch(String assocTypeUri) {
        AttachedTopic typeTopic = dms.getTopic("uri", new TopicValue(assocTypeUri), false); // fetchComposite=false
        // error check
        if (typeTopic == null) {
            throw new RuntimeException("Association type \"" + assocTypeUri + "\" not found");
        }
        // build type model
        AssociationTypeModel model = new AssociationTypeModel(typeTopic.getModel(),
            fetchDataTypeTopic(typeTopic).getUri(), fetchViewConfig(typeTopic));
        //
        setModel(model);
        initViewConfig();
    }

    void store() {
        dms.storage.createTopic(getModel());
        dms.associateWithTopicType(getModel());
        setValue(getValue());
        //
        getViewConfig().store();
    }
}
