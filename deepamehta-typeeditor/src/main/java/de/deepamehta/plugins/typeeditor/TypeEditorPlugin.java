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
    // *** Core Hooks (called from DeepaMehta 4 Core) ***
    // **************************************************



    @Override
    public void postRetypeAssociationHook(Association assoc, String oldTypeUri, Directives directives) {
        if (isAssocDef(assoc.getTypeUri())) {
            AssociationDefinitionModel assocDef = buildAssocDefModel(assoc);
            // update/create assoc def
            String topicTypeUri = assocDef.getWholeTopicTypeUri();
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
        } else if (isAssocDef(oldTypeUri)) {
            TopicType topicType = removeAssocDef(assoc);
            directives.add(Directive.UPDATE_TOPIC_TYPE, topicType);
        }
    }

    @Override
    public void postDeleteAssociationHook(Association assoc, Directives directives) {
        if (isAssocDef(assoc.getTypeUri())) {
            TopicType topicType = removeAssocDef(assoc);
            directives.add(Directive.UPDATE_TOPIC_TYPE, topicType);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private AssociationDefinitionModel buildAssocDefModel(Association assoc) {
        String wholeTopicTypeUri = getWholeTopicTypeUri(assoc);
        String partTopicTypeUri = getPartTopicTypeUri(assoc);
        // Note: the assoc def's ID is already known. Setting it explicitely
        // prevents the core from creating the underlying association.
        AssociationDefinitionModel model = new AssociationDefinitionModel(assoc.getId(), assoc.getTypeUri(),
            wholeTopicTypeUri, partTopicTypeUri);
        model.setWholeCardinalityUri("dm4.core.one");           // FIXME: handle cardinality
        model.setPartCardinalityUri("dm4.core.one");            // FIXME: handle cardinality
        model.setViewConfigModel(new ViewConfigurationModel()); // FIXME: this should be the default
        //
        return model;
    }

    private TopicType removeAssocDef(Association assoc) {
        String wholeTopicTypeUri = getWholeTopicTypeUri(assoc);
        String partTopicTypeUri = getPartTopicTypeUri(assoc);
        TopicType topicType = dms.getTopicType(wholeTopicTypeUri, null);
        logger.info("### Removing association definition \"" + partTopicTypeUri +
            "\" from topic type \"" + wholeTopicTypeUri + "\"");
        topicType.removeAssocDef(partTopicTypeUri);
        return topicType;
    }

    private boolean isAssocDef(String assocTypeUri) {
        return assocTypeUri.equals("dm4.core.aggregation_def") ||
               assocTypeUri.equals("dm4.core.composition_def");
    }

    // ---

    // ### FIXME: copy in AttachedAssociationDefinition
    private String getWholeTopicTypeUri(Association assoc) {
        return assoc.getTopic("dm4.core.whole_type").getUri();
    }

    // ### FIXME: copy in AttachedAssociationDefinition
    private String getPartTopicTypeUri(Association assoc) {
        return assoc.getTopic("dm4.core.part_type").getUri();
    }
}
