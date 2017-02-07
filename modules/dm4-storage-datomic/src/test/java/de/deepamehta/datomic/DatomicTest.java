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

import datomic.Entity;
import datomic.Peer;
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
        // Peer.shutdown() must NOT be called between running tests (in the same JVM instance).
        // Otherwise strange Datomic errors appear.
        /* if (storage != null) {
            storage.shutdown();
        } */
    }

    // --- Datomic API ---

    @Test
    public void deserialize() {
        assertSame(Symbol.class,  read("?e").getClass());
        assertSame(Keyword.class, read(":e").getClass());
    }

    @Test
    public void entity() {
        Entity entity = storage.entity(1234);
        assertNotNull(entity);
        assertEquals(0, entity.keySet().size());
    }

    @Test
    public void query() {
        Collection result = storage.query(list(read("?e")),
            list(list(read("?e"), ":dm4/entity-type", ":dm4.entity-type/topic")));
        assertEquals(0, result.size());
    }

    @Test
    public void queryString() {
        Collection result = storage.query("[:find ?e :where [?e :dm4/entity-type :dm4.entity-type/topic]]");
        assertEquals(0, result.size());
    }

    @Test
    public void storeEntity() {
        storage.storeEntity(":dm4/entity-type", ":dm4.entity-type/topic");
        //
        Collection result = storage.query(list(read("?e")),
            list(list(read("?e"), ":dm4/entity-type", ":dm4.entity-type/topic")));
        assertEquals(1, result.size());
    }

    @Test
    public void storeEntityAndQueryAsString() {
        storage.storeEntity(":dm4/entity-type", ":dm4.entity-type/topic");
        //
        Collection result = storage.query("[:find ?e :where [?e :dm4/entity-type :dm4.entity-type/topic]]");
        assertEquals(1, result.size());
    }

    @Test
    public void queryWithParameter() {
        storage.storeEntity(":dm4.object/uri", "dm4.test.uri");
        Collection result = storage.query("[:find ?e :in $ ?v :where [?e :dm4.object/uri ?v]]", "dm4.test.uri");
        assertEquals(1, result.size());
    }

    @Test
    public void typeMismatch() {
        try {
            storage.resolveTempId(storage.storeEntity(
                ":db/id", DatomicStorage.TEMP_ID,
                ":dm4.object/uri", 1234,    // type mismatch!
                ":dm4.object/type", "dm4.test.type_uri"));
            fail("IllegalArgumentException not thrown");
            // Note: exception is thrown only by resolveTempId(), not by storeEntity()
        } catch (Exception e) {
            Throwable cause = e.getCause().getCause();
            assertTrue(cause instanceof IllegalArgumentException);
            assertEquals(cause.getMessage(), ":db.error/wrong-type-for-attribute Value 1234 " +
                "is not a valid :string for attribute :dm4.object/uri");
        }
    }

    @Test
    public void typeMismatch2() {
        storage.storeEntity(
            ":db/id", DatomicStorage.TEMP_ID,
            ":dm4.object/uri", 1234,    // type mismatch!
            ":dm4.object/type", "dm4.test.type_uri"
        );
        Collection result = storage.query("[:find ?e :in $ ?v :where [?e :dm4.object/uri ?v]]", 1234);
        assertEquals(0, result.size());
        // Note: no error occurs; just the result is empty
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

    @Test
    @Ignore     // ### FIXME
    public void storeTopicProperty() {
        storage.storeTopicProperty(1234, ":dm4.test.prop_uri", "hello", false);
        storage.fetchProperty(1234, ":dm4.test.prop_uri");
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

}
