package systems.dmx.core.migrations;

import systems.dmx.core.model.IndexMode;
import systems.dmx.core.service.Migration;



/**
 * Add index modes to Core types.
 * Runs ALWAYS.
 * <p>
 * Part of DM 4.8.5
 */
public class Migration6 extends Migration {

    @Override
    public void run() {
        dm4.getTopicType("dm4.core.meta_type")
            .addIndexMode(IndexMode.KEY)
            .addIndexMode(IndexMode.FULLTEXT)
            .addIndexMode(IndexMode.FULLTEXT_KEY);
        dm4.getTopicType("dm4.core.topic_type")
            .addIndexMode(IndexMode.KEY)
            .addIndexMode(IndexMode.FULLTEXT)
            .addIndexMode(IndexMode.FULLTEXT_KEY);
        dm4.getTopicType("dm4.core.assoc_type")
            .addIndexMode(IndexMode.KEY)
            .addIndexMode(IndexMode.FULLTEXT)
            .addIndexMode(IndexMode.FULLTEXT_KEY);
        dm4.getTopicType("dm4.core.data_type")
            .addIndexMode(IndexMode.KEY)
            .addIndexMode(IndexMode.FULLTEXT)
            .addIndexMode(IndexMode.FULLTEXT_KEY);
        dm4.getTopicType("dm4.core.role_type")
            .addIndexMode(IndexMode.KEY)
            .addIndexMode(IndexMode.FULLTEXT)
            .addIndexMode(IndexMode.FULLTEXT_KEY);
        dm4.getTopicType("dm4.core.cardinality")
            .addIndexMode(IndexMode.KEY)
            .addIndexMode(IndexMode.FULLTEXT)
            .addIndexMode(IndexMode.FULLTEXT_KEY);
        dm4.getTopicType("dm4.core.plugin")
            .addIndexMode(IndexMode.KEY)
            .addIndexMode(IndexMode.FULLTEXT)
            .addIndexMode(IndexMode.FULLTEXT_KEY);
    }
}
