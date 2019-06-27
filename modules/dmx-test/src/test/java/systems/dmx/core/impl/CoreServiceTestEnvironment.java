package systems.dmx.core.impl;

import systems.dmx.core.util.JavaUtils;

import systems.dmx.core.service.CoreService;
import systems.dmx.core.service.ModelFactory;
import systems.dmx.core.storage.spi.DMXStorage;
import systems.dmx.core.storage.spi.DMXStorageFactory;

import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.logging.Logger;



public class CoreServiceTestEnvironment {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String DATABASE_FACTORY = "systems.dmx.storage.neo4j.Neo4jStorageFactory";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    // providing the test subclasses access to the core service and logger
    protected CoreServiceImpl dmx;
    protected ModelFactoryImpl mf;

    protected Logger logger = Logger.getLogger(getClass().getName());

    private DMXStorage db;
    private File dbPath;

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Before
    public void setup() {
        dbPath = JavaUtils.createTempDirectory("dmx-test-");
        mf = new ModelFactoryImpl();
        db = openDB(dbPath.getAbsolutePath());
        dmx = new CoreServiceImpl(new AccessLayer(db), null);     // bundleContext=null
    }

    @After
    public void shutdown() {
        if (db != null) {
            logger.info("### Shutting down the database");
            db.shutdown();
        }
        dmx.shutdown();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    // copy in CoreActivator (dmx-core)
    private DMXStorage openDB(String databasePath) {
        try {
            logger.info("##### Opening the database #####\n  databaseFactory=\"" + DATABASE_FACTORY +
                "\"\n  databasePath=\"" + databasePath + "\"");
            DMXStorageFactory factory = (DMXStorageFactory) Class.forName(DATABASE_FACTORY).newInstance();
            DMXStorage db = factory.newDMXStorage(databasePath, mf);
            logger.info("### Database opened successfully");
            return db;
        } catch (Exception e) {
            throw new RuntimeException("Opening the database failed, databaseFactory=\"" + DATABASE_FACTORY +
                "\", databasePath=\"" + databasePath + "\"", e);
        }
    }
}
