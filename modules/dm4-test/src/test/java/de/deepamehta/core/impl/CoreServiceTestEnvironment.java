package de.deepamehta.core.impl;

import de.deepamehta.core.util.JavaUtils;

import de.deepamehta.core.service.CoreService;
import de.deepamehta.core.service.ModelFactory;
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

    // providing the test subclasses access to the core service and logger
    protected CoreServiceImpl dm4;
    protected ModelFactoryImpl mf;

    protected Logger logger = Logger.getLogger(getClass().getName());

    private DeepaMehtaStorage storage;
    private File dbPath;

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Before
    public void setup() {
        dbPath = JavaUtils.createTempDirectory("dm4-test-");
        mf = new ModelFactoryImpl();
        storage = openDB(dbPath.getAbsolutePath());
        dm4 = new CoreServiceImpl(new PersistenceLayer(storage), null);     // bundleContext=null
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
            return factory.newDeepaMehtaStorage(databasePath, mf);
        } catch (Exception e) {
            throw new RuntimeException("Instantiating the storage layer failed (databasePath=\"" + databasePath +
                "\", databaseFactory=\"" + DATABASE_FACTORY + "\"", e);
        }
    }
}
