package de.deepamehta.core.impl;

import de.deepamehta.core.util.JavaUtils;

import de.deepamehta.core.storage.spi.DeepaMehtaStorage;
import de.deepamehta.core.storage.spi.DeepaMehtaStorageFactory;

import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.logging.Logger;



public class CoreServiceTestEnvironment {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String DATABASE_FACTORY = "de.deepamehta.storage.neo4j.Neo4jStorageFactory";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    // providing the test subclasses access to the core service
    protected EmbeddedService dms;

    private DeepaMehtaStorage storage;
    private File dbPath;

    protected Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Before
    public void setup() {
        dbPath = JavaUtils.createTempDirectory("dm4-test-");
        storage = openDB(dbPath.getAbsolutePath());
        dms = new EmbeddedService(new StorageDecorator(storage), null);
    }

    @After
    public void shutdown() {
        if (storage != null) {
            storage.shutdown();
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private DeepaMehtaStorage openDB(String databasePath) {
        try {
            logger.info("Instantiating the storage layer\n    databasePath=\"" + databasePath +
                "\"\n    databaseFactory=\"" + DATABASE_FACTORY + "\"");
            DeepaMehtaStorageFactory factory = (DeepaMehtaStorageFactory) Class.forName(DATABASE_FACTORY).newInstance();
            return factory.createInstance(databasePath);
        } catch (Exception e) {
            throw new RuntimeException("Instantiating the storage layer failed (databasePath=\"" + databasePath +
                "\", databaseFactory=\"" + DATABASE_FACTORY + "\"", e);
        }
    }
}
