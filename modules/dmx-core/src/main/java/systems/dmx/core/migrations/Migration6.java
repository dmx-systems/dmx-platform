package systems.dmx.core.migrations;

import systems.dmx.core.model.IndexMode;
import systems.dmx.core.service.Migration;



/**
 * Add index modes to Core types.
 * Runs ALWAYS.
 * <p>
 * Part of DMX 5.0
 */
public class Migration6 extends Migration {

    @Override
    public void run() {
        dmx.getTopicType("dmx.core.meta_type")
            .addIndexMode(IndexMode.KEY)
            .addIndexMode(IndexMode.FULLTEXT)
            .addIndexMode(IndexMode.FULLTEXT_KEY);
        dmx.getTopicType("dmx.core.topic_type")
            .addIndexMode(IndexMode.KEY)
            .addIndexMode(IndexMode.FULLTEXT)
            .addIndexMode(IndexMode.FULLTEXT_KEY);
        dmx.getTopicType("dmx.core.assoc_type")
            .addIndexMode(IndexMode.KEY)
            .addIndexMode(IndexMode.FULLTEXT)
            .addIndexMode(IndexMode.FULLTEXT_KEY);
        dmx.getTopicType("dmx.core.data_type")
            .addIndexMode(IndexMode.KEY)
            .addIndexMode(IndexMode.FULLTEXT)
            .addIndexMode(IndexMode.FULLTEXT_KEY);
        dmx.getTopicType("dmx.core.role_type")
            .addIndexMode(IndexMode.KEY)
            .addIndexMode(IndexMode.FULLTEXT)
            .addIndexMode(IndexMode.FULLTEXT_KEY);
        dmx.getTopicType("dmx.core.cardinality")
            .addIndexMode(IndexMode.KEY)
            .addIndexMode(IndexMode.FULLTEXT)
            .addIndexMode(IndexMode.FULLTEXT_KEY);
        dmx.getTopicType("dmx.core.plugin")
            .addIndexMode(IndexMode.KEY)
            .addIndexMode(IndexMode.FULLTEXT)
            .addIndexMode(IndexMode.FULLTEXT_KEY);
    }
}
