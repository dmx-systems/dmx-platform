package de.deepamehta.plugins.topicmaps.migrations;

import de.deepamehta.core.model.DataField;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.service.Migration;

import java.util.logging.Logger;



/**
 * Updates the "Relation ID" data field definition.
 * <p>
 * Note: this migration is distributed with deepamehta3-topicmaps v0.4.1. and is set to run only
 * when updating from deepamehta3-topicmaps v0.4 to v0.4.1 (see migration2.properties).
 */
public class Migration2 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private final Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        // clientContext=null
        TopicType relRefType = dms.getTopicType("de/deepamehta/core/topictype/TopicmapRelationRef", null);
        DataField relIdField = relRefType.getDataField("de/deepamehta/core/property/RelationID");
        relIdField.setIndexingMode("KEY");
    }
}
