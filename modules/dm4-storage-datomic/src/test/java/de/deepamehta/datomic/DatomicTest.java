package de.deepamehta.datomic;

import de.deepamehta.core.impl.ModelFactoryImpl;
import de.deepamehta.core.impl.PersistenceLayer;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.ModelFactory;
import de.deepamehta.core.storage.spi.DeepaMehtaStorage;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
import de.deepamehta.core.util.JavaUtils;

import static datomic.Util.list;
import static datomic.Util.read;

import clojure.lang.Keyword;
import clojure.lang.Symbol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import static java.util.Arrays.asList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;



public class DatomicTest {

    private DatomicStorage storage;
    private ModelFactoryImpl mf;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Before
    public void setup() {
        String databaseUri = "datomic:mem://dm4-test-" + UUID.randomUUID();
        mf = new ModelFactoryImpl();
        storage = new DatomicStorage(databaseUri, mf);
        storage.installSchema();
        new PersistenceLayer(storage);  // Note: the ModelFactory doesn't work when no PersistenceLayer is created
    }

    @After
    public void shutdown() {
        if (storage != null) {
            storage.shutdown();
        }
    }

    // --- Datomic API ---

    @Test
    public void deserialize() {
        assertSame(Symbol.class,  read("?e").getClass());
        assertSame(Keyword.class, read(":e").getClass());
    }

    @Test
    public void query() {
        Collection result = storage.query(list(read("?e")), list(read("$")),
            list(list(read("?e"), ":dm4/object-type", ":dm4.object-type/topic")));
        assertEquals(0, result.size());
    }

    @Test
    public void queryString() {
        Collection result = storage.query("[:find ?e :where [?e :dm4/object-type :dm4.object-type/topic]]");
        assertEquals(0, result.size());
    }

    @Test
    public void storeEntity() {
        storage.storeEntity(":dm4/object-type", ":dm4.object-type/topic");
        //
        Collection result = storage.query(list(read("?e")), list(read("$")),
            list(list(read("?e"), ":dm4/object-type", ":dm4.object-type/topic")));
        assertEquals(1, result.size());
    }

    @Test
    public void storeEntityAndQueryAsString() {
        storage.storeEntity(":dm4/object-type", ":dm4.object-type/topic");
        //
        Collection result = storage.query("[:find ?e :where [?e :dm4/object-type :dm4.object-type/topic]]");
        assertEquals(1, result.size());
    }

    // --- DeepaMehtaStorage ---

    @Test
    public void storeTopic() {
        TopicModel topic = mf.newTopicModel("dm4.test.type_uri");
        assertEquals(-1, topic.getId());
        storage.storeTopic(topic);
        assertTrue(topic.getId() != -1);
    }

    @Test
    public void uriUniqueness() {
        try {
            TopicModel topic = mf.newTopicModel("dm4.test.uri", "dm4.test.type_uri");
            storage.storeTopic(topic);
            storage.storeTopic(topic);
            fail("\"URI not unique\" exception not thrown");
        } catch (Exception e) {
            assertEquals("URI \"dm4.test.uri\" is not unique", e.getMessage());
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

}
