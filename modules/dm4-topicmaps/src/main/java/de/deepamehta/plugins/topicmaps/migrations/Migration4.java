package de.deepamehta.plugins.topicmaps.migrations;

import de.deepamehta.core.Association;
import de.deepamehta.core.ChildTopics;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Migration;

import java.util.logging.Logger;



/**
 * Converts the "Topic Mapcontext" association's child topics into properties.
 * Runs only in UPDATE mode.
 * <p>
 * Part of DM 4.6
 */
public class Migration4 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private long assocs = 0, topicsDeleted = 0, typesDeleted = 0;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        logger.info("########## Converting \"Topic Mapcontext\" associations");
        //
        // 1) convert the "Topic Mapcontext" association's child topics into properties
        for (Association assoc : dms.getAssociations("dm4.topicmaps.topic_mapcontext")) {
            migrateMapcontextAssociation(assoc);
        }
        //
        // 2) delete "Topic Mapcontext" child types
        deleteTopicType("dm4.topicmaps.x");
        deleteTopicType("dm4.topicmaps.y");
        deleteTopicType("dm4.topicmaps.visibility");
        //
        // 3) make "Topic Mapcontext" a simple type
        dms.getAssociationType("dm4.topicmaps.topic_mapcontext").setDataTypeUri("dm4.core.text");
        //
        logger.info("########## Converting \"Topic Mapcontext\" associations complete\n    Associations processed: " +
            assocs + "\n    X, Y, Visibility topics deleted: " + topicsDeleted + "\n    Topic types deleted: " +
            typesDeleted);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void migrateMapcontextAssociation(Association assoc) {
        assocs++;
        //
        ChildTopics childs = assoc.getChildTopics();
        int x = childs.getInt("dm4.topicmaps.x");
        int y = childs.getInt("dm4.topicmaps.y");
        boolean visibility = childs.getBoolean("dm4.topicmaps.visibility");
        //
        assoc.setProperty("dm4.topicmaps.x", x, false);                     // addToIndex = false
        assoc.setProperty("dm4.topicmaps.y", y, false);                     // addToIndex = false
        assoc.setProperty("dm4.topicmaps.visibility", visibility, false);   // addToIndex = false
    }

    private void deleteTopicType(String topicTypeUri) {
        typesDeleted++;
        // delete instances
        for (Topic topic : dms.getTopics(topicTypeUri)) {
            topic.delete();
            topicsDeleted++;
        }
        // delete type
        dms.deleteTopicType(topicTypeUri);
    }
}
