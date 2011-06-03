package de.deepamehta.plugins.iconpicker;

import de.deepamehta.core.model.DataField;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.service.Plugin;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;



public class IconPickerPlugin extends Plugin {

    private Logger logger = Logger.getLogger(getClass().getName());



    // ************************
    // *** Overriding Hooks ***
    // ************************



    @Override
    public void providePropertiesHook(Topic topic) {
        if (topic.typeUri.equals("de/deepamehta/core/topictype/Icon")) {
            topic.setProperty("de/deepamehta/core/property/IconSource",
                dms.getTopicProperty(topic.id, "de/deepamehta/core/property/IconSource"));
        }
    }
}
