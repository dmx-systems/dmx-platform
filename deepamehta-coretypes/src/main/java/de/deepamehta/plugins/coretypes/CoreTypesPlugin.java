package de.deepamehta.plugins.coretypes;

import de.deepamehta.core.model.Properties;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Plugin;

import java.util.logging.Logger;



public class CoreTypesPlugin extends Plugin {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************
    // *** Overriding Hooks ***
    // ************************



    @Override
    public void postUpdateHook(Topic topic, Properties oldProperties) {
        if (topic.typeUri.equals("de/deepamehta/core/topictype/TopicType")) {
            // update type URI
            String oldTypeUri = oldProperties.get("de/deepamehta/core/property/TypeURI").toString();
            String newTypeUri = topic.getProperty("de/deepamehta/core/property/TypeURI").toString();
            if (!oldTypeUri.equals(newTypeUri)) {
                logger.info("### Changing type URI from \"" + oldTypeUri + "\" to \"" + newTypeUri + "\"");
                dms.getTopicType(oldTypeUri, null).setTypeUri(newTypeUri);  // clientContext=null
            }
            // update type label
            String oldTypeLabel = oldProperties.get("de/deepamehta/core/property/TypeLabel").toString();
            String newTypeLabel = topic.getProperty("de/deepamehta/core/property/TypeLabel").toString();
            if (!oldTypeLabel.equals(newTypeLabel)) {
                logger.info("### Changing type label from \"" + oldTypeLabel + "\" to \"" + newTypeLabel + "\"");
                dms.getTopicType(newTypeUri, null).setLabel(newTypeLabel);  // clientContext=null
            }
        }
    }
}
