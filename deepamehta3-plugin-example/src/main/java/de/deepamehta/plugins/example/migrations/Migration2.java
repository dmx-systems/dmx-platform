package de.deepamehta.plugins.example.migrations;

import de.deepamehta.core.model.DataField;
import de.deepamehta.core.service.Migration;

public class Migration2 extends Migration {

    @Override
    public void run() {
        DataField descriptionField = new DataField("Description", "html");
        descriptionField.setUri("de/deepamehta/core/property/Description");
        descriptionField.setRendererClass("BodyTextRenderer");
        descriptionField.setIndexingMode("FULLTEXT");
        dms.addDataField("de/deepamehta/example/topictype/Example", descriptionField);
    }
}
