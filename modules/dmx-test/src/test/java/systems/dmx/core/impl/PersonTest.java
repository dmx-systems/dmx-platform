package systems.dmx.core.impl;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.ChildTopics;
import systems.dmx.core.CompDef;
import systems.dmx.core.DMXObject;
import systems.dmx.core.DMXType;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.storage.spi.DMXTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;

import static java.util.Arrays.asList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;



public class PersonTest extends CoreServiceTestEnvironment {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ----------------------------------------------------------------------------------------------------------- Tests

    @Test
    public void addressLabel() {
        DMXTransaction tx = dmx.beginTx();
        try {
            defineAddressModel();
            Topic address = createAddress();
            // one address exists
            assertEquals(1, dmx.getTopicsByType("dmx.contacts.address").size());
            // address label concatenates all fields
            assertEquals("Parkstr. 3 13187 Berlin Germany", address.getSimpleValue().toString());
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void immutability() {
        DMXTransaction tx = dmx.beginTx();
        try {
            defineAddressModel();
            Topic address = createAddress();
            // this looks like we override "Berlin" with "Hamburg"
            address.getChildTopics().set("dmx.contacts.city", "Hamburg");
            // ... BUT the original address is unchanged
            assertEquals("Berlin", address.getChildTopics().getString("dmx.contacts.city"));
            // ... and another Address topic has been created
            List<Topic> addrs = dmx.getTopicsByType("dmx.contacts.address");
            assertEquals(2, addrs.size());
            Topic address2 = addrs.get(0).getId() == address.getId() ? addrs.get(1) : addrs.get(0);
            assertNotEquals(address.getId(), address2.getId());
            // there are 2 City topics now
            assertEquals(2, dmx.getTopicsByType("dmx.contacts.city").size());
            //
            assertEquals("Hamburg", address2.getChildTopics().getString("dmx.contacts.city"));
            assertEquals(1, address2.getChildTopics().size());
            assertEquals(4, address.getChildTopics().size());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void defineAddressModel() {
        dmx.createTopicType(mf.newTopicTypeModel("dmx.contacts.street",      "Street",      TEXT));
        dmx.createTopicType(mf.newTopicTypeModel("dmx.contacts.postal_code", "Postal Code", TEXT));
        dmx.createTopicType(mf.newTopicTypeModel("dmx.contacts.city",        "City",        TEXT));
        dmx.createTopicType(mf.newTopicTypeModel("dmx.contacts.country",     "Country",     TEXT));
        dmx.createTopicType(mf.newTopicTypeModel("dmx.contacts.address", "Address", VALUE)
            .addCompDef(mf.newCompDefModel(null, false, true, "dmx.contacts.address", "dmx.contacts.street",      ONE))
            .addCompDef(mf.newCompDefModel(null, false, true, "dmx.contacts.address", "dmx.contacts.postal_code", ONE))
            .addCompDef(mf.newCompDefModel(null, false, true, "dmx.contacts.address", "dmx.contacts.city",        ONE))
            .addCompDef(mf.newCompDefModel(null, false, true, "dmx.contacts.address", "dmx.contacts.country",     ONE))
        );
    }

    private Topic createAddress() {
        return dmx.createTopic(mf.newTopicModel("dmx.contacts.address", mf.newChildTopicsModel()
            .set("dmx.contacts.street",      "Parkstr. 3")
            .set("dmx.contacts.postal_code", "13187")
            .set("dmx.contacts.city",        "Berlin")
            .set("dmx.contacts.country",     "Germany")
        ));
    }
}
