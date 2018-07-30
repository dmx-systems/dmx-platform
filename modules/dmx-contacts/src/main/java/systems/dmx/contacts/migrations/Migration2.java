package systems.dmx.contacts.migrations;

import systems.dmx.core.Topic;
import systems.dmx.core.model.AssociationDefinitionModel;
import systems.dmx.core.model.AssociationTypeModel;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.service.Migration;

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
        dmx.createAssociationType(mf.newAssociationTypeModel("dmx.contacts.phone_entry", "Phone Entry",
            "dmx.core.identity")
            .addAssocDef(mf.newAssociationDefinitionModel("dmx.core.aggregation_def",
            "dmx.contacts.phone_entry", "dmx.contacts.phone_label", "dmx.core.many", "dmx.core.one")));
        dmx.createAssociationType(mf.newAssociationTypeModel("dmx.contacts.address_entry", "Address Entry",
            "dmx.core.identity")
            .addAssocDef(mf.newAssociationDefinitionModel("dmx.core.aggregation_def",
            "dmx.contacts.address_entry", "dmx.contacts.address_label", "dmx.core.many", "dmx.core.one")));
        dmx.getTopicType("dmx.contacts.person")
            .addAssocDefBefore(
                mf.newAssociationDefinitionModel("dmx.core.composition_def", "dmx.contacts.phone_entry", false, false,
                "dmx.contacts.person", "dmx.contacts.phone_number", "dmx.core.one", "dmx.core.many"),
            "dmx.contacts.email_address")
            .addAssocDefBefore(
                mf.newAssociationDefinitionModel("dmx.core.composition_def", "dmx.contacts.address_entry", false, false,
                "dmx.contacts.person", "dmx.contacts.address", "dmx.core.one", "dmx.core.many"),
            "dmx.contacts.notes");
        dmx.getTopicType("dmx.contacts.institution")
            .addAssocDefBefore(
                mf.newAssociationDefinitionModel("dmx.core.composition_def", "dmx.contacts.phone_entry", false, false,
                "dmx.contacts.institution", "dmx.contacts.phone_number", "dmx.core.one", "dmx.core.many"),
            "dmx.contacts.email_address")
            .addAssocDefBefore(
                mf.newAssociationDefinitionModel("dmx.core.composition_def", "dmx.contacts.address_entry", false, false,
                "dmx.contacts.institution", "dmx.contacts.address", "dmx.core.one", "dmx.core.many"),
            "dmx.contacts.notes");
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
        for (Topic phoneEntry   : dmx.getTopicsByType("dmx.contacts.phone_entry"))   bufferPhoneEntry(phoneEntry);
        for (Topic addressEntry : dmx.getTopicsByType("dmx.contacts.address_entry")) bufferAddressEntry(addressEntry);
        //
        // 2) temporarily change entry types
        //
        // Note: we change comp_def to aggr_def to avoid deleting childs when deleting the entry topics (next step).
        // The childs are the Phone and Address topics we want keep and reassign later (while the actual conversion).
        dmx.getTopicType("dmx.contacts.phone_entry").getAssocDef("dmx.contacts.phone_number")
            .setTypeUri("dmx.core.aggregation_def");
        dmx.getTopicType("dmx.contacts.address_entry").getAssocDef("dmx.contacts.address")
            .setTypeUri("dmx.core.aggregation_def");
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
        dmx.deleteTopicType("dmx.contacts.phone_entry");
        dmx.deleteTopicType("dmx.contacts.address_entry");
    }

    // ---

    private void bufferPhoneEntry(Topic phoneEntry) {
        Topic parent = phoneEntry.getRelatedTopic("dmx.core.composition", "dmx.core.child", "dmx.core.parent", null);
        Topic phoneLabel  = phoneEntry.getChildTopics().getTopic("dmx.contacts.phone_label");
        Topic phoneNumber = phoneEntry.getChildTopics().getTopic("dmx.contacts.phone_number");
        phoneEntries.add(new Entry(phoneEntry, parent, phoneLabel.getId(), phoneNumber.getId()));
    }

    private void bufferAddressEntry(Topic addressEntry) {
        Topic parent = addressEntry.getRelatedTopic("dmx.core.composition", "dmx.core.child", "dmx.core.parent", null);
        Topic addressLabel = addressEntry.getChildTopics().getTopic("dmx.contacts.address_label");
        Topic address      = addressEntry.getChildTopics().getTopic("dmx.contacts.address");
        addressEntries.add(new Entry(addressEntry, parent, addressLabel.getId(), address.getId()));
    }

    // ---

    private void convertPhoneEntry(Entry entry) {
        entry.parent.getChildTopics().addRef("dmx.contacts.phone_number", entry.objectId, mf.newChildTopicsModel()
            .putRef("dmx.contacts.phone_label", entry.labelId));
    }

    private void convertAddressEntry(Entry entry) {
        entry.parent.getChildTopics().addRef("dmx.contacts.address", entry.objectId, mf.newChildTopicsModel()
            .putRef("dmx.contacts.address_label", entry.labelId));
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
