package de.deepamehta.plugins.coretypes;

import de.deepamehta.core.model.DataField;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.service.Plugin;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;



public class CoreTypesPlugin extends Plugin {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    // FIXME: is this constructor still needed?
    // Probably it is a relict from a time when we had "internal core plugins".
    public CoreTypesPlugin() {
        configProperties = new Properties();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************
    // *** Overriding Hooks ***
    // ************************



    @Override
    public void postUpdateHook(Topic topic, Map<String, Object> oldProperties) {
        if (topic.typeUri.equals("de/deepamehta/core/topictype/TopicType")) {
            // update type URI
            String oldTypeUri = (String) oldProperties.get("de/deepamehta/core/property/TypeURI");
            String newTypeUri = (String) topic.getProperty("de/deepamehta/core/property/TypeURI");
            if (!oldTypeUri.equals(newTypeUri)) {
                logger.info("### Changing type URI from \"" + oldTypeUri + "\" to \"" + newTypeUri + "\"");
                dms.getTopicType(oldTypeUri, null).setTypeUri(newTypeUri);  // clientContext=null
            }
            // update type label
            String oldTypeLabel = (String) oldProperties.get("de/deepamehta/core/property/TypeLabel");
            String newTypeLabel = (String) topic.getProperty("de/deepamehta/core/property/TypeLabel");
            if (!oldTypeLabel.equals(newTypeLabel)) {
                logger.info("### Changing type label from \"" + oldTypeLabel + "\" to \"" + newTypeLabel + "\"");
                dms.getTopicType(newTypeUri, null).setLabel(newTypeLabel);  // clientContext=null
            }
        }
    }
}
