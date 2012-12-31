package de.deepamehta.core.impl.service;

import de.deepamehta.core.impl.storage.MGStorageBridge;
import de.deepamehta.core.util.JavaUtils;

import de.deepamehta.core.storage.spi.MehtaGraph;
import de.deepamehta.core.storage.spi.MehtaGraphFactory;

import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.logging.Logger;



public class CoreServiceTestEnvironment {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String DATABASE_FACTORY = "de.deepamehta.mehtagraph.neo4j.Neo4jMehtaGraphFactory";
    // ### TODO: enable property access
    // System.getProperty("dm4.database.factory");

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected EmbeddedService dms;
    private File dbPath;

    protected Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Before
    public void setup() {
        try {
            logger.info("Setting up test DB");
            dbPath = JavaUtils.createTempDirectory("dm4-");
            MehtaGraph mehtaGraph = openDB(dbPath.getAbsolutePath());
            dms = new EmbeddedService(new MGStorageBridge(mehtaGraph), null);
            dms.setupDB();
        } catch (Exception e) {
            throw new RuntimeException("Setting up test DB failed", e);
        }
    }

    @After
    public void tearDown() {
        dms.shutdown();
        dbPath.delete();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    // ### TODO: copied from CoreActivator
    private MehtaGraph openDB(String databasePath) {
        try {
            logger.info("Instantiating the MehtaGraph storage engine\n    databasePath=\"" + databasePath +
                "\"\n    databaseFactory=\"" + DATABASE_FACTORY + "\"");
            MehtaGraphFactory factory = (MehtaGraphFactory) Class.forName(DATABASE_FACTORY).newInstance();
            return factory.createInstance(databasePath);
        } catch (Exception e) {
            throw new RuntimeException("Instantiating the MehtaGraph storage engine failed", e);
        }
    }
}
