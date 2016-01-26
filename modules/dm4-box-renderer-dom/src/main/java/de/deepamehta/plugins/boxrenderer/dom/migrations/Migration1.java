package de.deepamehta.plugins.boxrenderer.dom.migrations;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Migration;

import java.util.logging.Logger;



/**
 * Moves the BoxRenderer properties from topics to "Topic Mapcontext" associations.
 * Runs only in UPDATE mode.
 * <p>
 * Part of DM 4.6
 */
public class Migration1 extends Migration {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String PROP_COLOR    = "dm4.boxrenderer.color";
    private static final String PROP_EXPANDED = "dm4.boxrenderer.expanded";

    private static final String TOPIC_MAPCONTEXT = "dm4.topicmaps.topic_mapcontext";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private long topics = 0, props = 0;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        logger.info("########## Moving BoxRenderer properties from topics to \"Topic Mapcontext\" associations");
        //
        for (Topic topic : dms.getAllTopics()) {
            migrateBoxRendererProperties(topic);
        }
        //
        logger.info("########## Moving BoxRenderer properties complete\n    Topics processed: " +
            topics + "\n    Topics with BoxRenderer properties: " + props);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void migrateBoxRendererProperties(Topic topic) {
        String color = null;
        Boolean expanded = null;
        topics++;
        //
        if (topic.hasProperty(PROP_COLOR)) {
            color = (String) topic.getProperty(PROP_COLOR);
        }
        if (topic.hasProperty(PROP_EXPANDED)) {
            expanded = (Boolean) topic.getProperty(PROP_EXPANDED);
        }
        //
        if (color != null || expanded != null) {
            props++;
            for (RelatedTopic topicmap : topic.getRelatedTopics(TOPIC_MAPCONTEXT, "dm4.topicmaps.topicmap_topic",
                                                                "dm4.core.default", "dm4.topicmaps.topicmap")) {
                Association mapcontextAssoc = topicmap.getRelatingAssociation();
                if (color != null) {
                    mapcontextAssoc.setProperty(PROP_COLOR, color, false);          // addToIndex = false
                }
                if (expanded != null) {
                    mapcontextAssoc.setProperty(PROP_EXPANDED, expanded, false);    // addToIndex = false
                }
            }
            //
            topic.removeProperty(PROP_COLOR);
            topic.removeProperty(PROP_EXPANDED);
        }
    }
}
