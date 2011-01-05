package de.deepamehta.plugins.contacts.migrations;

import de.deepamehta.core.model.DataField;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.service.Migration;

import java.util.logging.Logger;



/**
 * Repairs data field definitions which were broken in deepamehta3-contacts v0.4.
 * <p>
 * Note: this migration is distributed with deepamehta3-contacts v0.4.1. and is set to run only
 * when updating from deepamehta3-contacts v0.4 to v0.4.1 (see migration2.properties).
 */
public class Migration2 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private final Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        // update the indexers
        TopicType personType = dms.getTopicType("de/deepamehta/core/topictype/Person", null);   // clientContext=null
        // Note: these 5 properties are shared between "Person" and "Institution" topic types.
        // We go only through the Person type here. So, the properties are up-to-date in DB,
        // but not in the type cache. Workaround: after updating restart DeepaMehta.
        DataField phoneField   = personType.getDataField("de/deepamehta/core/property/Phone");
        DataField emailField   = personType.getDataField("de/deepamehta/core/property/Email");
        DataField websiteField = personType.getDataField("de/deepamehta/core/property/Website");
        DataField addressField = personType.getDataField("de/deepamehta/core/property/Address");
        DataField notesField   = personType.getDataField("de/deepamehta/core/property/Notes");
        phoneField.setIndexingMode("FULLTEXT");
        emailField.setIndexingMode("FULLTEXT");
        websiteField.setIndexingMode("FULLTEXT");
        addressField.setIndexingMode("FULLTEXT");
        notesField.setIndexingMode("FULLTEXT");
    }
}
