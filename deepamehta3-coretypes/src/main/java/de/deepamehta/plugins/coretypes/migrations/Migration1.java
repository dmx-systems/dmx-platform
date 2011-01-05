package de.deepamehta.plugins.coretypes.migrations;

import de.deepamehta.core.service.Migration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



/**
 * This migration creates the topic types "Topic Type" and "Search Result".
 * <p>
 * The deepamehta3-coretypes plugin was introduced with DeepaMehta 3 v0.4.1.
 * In DeepaMehta 3 v0.4 the topic types "Topic Type" and "Search Result" were created by the deepamehta3-core module.
 * <p>
 * Distributed with deepamehta3-coretypes v0.4.1.
 */
public class Migration1 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ***************************
    // *** Implement Migration ***
    // ***************************



    /**
     * Creates the topic types "Topic Type" and "Search Result" by reading from file.
     * <p>
     * Note: usually we would do this by a declarative migration (setting up a types1.json file) and set it to run
     * only while a clean install (migration run mode CLEAN_INSTALL).
     * But, this migration always detects a clean install even while an update (from DeepaMehta 3 v0.4 to v0.4.1).
     * This is because the deepamehta3-coretypes plugin did not exist in DeepaMehta 3 v0.4 (and *is* clean installed in
     * DeepaMehta 3 v0.4.1). When updating from DeepaMehta 3 v0.4 to v0.4.1 "topic type already exists" would be thrown.
     * <p>
     * We solve this situation by using an imperative migration and check if the types already exist.
     */
    @Override
    public void run() {
        Set typeURIs = dms.getTopicTypeUris();
        // create topic type "Topic Type"
        if (!typeURIs.contains("de/deepamehta/core/topictype/TopicType")) {
            readMigrationFile("/migrations/topic-type.json");
        } else {
            logger.info("Do NOT create topic type \"Topic Type\" -- already exists");
            // update icon_src
            long typeId = dms.getTopicType("de/deepamehta/core/topictype/TopicType", null).id;     // clientContext=null
            Map properties = new HashMap();
            logger.info("Updating icon_src of topic type \"Topic Type\" (topic " + typeId + ")");
            properties.put("icon_src", "/de.deepamehta.3-coretypes/images/drawer.png");
            dms.setTopicProperties(typeId, properties);
        }
        // create topic type "Search Result"
        if (!typeURIs.contains("de/deepamehta/core/topictype/SearchResult")) {
            readMigrationFile("/migrations/search-result.json");
        } else {
            logger.info("Do NOT create topic type \"Search Result\" -- already exists");
            // update icon_src
            long typeId = dms.getTopicType("de/deepamehta/core/topictype/SearchResult", null).id;  // clientContext=null
            Map properties = new HashMap();
            logger.info("Updating icon_src of topic type \"Search Result\" (topic " + typeId + ")");
            properties.put("icon_src", "/de.deepamehta.3-coretypes/images/bucket.png");
            dms.setTopicProperties(typeId, properties);
        }
    }
}
