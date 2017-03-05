package de.deepamehta.typeeditor.migrations;

import de.deepamehta.core.service.Migration;



public class Migration2 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        setTopicTypeViewConfigValue("dm4.core.role_type", "page_renderer_uri", "dm4.typeeditor.roletype_renderer");
    }
}
