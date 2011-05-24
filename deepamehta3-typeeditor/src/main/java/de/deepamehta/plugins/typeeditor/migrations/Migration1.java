package de.deepamehta.plugins.typeeditor.migrations;

import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.model.ViewConfiguration;
import de.deepamehta.core.service.Migration;

import java.util.logging.Logger;



public class Migration1 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private final Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        ViewConfiguration viewConfig = dms.getTopicType("dm3.core.topic_type", null).getViewConfig();
        //
        Composite comp = new Composite("{dm3.webclient.js_renderer_class: \"FieldDefinitionRenderer\"}");
        viewConfig.addConfigTopic(new TopicModel("dm3.webclient.view_config", comp));
    }
}
