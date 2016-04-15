package de.deepamehta.contacts.migrations;

import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.service.Migration;



/**
 * Adds "Date of Birth" to Person.
 * Runs ALWAYS.
 * <p>
 * Part of DM 4.8
 */
public class Migration4 extends Migration {

    @Override
    public void run() {
        dm4.getTopicType("dm4.contacts.person")
            .addAssocDefBefore(
                mf.newAssociationDefinitionModel("dm4.core.composition_def", "dm4.contacts.date_of_birth",
                "dm4.contacts.person", "dm4.datetime.date", "dm4.core.one", "dm4.core.one"),
            "dm4.contacts.phone_number#dm4.contacts.phone_entry");
    }
}
