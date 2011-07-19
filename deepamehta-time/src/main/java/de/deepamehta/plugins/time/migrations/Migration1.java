package de.deepamehta.plugins.time.migrations;

import de.deepamehta.core.service.Migration;



/**
 * Distributed with Time plugin v0.4
 * Used then to add "Date Created" and "Date Modified" data fields to all existing topic types.
 * <p>
 * Note: emptied for Time plugin v0.4.2 (in conjunction with deepamehta3-core v0.4.3).
 * deepamehta3-core v0.4.3 introduced the MODIFY_TOPIC_TYPE hook to allow type modifications in a more consistent
 * way and without code doubling.
 */
public class Migration1 extends Migration {

    @Override
    public void run() {
    }
}
