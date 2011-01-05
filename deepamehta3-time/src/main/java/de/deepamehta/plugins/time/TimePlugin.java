package de.deepamehta.plugins.time;

import de.deepamehta.core.model.DataField;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.service.Plugin;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;



public class TimePlugin extends Plugin {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************
    // *** Overriding Hooks ***
    // ************************



    /**
     * Sets a timestamp at every topic being created.
     */
    @Override
    public void preCreateHook(Topic topic, Map<String, String> clientContext) {
        logger.info("Setting timestamp of " + topic);
        long time = System.currentTimeMillis();
        TopicType type = dms.getTopicType(topic.typeUri, null);   // clientContext=null
        // Note: the timestamp data fields might be (interactively) removed meanwhile
        if (type.hasDataField("de/deepamehta/core/property/DateCreated")) {
            topic.setProperty("de/deepamehta/core/property/DateCreated", time);
        }
        if (type.hasDataField("de/deepamehta/core/property/DateModified")) {
            topic.setProperty("de/deepamehta/core/property/DateModified", time);
        }
    }

    /**
     * Updates the timestamp for every topic being modified.
     */
    @Override
    public void preUpdateHook(Topic topic, Map<String, Object> newProperties) {
        long time = System.currentTimeMillis();
        topic.setProperty("de/deepamehta/core/property/DateModified", time);
    }

    // ---

    @Override
    public void providePropertiesHook(Topic topic) {
        topic.setProperty("de/deepamehta/core/property/DateModified",
            dms.getTopicProperty(topic.id, "de/deepamehta/core/property/DateModified"));
    }

    // ---

    /**
     * Adds "Date Created" and "Date Modified" data fields to all topic types.
     */
    @Override
    public void modifyTopicTypeHook(TopicType topicType, Map<String, String> clientContext) {
        //
        DataField dateCreatedField = new DataField("Date Created", "number");
        dateCreatedField.setUri("de/deepamehta/core/property/DateCreated");
        dateCreatedField.setEditable(false);
        dateCreatedField.setRendererClass("TimestampFieldRenderer");
        dateCreatedField.setIndexingMode("FULLTEXT_KEY");
        //
        DataField dateModifiedField = new DataField("Date Modified", "number");
        dateModifiedField.setUri("de/deepamehta/core/property/DateModified");
        dateModifiedField.setEditable(false);
        dateModifiedField.setRendererClass("TimestampFieldRenderer");
        dateModifiedField.setIndexingMode("FULLTEXT_KEY");
        //
        topicType.addDataField(dateCreatedField);
        topicType.addDataField(dateModifiedField);
    }
}
