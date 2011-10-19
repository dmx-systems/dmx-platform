package de.deepamehta.plugins.geomaps;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ClientContext;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Plugin;

import java.util.logging.Logger;



public class GeomapsPlugin extends Plugin {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************************
    // *** Core Hooks (called from DeepaMehta 4 Core) ***
    // **************************************************



    @Override
    public void postCreateHook(Topic topic, ClientContext clientContext) {
        if (topic.getTypeUri().equals("dm4.contacts.address")) {
            logger.info("### Creating \"Address\" " + topic);
        }
    }

    @Override
    public void postUpdateHook(Topic topic, TopicModel oldTopic, Directives directives) {
        if (topic.getTypeUri().equals("dm4.contacts.address")) {
            logger.info("### Updating \"Address\" " + topic + " (old " + oldTopic + ")");
        }
    }
}
