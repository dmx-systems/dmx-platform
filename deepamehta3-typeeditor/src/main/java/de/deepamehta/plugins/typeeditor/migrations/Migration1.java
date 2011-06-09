package de.deepamehta.plugins.typeeditor.migrations;

import de.deepamehta.core.service.Migration;



public class Migration1 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        addTopicTypeSetting("dm3.core.topic_type", "js_page_renderer_class", "TopictypeRenderer");
    }
}
