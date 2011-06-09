package de.deepamehta.plugins.webclient.migrations;

import de.deepamehta.core.service.Migration;



public class Migration2 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        addIconToTopicType("dm3.core.topic_type",  "box-blue.png");
        addIconToTopicType("dm3.core.assoc_type",  "box-red.png");
        addIconToTopicType("dm3.core.data_type",   "box-green.png");
        addIconToTopicType("dm3.core.cardinality", "box-yellow.png");
        addIconToTopicType("dm3.core.index_mode",  "box-orange.png");
        addIconToTopicType("dm3.core.meta_type",   "box-grey.png");
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void addIconToTopicType(String topicTypeUri, String iconfile) {
        addTopicTypeSetting(topicTypeUri, "icon_src", "/de.deepamehta.3-webclient/images/" + iconfile);
    }
}
