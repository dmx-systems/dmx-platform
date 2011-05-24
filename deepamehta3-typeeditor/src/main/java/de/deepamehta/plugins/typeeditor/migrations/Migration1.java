package de.deepamehta.plugins.topicmaps.migrations;

import de.deepamehta.core.model.DataField;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.service.Migration;

import java.util.logging.Logger;



public class Migration1 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private final Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        TopicType topicType = dms.getTopicType("dm3.core.topic_type", null);
    }
}
