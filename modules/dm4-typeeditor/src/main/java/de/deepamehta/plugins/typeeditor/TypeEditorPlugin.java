package de.deepamehta.plugins.typeeditor;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.event.PostDeleteAssociationListener;
import de.deepamehta.core.service.event.PostRetypeAssociationListener;

import java.util.logging.Logger;



public class TypeEditorPlugin extends PluginActivator implements PostRetypeAssociationListener,
                                                                 PostDeleteAssociationListener {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void postRetypeAssociation(Association assoc, String oldTypeUri, Directives directives) {
        if (isAssocDef(assoc.getTypeUri())) {
            // update/create assoc def
            AssociationDefinitionModel assocDef;
            String topicTypeUri;
            TopicType topicType;
            if (isAssocDef(oldTypeUri)) {
                assocDef = dms.getObjectFactory().fetchAssociationDefinition(assoc).getModel();
                topicTypeUri = assocDef.getWholeTopicTypeUri();
                topicType = dms.getTopicType(topicTypeUri, null);
                logger.info("### Updating association definition \"" + assocDef.getUri() +
                    "\" of topic type \"" + topicTypeUri + "\" (" + assocDef + ")");
                topicType.updateAssocDef(assocDef);
            } else {
                assocDef = buildAssocDefModel(assoc);
                topicTypeUri = assocDef.getWholeTopicTypeUri();
                topicType = dms.getTopicType(topicTypeUri, null);
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
    public void postDeleteAssociation(Association assoc, Directives directives) {
        if (isAssocDef(assoc.getTypeUri())) {
            TopicType topicType = removeAssocDef(assoc);
            directives.add(Directive.UPDATE_TOPIC_TYPE, topicType);
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private AssociationDefinitionModel buildAssocDefModel(Association assoc) {
        String wholeTopicTypeUri = fetchWholeTopicType(assoc).getUri();
        String partTopicTypeUri  = fetchPartTopicType(assoc).getUri();
        // Note: the assoc def's ID is already known. Setting it explicitely
        // prevents the core from creating the underlying association.
        return new AssociationDefinitionModel(assoc.getId(), assoc.getTypeUri(), wholeTopicTypeUri, partTopicTypeUri,
            "dm4.core.one", "dm4.core.one", null);  // viewConfigModel=null
    }

    private TopicType removeAssocDef(Association assoc) {
        String wholeTopicTypeUri = fetchWholeTopicType(assoc).getUri();
        String partTopicTypeUri  = fetchPartTopicType(assoc).getUri();
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

    private Topic fetchWholeTopicType(Association assoc) {
        return dms.getObjectFactory().fetchWholeTopicType(assoc);
    }

    private Topic fetchPartTopicType(Association assoc) {
        return dms.getObjectFactory().fetchPartTopicType(assoc);
    }
}
