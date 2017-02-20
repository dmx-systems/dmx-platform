package de.deepamehta.core.impl;

import de.deepamehta.core.storage.spi.DeepaMehtaStorage;
import de.deepamehta.core.storage.spi.DeepaMehtaStorageFactory;

import org.junit.After;
import org.junit.Before;

import java.util.UUID;
import java.util.logging.Logger;



public class CoreServiceTestEnvironment {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String DATABASE_FACTORY = "de.deepamehta.datomic.DatomicStorageFactory";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    // providing the test subclasses access to the core service and logger
    protected CoreServiceImpl dm4;
    protected ModelFactoryImpl mf;

    protected Logger logger = Logger.getLogger(getClass().getName());

    private DeepaMehtaStorage storage;

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Before
    public void setup() {
        String databaseUri = "datomic:mem://dm4-test-" + UUID.randomUUID();
        mf = new ModelFactoryImpl();
        storage = openDB(databaseUri);
        dm4 = new CoreServiceImpl(new PersistenceLayer(storage), null);     // bundleContext=null
    }

    @After
    public void shutdown() {
        // Peer.shutdown() must NOT be called between running tests (in the same JVM instance).
        // Otherwise strange Datomic errors appear.
        /* if (storage != null) {
            storage.shutdown();
        } */
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private DeepaMehtaStorage openDB(String databaseUri) {
        try {
            logger.info("Instantiating the storage layer\n    databaseUri=\"" + databaseUri +
                "\"\n    databaseFactory=\"" + DATABASE_FACTORY + "\"");
            DeepaMehtaStorageFactory factory = (DeepaMehtaStorageFactory) Class.forName(DATABASE_FACTORY).newInstance();
            return factory.newDeepaMehtaStorage(databaseUri, mf);
        } catch (Exception e) {
            throw new RuntimeException("Instantiating the storage layer failed (databaseUri=\"" + databaseUri +
                "\", databaseFactory=\"" + DATABASE_FACTORY + "\"", e);
        }
    }
}
