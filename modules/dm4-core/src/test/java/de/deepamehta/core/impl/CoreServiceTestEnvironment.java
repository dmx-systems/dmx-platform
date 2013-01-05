package de.deepamehta.core.impl;

import de.deepamehta.core.util.JavaUtils;

import de.deepamehta.core.storage.spi.DeepaMehtaStorage;
import de.deepamehta.core.storage.spi.MehtaGraphFactory;

import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.logging.Logger;



public class CoreServiceTestEnvironment {

    // ------------------------------------------------------------------------------------------------------- Constants

    // ### TODO: drop this. Register as OSGi service instead.
    private static final String DATABASE_FACTORY = "de.deepamehta.storage.neo4j.Neo4jMehtaGraphFactory";
    // ### TODO: enable property access
    // System.getProperty("dm4.database.factory");

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected EmbeddedService dms;
    private File dbPath;

    protected Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Before
    public void setUp() {
        dbPath = JavaUtils.createTempDirectory("dm4-");
        DeepaMehtaStorage storage = accessStorage(dbPath.getAbsolutePath());
        dms = new EmbeddedService(new StorageDecorator(storage), null);
        dms.setupDB();
    }

    @After
    public void tearDown() {
        if (dms != null) {
            dms.shutdown();
        }
        dbPath.delete();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    // ### TODO: copied from CoreActivator
    private DeepaMehtaStorage accessStorage(String databasePath) {
        try {
            logger.info("Accessing the storage layer\n    databasePath=\"" + databasePath +
                "\"\n    databaseFactory=\"" + DATABASE_FACTORY + "\"");
            MehtaGraphFactory factory = (MehtaGraphFactory) Class.forName(DATABASE_FACTORY).newInstance();
            return factory.createInstance(databasePath);
        } catch (Exception e) {
            throw new RuntimeException("Accessing the storage layer failed (databasePath=\"" + databasePath +
                "\", databaseFactory=\"" + DATABASE_FACTORY + "\"", e);
        }
    }
}
