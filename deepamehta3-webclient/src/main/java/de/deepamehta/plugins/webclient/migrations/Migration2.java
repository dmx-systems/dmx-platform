package de.deepamehta.plugins.webclient.migrations;

import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.service.Migration;

import java.util.logging.Logger;



public class Migration2 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private final Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        addIconToType("dm3.core.topic_type",  "box-blue.png");
        addIconToType("dm3.core.assoc_type",  "box-red.png");
        addIconToType("dm3.core.data_type",   "box-green.png");
        addIconToType("dm3.core.cardinality", "box-yellow.png");
        addIconToType("dm3.core.index_mode",  "box-orange.png");
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void addIconToType(String topicTypeUri, String iconfile) {
        dms.getTopicType(topicTypeUri, null).getViewConfig().addSetting("dm3.webclient.view_config",
            "dm3.webclient.icon_src", "/de.deepamehta.3-webclient/images/" + iconfile);
    }
}
