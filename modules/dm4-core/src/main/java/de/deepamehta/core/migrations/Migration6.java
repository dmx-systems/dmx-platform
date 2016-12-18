package de.deepamehta.core.migrations;

import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.service.Migration;



/**
 * Adds index modes to "Meta Type", "Topic Type" and "Association Type".
 * Runs ALWAYS.
 * <p>
 * Part of DM 4.8.5
 */
public class Migration6 extends Migration {

    @Override
    public void run() {
        dm4.getTopicType("dm4.core.meta_type")
            .addIndexMode(IndexMode.FULLTEXT)
            .addIndexMode(IndexMode.FULLTEXT_KEY);
        dm4.getTopicType("dm4.core.topic_type")
            .addIndexMode(IndexMode.FULLTEXT)
            .addIndexMode(IndexMode.FULLTEXT_KEY);
        dm4.getTopicType("dm4.core.assoc_type")
            .addIndexMode(IndexMode.FULLTEXT)
            .addIndexMode(IndexMode.FULLTEXT_KEY);
    }
}
