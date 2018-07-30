package systems.dmx.topicmaps.migrations;

import systems.dmx.core.Association;
import systems.dmx.core.ChildTopics;
import systems.dmx.core.Topic;
import systems.dmx.core.service.Migration;

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
        for (Association assoc : dmx.getAssociationsByType("dmx.topicmaps.topic_mapcontext")) {
            migrateMapcontextAssociation(assoc);
        }
        //
        // 2) delete "Topic Mapcontext" child types
        deleteTopicType("dmx.topicmaps.x");
        deleteTopicType("dmx.topicmaps.y");
        deleteTopicType("dmx.topicmaps.visibility");
        //
        // 3) make "Topic Mapcontext" a simple type
        dmx.getAssociationType("dmx.topicmaps.topic_mapcontext").setDataTypeUri("dmx.core.text");
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
        int x = childs.getInt("dmx.topicmaps.x");
        int y = childs.getInt("dmx.topicmaps.y");
        boolean visibility = childs.getBoolean("dmx.topicmaps.visibility");
        //
        assoc.setProperty("dmx.topicmaps.x", x, false);                     // addToIndex = false
        assoc.setProperty("dmx.topicmaps.y", y, false);                     // addToIndex = false
        assoc.setProperty("dmx.topicmaps.visibility", visibility, false);   // addToIndex = false
    }

    private void deleteTopicType(String topicTypeUri) {
        typesDeleted++;
        // delete instances
        for (Topic topic : dmx.getTopicsByType(topicTypeUri)) {
            topic.delete();
            topicsDeleted++;
        }
        // delete type
        dmx.deleteTopicType(topicTypeUri);
    }
}
