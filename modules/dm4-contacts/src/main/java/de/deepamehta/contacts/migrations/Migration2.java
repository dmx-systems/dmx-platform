package de.deepamehta.contacts.migrations;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.service.Migration;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;



/**
 * Changes Contacts model and converts content.
 * Runs only in UPDATE mode.
 * <p>
 * Part of DM 4.6
 */
public class Migration2 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private List<Entry> phoneEntries = new ArrayList();
    private List<Entry> addressEntries = new ArrayList();

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        logger.info("########## Converting Phone Entry and Address Entry topics");
        //
        // 1) prepare
        //
        bufferContentAndDeleteTypes();
        //
        // 2) change model
        //
        dm4.createAssociationType(mf.newAssociationTypeModel("dm4.contacts.phone_entry", "Phone Entry",
            "dm4.core.identity")
            .addAssocDef(mf.newAssociationDefinitionModel("dm4.core.aggregation_def",
            "dm4.contacts.phone_entry", "dm4.contacts.phone_label", "dm4.core.many", "dm4.core.one")));
        dm4.createAssociationType(mf.newAssociationTypeModel("dm4.contacts.address_entry", "Address Entry",
            "dm4.core.identity")
            .addAssocDef(mf.newAssociationDefinitionModel("dm4.core.aggregation_def",
            "dm4.contacts.address_entry", "dm4.contacts.address_label", "dm4.core.many", "dm4.core.one")));
        dm4.getTopicType("dm4.contacts.person")
            .addAssocDefBefore(
                mf.newAssociationDefinitionModel("dm4.core.composition_def", "dm4.contacts.phone_entry", false, false,
                "dm4.contacts.person", "dm4.contacts.phone_number", "dm4.core.one", "dm4.core.many"),
            "dm4.contacts.email_address")
            .addAssocDefBefore(
                mf.newAssociationDefinitionModel("dm4.core.composition_def", "dm4.contacts.address_entry", false, false,
                "dm4.contacts.person", "dm4.contacts.address", "dm4.core.one", "dm4.core.many"),
            "dm4.contacts.notes");
        dm4.getTopicType("dm4.contacts.institution")
            .addAssocDefBefore(
                mf.newAssociationDefinitionModel("dm4.core.composition_def", "dm4.contacts.phone_entry", false, false,
                "dm4.contacts.institution", "dm4.contacts.phone_number", "dm4.core.one", "dm4.core.many"),
            "dm4.contacts.email_address")
            .addAssocDefBefore(
                mf.newAssociationDefinitionModel("dm4.core.composition_def", "dm4.contacts.address_entry", false, false,
                "dm4.contacts.institution", "dm4.contacts.address", "dm4.core.one", "dm4.core.many"),
            "dm4.contacts.notes");
        //
        // 3) convert content
        //
        for (Entry entry : phoneEntries)   convertPhoneEntry(entry);
        for (Entry entry : addressEntries) convertAddressEntry(entry);
        //
        logger.info("########## Converting Phone Entry and Address Entry topics complete\n    " +
            "Phone entries converted: " + phoneEntries.size() + "\n    " +
            "Address entries converted: " + addressEntries.size());
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void bufferContentAndDeleteTypes() {
        //
        // 1) buffer entry topic content in memory
        //
        // Note: the actual conversion (as performed later) relies on the buffered content
        for (Topic phoneEntry   : dm4.getTopicsByType("dm4.contacts.phone_entry"))   bufferPhoneEntry(phoneEntry);
        for (Topic addressEntry : dm4.getTopicsByType("dm4.contacts.address_entry")) bufferAddressEntry(addressEntry);
        //
        // 2) temporarily change entry types
        //
        // Note: we change comp_def to aggr_def to avoid deleting childs when deleting the entry topics (next step).
        // The childs are the Phone and Address topics we want keep and reassign later (while the actual conversion).
        dm4.getTopicType("dm4.contacts.phone_entry").getAssocDef("dm4.contacts.phone_number")
            .setTypeUri("dm4.core.aggregation_def");
        dm4.getTopicType("dm4.contacts.address_entry").getAssocDef("dm4.contacts.address")
            .setTypeUri("dm4.core.aggregation_def");
        //
        // 3) delete entry topics
        //
        // Note: deleting the entry types (next step) requires to delete all instances before.
        for (Entry entry : phoneEntries)   entry.topic.delete();
        for (Entry entry : addressEntries) entry.topic.delete();
        //
        // 4) delete entry types
        //
        // Note: the entry topic types must be deleted as they are recreated as association types with the same URI.
        dm4.deleteTopicType("dm4.contacts.phone_entry");
        dm4.deleteTopicType("dm4.contacts.address_entry");
    }

    // ---

    private void bufferPhoneEntry(Topic phoneEntry) {
        Topic parent = phoneEntry.getRelatedTopic("dm4.core.composition", "dm4.core.child", "dm4.core.parent", null);
        Topic phoneLabel  = phoneEntry.getChildTopics().getTopic("dm4.contacts.phone_label");
        Topic phoneNumber = phoneEntry.getChildTopics().getTopic("dm4.contacts.phone_number");
        phoneEntries.add(new Entry(phoneEntry, parent, phoneLabel.getId(), phoneNumber.getId()));
    }

    private void bufferAddressEntry(Topic addressEntry) {
        Topic parent = addressEntry.getRelatedTopic("dm4.core.composition", "dm4.core.child", "dm4.core.parent", null);
        Topic addressLabel = addressEntry.getChildTopics().getTopic("dm4.contacts.address_label");
        Topic address      = addressEntry.getChildTopics().getTopic("dm4.contacts.address");
        addressEntries.add(new Entry(addressEntry, parent, addressLabel.getId(), address.getId()));
    }

    // ---

    private void convertPhoneEntry(Entry entry) {
        entry.parent.getChildTopics().addRef("dm4.contacts.phone_number", entry.objectId, mf.newChildTopicsModel()
            .putRef("dm4.contacts.phone_label", entry.labelId));
    }

    private void convertAddressEntry(Entry entry) {
        entry.parent.getChildTopics().addRef("dm4.contacts.address", entry.objectId, mf.newChildTopicsModel()
            .putRef("dm4.contacts.address_label", entry.labelId));
    }

    // ---

    private class Entry {

        private Topic topic;
        private Topic parent;
        private long labelId;
        private long objectId;

        private Entry(Topic topic, Topic parent, long labelId, long objectId) {
            this.topic = topic;
            this.parent = parent;
            this.labelId = labelId;
            this.objectId = objectId;
        }
    }
}
