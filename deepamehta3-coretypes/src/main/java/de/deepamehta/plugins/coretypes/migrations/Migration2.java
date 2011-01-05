package de.deepamehta.plugins.coretypes.migrations;

import de.deepamehta.core.model.DataField;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.service.Migration;

import java.util.logging.Logger;



/**
 * This migration adds a "Search Result" data field to the "Search Result" topic type.
 * <p>
 * This migration must run ALWAYS and we must check if the "Search Result" data field exists already.
 * <p>
 * Consider these cases:
 * 1) Clean installation of DeepaMehta 3 v0.4.3 => a plugin CLEAN_INSTALL is detected.
 *    Migration 1 runs and search-result.json *is* processed.
 *    Migration 2 runs and the "Search Result" data field is *not* created (exists already).
 * 2) Updating DeepaMehta 3 v0.4 to v0.4.3 => a plugin CLEAN_INSTALL is detected.
 *    Migration 1 runs and search-result.json is *not* processed (topic type "Search Result" exists already).
 *    Migration 2 runs and the "Search Result" data field *is* created.
 * 3) Updating DeepaMehta 3 v0.4.1 to v0.4.3 => a plugin UPDATE is detected.
 *    Migration 2 runs and the "Search Result" data field *is* created.
 * <p>
 * We want search-result.json to reflect the up-to-date data model instead of reconstructing it from all
 * previous migrations.
 * <p>
 * Distributed with deepamehta3-coretypes v0.4.2 (introduced with DeepaMehta 3 v0.4.3).
 * In this release we added the "Search Result" data field to search-result.json.
 */
public class Migration2 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ***************************
    // *** Implement Migration ***
    // ***************************



    /**
     * Adds a "Search Result" data field to the "Search Result" topic type.
     */
    @Override
    public void run() {
        TopicType type = dms.getTopicType("de/deepamehta/core/topictype/SearchResult", null);   // clientContext=null
        if (!type.hasDataField("de/deepamehta/core/property/search_result")) {
            DataField resultField = new DataField("Search Result", "reference");
            resultField.setUri("de/deepamehta/core/property/search_result");
            resultField.setRefRelationTypeId("SEARCH_RESULT");
            resultField.setEditor("checkboxes");
            //
            type.addDataField(resultField);
        } else {
            logger.info("Do NOT create data field \"Search Result\" -- already exists");
        }
    }
}
