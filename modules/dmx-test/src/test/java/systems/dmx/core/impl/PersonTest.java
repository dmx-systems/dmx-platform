package systems.dmx.core.impl;

import static systems.dmx.contacts.Constants.*;
import static systems.dmx.core.Constants.*;
import static systems.dmx.datetime.Constants.*;
import systems.dmx.core.ChildTopics;
import systems.dmx.core.CompDef;
import systems.dmx.core.DMXObject;
import systems.dmx.core.DMXType;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.storage.spi.DMXTransaction;
import systems.dmx.core.util.DMXUtils;

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
import java.util.stream.Collectors;



public class PersonTest extends CoreServiceTestEnvironment {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ----------------------------------------------------------------------------------------------------------- Tests

    @Test
    public void labelRules() {
        DMXTransaction tx = dmx.beginTx();
        try {
            definePersonModel();
            Topic person = createPerson();
            // one Person and one Address exists
            List<Topic> persons   = dmx.getTopicsByType(PERSON);
            List<Topic> addresses = dmx.getTopicsByType(ADDRESS);
            assertEquals(1, persons.size());
            assertEquals(1, addresses.size());
            // labels are concatenated
            ChildTopics children = persons.get(0).getChildTopics();
            assertEquals("Dave Stauges", children.getString(PERSON_NAME));
            assertEquals("Parkstr. 3 13187 Berlin Germany",
                                         children.getTopics(ADDRESS + "#" + ADDRESS_ENTRY)
                                            .get(0).getSimpleValue().toString());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // --- Search ---

    @Test
    public void getTopicsByValue() {
        DMXTransaction tx = dmx.beginTx();
        try {
            definePersonModel();
            createPerson();
            // getTopicsByValue() retrieves by exact value.
            // Single word (like in fulltext search) is not supported.
            // Lucene query syntax (phrase, wildcards, escaping, ...) is not supported.
            // Search is case-sensitive.
            assertEquals(1, dmx.getTopicsByValue(STREET, new SimpleValue("Parkstr. 3")).size());
            assertEquals(0, dmx.getTopicsByValue(STREET, new SimpleValue("parkSTR. 3")).size());
            assertEquals(0, dmx.getTopicsByValue(STREET, new SimpleValue("Parkstr.\\ 3")).size());
            assertEquals(0, dmx.getTopicsByValue(STREET, new SimpleValue("\"Parkstr. 3\"")).size());
            assertEquals(0, dmx.getTopicsByValue(STREET, new SimpleValue("Parkstr.")).size());
            assertEquals(0, dmx.getTopicsByValue(STREET, new SimpleValue("Park*")).size());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void queryTopics() {
        DMXTransaction tx = dmx.beginTx();
        try {
            definePersonModel();
            createPerson();
            // Lucene query syntax (phrase, wildcards, escaping, ...) is supported.
            // Single word (like in fulltext search) is not supported. Spaces must be escaped.
            // Search is case-sensitive.
            assertEquals(0, dmx.queryTopics(STREET, "Parkstr. 3").size());
            assertEquals(1, dmx.queryTopics(STREET, "Parkstr.\\ 3").size());
            assertEquals(0, dmx.queryTopics(STREET, "parkSTR.\\ 3").size());
            assertEquals(1, dmx.queryTopics(STREET, "\"Parkstr. 3\"").size());
            assertEquals(0, dmx.queryTopics(STREET, "Parkstr.").size());
            assertEquals(1, dmx.queryTopics(STREET, "Park*").size());
            assertEquals(1, dmx.queryTopics(STREET, "Parkstr??3").size());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void queryTopicsFulltext() {
        DMXTransaction tx = dmx.beginTx();
        try {
            definePersonModel();
            createPerson();
            // Single words do match.
            // Lucene query syntax (phrase, wildcards, escaping, ...) is supported.
            // Note: by default search terms are combined by "OR" (Lucene default), not "AND".
            // Use "AND" (uppercase is required) or synonymous "&&" explicitly.
            // Search is case-insensitive.
            assertEquals(1, dmx.queryTopicsFulltext("Parkstr. 3",       STREET, false).topics.size());
            assertEquals(1, dmx.queryTopicsFulltext("Parkstr. XYZ",     STREET, false).topics.size());
            assertEquals(1, dmx.queryTopicsFulltext("Parkstr. OR XYZ",  STREET, false).topics.size());
            assertEquals(0, dmx.queryTopicsFulltext("Parkstr. AND XYZ", STREET, false).topics.size());
            assertEquals(0, dmx.queryTopicsFulltext("Parkstr. && XYZ",  STREET, false).topics.size());
            assertEquals(1, dmx.queryTopicsFulltext("Parkstr. and XYZ", STREET, false).topics.size());
            assertEquals(1, dmx.queryTopicsFulltext("3 AND Parkstr.",   STREET, false).topics.size());
            assertEquals(1, dmx.queryTopicsFulltext("XYZ Parkstr.",     STREET, false).topics.size());
            assertEquals(1, dmx.queryTopicsFulltext("Parkstr.\\ 3",     STREET, false).topics.size());
            assertEquals(1, dmx.queryTopicsFulltext("\"Parkstr. 3\"",   STREET, false).topics.size());
            assertEquals(1, dmx.queryTopicsFulltext("Parkstr.",         STREET, false).topics.size());
            assertEquals(1, dmx.queryTopicsFulltext("parkSTR.",         STREET, false).topics.size());
            assertEquals(1, dmx.queryTopicsFulltext("Park*",            STREET, false).topics.size());
            assertEquals(0, dmx.queryTopicsFulltext("Park",             STREET, false).topics.size());
            assertEquals(1, dmx.queryTopicsFulltext("Park????",         STREET, false).topics.size());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void getTopicByValueNullTypeUri() {
        DMXTransaction tx = dmx.beginTx();
        try {
            dmx.getTopicByValue(null, new SimpleValue("Parkstr. 3"));       // typeUri=null
            fail();
        } catch (RuntimeException e) {
            // is expected
        } finally {
            tx.finish();
        }
    }

    @Test
    public void getTopicsByValueNullTypeUri() {
        DMXTransaction tx = dmx.beginTx();
        try {
            dmx.getTopicsByValue(null, new SimpleValue("Parkstr. 3"));      // typeUri=null
            fail();
        } catch (RuntimeException e) {
            // is expected
        } finally {
            tx.finish();
        }
    }

    @Test
    public void queryTopicsNullTypeUri() {
        //DMXTransaction tx = dmx.beginTx();
        try {
            dmx.queryTopics(null, "Parkstr. 3");           // typeUri=null
            fail();
        } catch (RuntimeException e) {
            // is expected
        } finally {
            //tx.finish();
        }
    }

    @Test
    public void searchNumber() {
        DMXTransaction tx = dmx.beginTx();
        try {
            definePersonModel();
            createPerson();
            //
            assertEquals(1, dmx.getTopicsByValue(YEAR, new SimpleValue(1972)).size());
            assertEquals(1, dmx.getTopicsByValue(YEAR, new SimpleValue("1972")).size());
            assertEquals(0, dmx.getTopicsByValue(YEAR, new SimpleValue("19*")).size());
            assertEquals(1, dmx.queryTopics(YEAR, "1972").size());
            assertEquals(1, dmx.queryTopics(YEAR, "19*").size());
            assertEquals(0, dmx.queryTopics(YEAR, "19?").size());
            assertEquals(1, dmx.queryTopics(YEAR, "19??").size());
            assertEquals(1, dmx.queryTopicsFulltext("1972", YEAR, false).topics.size());
            assertEquals(1, dmx.queryTopicsFulltext("19*", YEAR, false).topics.size());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // --- Manipulation ---

    @Test
    public void immutability() {
        DMXTransaction tx = dmx.beginTx();
        try {
            definePersonModel();
            Topic person = createPerson();
            Topic address = person.getChildTopics().getTopics(ADDRESS + "#" + ADDRESS_ENTRY).get(0);
            // this looks like we override "Berlin" with "Hamburg"
            address.update(mf.newChildTopicsModel().set(CITY, "Hamburg"));
            // ... BUT the original address is unchanged
            assertEquals("Berlin", address.getChildTopics().getString(CITY));
            // ... and another Address topic has been created
            List<Topic> addrs = dmx.getTopicsByType(ADDRESS);
            assertEquals(2, addrs.size());
            Topic address2 = addrs.get(0).getId() == address.getId() ? addrs.get(1) : addrs.get(0);
            assertNotEquals(address.getId(), address2.getId());
            // there are 2 City topics now
            assertEquals(2, dmx.getTopicsByType(CITY).size());
            //
            assertEquals("Hamburg", address2.getChildTopics().getString(CITY));
            assertEquals(1, address2.getChildTopics().size());
            assertEquals(4, address.getChildTopics().size());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void mutateEntity() {
        DMXTransaction tx = dmx.beginTx();
        try {
            definePersonModel();
            // create person
            Topic person = createPerson();
            ChildTopics children = person.getChildTopics();
            assertEquals("<p>Software Developer</p>", children.getString(PERSON_DESCRIPTION));
            // change Person Description in-place
            person.update(mf.newChildTopicsModel().set(PERSON_DESCRIPTION, "<p>Cook</p>"));
            assertEquals("<p>Cook</p>", children.getString(PERSON_DESCRIPTION));
            // refetch ... there is still only 1 person in the DB (it was mutated in-place)
            List<Topic> persons = dmx.getTopicsByType(PERSON);
            assertEquals(1, persons.size());
            // no children are loaded yet
            person = persons.get(0);
            children = person.getChildTopics();
            assertEquals(0, children.size());
            // the Person Description has changed in-place
            assertEquals("<p>Cook</p>", children.getString(PERSON_DESCRIPTION));
            assertEquals(1, children.size());
            // the other children (Person Name, Birtday, Email Address, Address, Description) are still there
            person.loadChildTopics();
            assertEquals(5, children.size());
            // now there are 2 Person Description topics in the DB (the original one is not mutated/deleted)
            List<Topic> descriptions = dmx.getTopicsByType(PERSON_DESCRIPTION);
            assertEquals(2, descriptions.size());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void mutateEntityComposite() {
        DMXTransaction tx = dmx.beginTx();
        try {
            definePersonModel();
            // create Person
            Topic person = createPerson();
            ChildTopics children = person.getChildTopics();
            // change Last Name
            person.update(mf.newChildTopicsModel().set(PERSON_NAME, mf.newChildTopicsModel()
                .set(FIRST_NAME, "Dave")
                .set(LAST_NAME, "Habling")
            ));
            // name has changed in memory
            ChildTopics name = children.getChildTopics(PERSON_NAME);
            assertEquals("Dave", name.getString(FIRST_NAME));
            assertEquals("Habling", name.getString(LAST_NAME));
            // check DB content; refetch ...
            assertEquals(2, dmx.getTopicsByType(LAST_NAME).size());
            assertEquals(1, dmx.getTopicsByType(FIRST_NAME).size());
            List<Topic> persons = dmx.getTopicsByType(PERSON);
            assertEquals(1, persons.size());
            // name has changed in DB
            name = persons.get(0).getChildTopics().getChildTopics(PERSON_NAME);;
            assertEquals("Dave", name.getString(FIRST_NAME));
            assertEquals("Habling", name.getString(LAST_NAME));
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void mutateEntityCompositeWrong() {
        DMXTransaction tx = dmx.beginTx();
        try {
            definePersonModel();
            // create Person
            Topic person = createPerson();
            ChildTopics children = person.getChildTopics();
            // change Last Name -- DON'T DO IT THIS WAY (it produces unexpected result)
            children.getTopic(PERSON_NAME).update(
                mf.newChildTopicsModel().set(LAST_NAME, "Habling")
            );
            // last name is still unchanged
            assertEquals("Stauges", children.getChildTopics(PERSON_NAME)
                .getString(LAST_NAME));
            //
            // now there are 2 Person Name topics in the DB, the 2nd has only Last Name (no First Name)
            assertEquals(2, dmx.getTopicsByType(PERSON_NAME).size());
            assertEquals(2, dmx.getTopicsByType(LAST_NAME).size());
            assertEquals(1, dmx.getTopicsByType(FIRST_NAME).size());
            // Last Name "Habling" is assigned to a Person Name parent
            Topic lastName = dmx.getTopicByValue(LAST_NAME, new SimpleValue("Habling"));
            Topic personName = lastName.getRelatedTopic(COMPOSITION, CHILD, PARENT, PERSON_NAME);
            assertNotNull(personName);
            // ... which has no First Name (but only a Last Name)
            assertNull(personName.getChildTopics().getString(FIRST_NAME, null));
            assertEquals("Habling", personName.getChildTopics().getString(LAST_NAME));
            // ... and is not assigned to any Person
            Topic person2 = personName.getRelatedTopic(COMPOSITION, CHILD, PARENT, PERSON);
            assertNull(person2);
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void addEmailAddress() {
        DMXTransaction tx = dmx.beginTx();
        try {
            definePersonModel();
            // create Person
            Topic person = createPerson();
            ChildTopics children = person.getChildTopics();
            List<RelatedTopic> emailAddresses = children.getTopics(EMAIL_ADDRESS);
            assertEquals(1, emailAddresses.size());
            assertEquals("me@example.com", emailAddresses.get(0).getSimpleValue().toString());
            // add 2nd Email Address
            person.update(mf.newChildTopicsModel().add(EMAIL_ADDRESS, "me@example2.com"));
            //
            // check memory
            emailAddresses = children.getTopics(EMAIL_ADDRESS);
            assertEquals(2, emailAddresses.size());
            // check DB content; refetch ...
            List<Topic> persons = dmx.getTopicsByType(PERSON);
            assertEquals(1, persons.size());
            emailAddresses = persons.get(0).getChildTopics().getTopics(EMAIL_ADDRESS);
            List<String> eas = emailAddresses.stream().map(
                ea -> ea.getSimpleValue().toString()
            ).collect(Collectors.toList());
            assertEquals(2, eas.size());
            assertTrue(eas.contains("me@example.com"));
            assertTrue(eas.contains("me@example2.com"));
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void replaceEmailAddress() {
        DMXTransaction tx = dmx.beginTx();
        try {
            definePersonModel();
            // create Person
            Topic person = createPerson();
            ChildTopics children = person.getChildTopics();
            // add 2nd Email Address
            person.update(mf.newChildTopicsModel().add(EMAIL_ADDRESS, "me@example2.com"));
            // replace 1st Email Address
            List<? extends RelatedTopicModel> eams = person.getModel().getChildTopics().getTopics(EMAIL_ADDRESS);
            // Note: in order to re-use a retrieved-from-db model as an update model you have to clone the former
            RelatedTopicModel eam = DMXUtils.findByValue(new SimpleValue("me@example.com"), eams).clone();
            eam.setSimpleValue("me@example3.com");
            person.update(mf.newChildTopicsModel().add(EMAIL_ADDRESS, eam));
            //
            // check memory
            List<RelatedTopic> emailAddresses = children.getTopics(EMAIL_ADDRESS);
            assertEquals(2, emailAddresses.size());
            // check DB content; refetch ...
            List<Topic> persons = dmx.getTopicsByType(PERSON);
            assertEquals(1, persons.size());
            emailAddresses = persons.get(0).getChildTopics().getTopics(EMAIL_ADDRESS);
            List<String> eas = emailAddresses.stream().map(
                ea -> ea.getSimpleValue().toString()
            ).collect(Collectors.toList());
            assertEquals(2, eas.size());
            assertTrue(eas.contains("me@example2.com"));
            assertTrue(eas.contains("me@example3.com"));
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void removeEmailAddress() {
        DMXTransaction tx = dmx.beginTx();
        try {
            definePersonModel();
            // create Person
            Topic person = createPerson();
            ChildTopics children = person.getChildTopics();
            Topic ea1 = children.getTopics(EMAIL_ADDRESS).get(0);
            // add 2nd Email Address
            person.update(mf.newChildTopicsModel().add(EMAIL_ADDRESS, "me@example2.com"));
            // remove 1st Email Address
            person.update(mf.newChildTopicsModel().addDeletionRef(EMAIL_ADDRESS, ea1.getId()));
            //
            // check memory
            List<RelatedTopic> emailAddresses = children.getTopics(EMAIL_ADDRESS);
            assertEquals(1, emailAddresses.size());
            // check DB content; refetch ...
            List<Topic> persons = dmx.getTopicsByType(PERSON);
            assertEquals(1, persons.size());
            emailAddresses = persons.get(0).getChildTopics().getTopics(EMAIL_ADDRESS);
            assertEquals(1, emailAddresses.size());
            assertEquals("me@example2.com", emailAddresses.get(0).getSimpleValue().toString());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void definePersonModel() {
        defineDateModel();
        defineAddressModel();
        // Person Name
        dmx.createTopicType(mf.newTopicTypeModel(FIRST_NAME,  "First Name",  TEXT));
        dmx.createTopicType(mf.newTopicTypeModel(LAST_NAME,   "Last Name",   TEXT));
        dmx.createTopicType(mf.newTopicTypeModel(PERSON_NAME, "Person Name", VALUE)
          .addCompDef(mf.newCompDefModel(null, false, true, PERSON_NAME, FIRST_NAME, ONE))
          .addCompDef(mf.newCompDefModel(null, false, true, PERSON_NAME, LAST_NAME,  ONE))
        );
        // Person
        dmx.createAssocType(mf.newAssocTypeModel(DATE_OF_BIRTH,      "Date of Birth",      TEXT));
        dmx.createAssocType(mf.newAssocTypeModel(ADDRESS_ENTRY,      "Address Entry",      TEXT));
        dmx.createTopicType(mf.newTopicTypeModel(EMAIL_ADDRESS,      "Email Address",      TEXT));
        dmx.createTopicType(mf.newTopicTypeModel(PERSON_DESCRIPTION, "Person Description", HTML));
        dmx.createTopicType(mf.newTopicTypeModel(PERSON,             "Person",             ENTITY)
          .addCompDef(mf.newCompDefModel(null, true, false, PERSON, PERSON_NAME, ONE))
          .addCompDef(mf.newCompDefModel(DATE_OF_BIRTH, false, false,
                                         PERSON, DATE,               ONE))
          .addCompDef(mf.newCompDefModel(PERSON, EMAIL_ADDRESS,      MANY))
          .addCompDef(mf.newCompDefModel(ADDRESS_ENTRY, false, false,
                                         PERSON, ADDRESS,            MANY))
          .addCompDef(mf.newCompDefModel(PERSON, PERSON_DESCRIPTION, ONE))
        );
    }

    private void defineDateModel() {
        dmx.createTopicType(mf.newTopicTypeModel(MONTH, "Month",   NUMBER));
        dmx.createTopicType(mf.newTopicTypeModel(DAY,   "Day",     NUMBER));
        dmx.createTopicType(mf.newTopicTypeModel(YEAR,  "Year",    NUMBER));
        dmx.createTopicType(mf.newTopicTypeModel(DATE,  "Address", VALUE)
            .addCompDef(mf.newCompDefModel(null, false, true, DATE, MONTH, ONE))
            .addCompDef(mf.newCompDefModel(null, false, true, DATE, DAY,   ONE))
            .addCompDef(mf.newCompDefModel(null, false, true, DATE, YEAR,  ONE))
        );
    }

    private void defineAddressModel() {
        dmx.createTopicType(mf.newTopicTypeModel(STREET,      "Street",      TEXT));
        dmx.createTopicType(mf.newTopicTypeModel(POSTAL_CODE, "Postal Code", TEXT));
        dmx.createTopicType(mf.newTopicTypeModel(CITY,        "City",        TEXT));
        dmx.createTopicType(mf.newTopicTypeModel(COUNTRY,     "Country",     TEXT));
        dmx.createTopicType(mf.newTopicTypeModel(ADDRESS,     "Address",     VALUE)
            .addCompDef(mf.newCompDefModel(null, false, true, ADDRESS, STREET,      ONE))
            .addCompDef(mf.newCompDefModel(null, false, true, ADDRESS, POSTAL_CODE, ONE))
            .addCompDef(mf.newCompDefModel(null, false, true, ADDRESS, CITY,        ONE))
            .addCompDef(mf.newCompDefModel(null, false, true, ADDRESS, COUNTRY,     ONE))
        );
    }

    private Topic createPerson() {
        return dmx.createTopic(mf.newTopicModel(PERSON, mf.newChildTopicsModel()
            .set(PERSON_NAME, mf.newChildTopicsModel()
                .set(FIRST_NAME, "Dave")
                .set(LAST_NAME,  "Stauges"))
            .set(DATE + "#" + DATE_OF_BIRTH, mf.newChildTopicsModel()
                .set(MONTH,      5)  // May
                .set(DAY,        1)  // 1st
                .set(YEAR,       1972))
            .add(EMAIL_ADDRESS, "me@example.com")
            .add(ADDRESS + "#" + ADDRESS_ENTRY, mf.newChildTopicsModel()
                .set(STREET,      "Parkstr. 3")
                .set(POSTAL_CODE, "13187")
                .set(CITY,        "Berlin")
                .set(COUNTRY,     "Germany"))
            .set(PERSON_DESCRIPTION, "<p>Software Developer</p>")
        ));
    }
}
