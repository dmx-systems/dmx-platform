package de.deepamehta.plugins.typeeditor;

import de.deepamehta.core.Association;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.ViewConfigurationModel;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Plugin;

import java.util.logging.Logger;



public class TypeEditorPlugin extends Plugin {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************************
    // *** Core Hooks (called from DeepaMehta 3 Core) ***
    // **************************************************



    @Override
    public void postRetypeAssociationHook(Association assoc, String oldTypeUri, Directives directives) {
        if (isAssocDef(assoc.getTypeUri())) {
            AssociationDefinitionModel assocDef = buildAssocDefModel(assoc);
            // update/create assoc def
            String topicTypeUri = assocDef.getTopicTypeUri1();
            TopicType topicType = dms.getTopicType(topicTypeUri, null);
            if (isAssocDef(oldTypeUri)) {
                logger.info("### Updating association definition \"" + assocDef.getUri() +
                    "\" of topic type \"" + topicTypeUri + "\" (" + assocDef + ")");
                topicType.updateAssocDef(assocDef);
            } else {
                logger.info("### Adding association definition \"" + assocDef.getUri() +
                    "\" to topic type \"" + topicTypeUri + "\" (" + assocDef + ")");
                topicType.addAssocDef(assocDef);
            }
            directives.add(Directive.UPDATE_TOPIC_TYPE, topicType);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private AssociationDefinitionModel buildAssocDefModel(Association assoc) {
        String topicTypeUri1 = assoc.getTopic("dm3.core.topic_type_1").getUri();
        String topicTypeUri2 = assoc.getTopic("dm3.core.topic_type_2").getUri();
        // Note: the assoc def's ID is already known. Setting it explicitely
        // prevents the core from creating the underlying association.
        AssociationDefinitionModel model = new AssociationDefinitionModel(assoc.getId(),
            topicTypeUri1, topicTypeUri2);
        model.setAssocTypeUri(assoc.getTypeUri());
        model.setCardinalityUri1("dm3.core.one");
        model.setCardinalityUri2("dm3.core.one");
        model.setViewConfigModel(new ViewConfigurationModel());     // FIXME: this should be the default
        //
        return model;
    }

    private boolean isAssocDef(String assocTypeUri) {
        return assocTypeUri.equals("dm3.core.aggregation_def") ||
               assocTypeUri.equals("dm3.core.composition_def");
    }
}
