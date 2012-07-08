package de.deepamehta.plugins.iconpicker.migrations;

import de.deepamehta.core.service.Migration;



public class Migration1 extends Migration {

    @Override
    public void run() {
        addTopicTypeSetting("dm4.webclient.icon", "field_renderer_uri", "dm4.iconpicker.icon_field_renderer");
    }
}
