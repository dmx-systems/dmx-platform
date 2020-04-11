package systems.dmx.core.impl;

import systems.dmx.core.osgi.CoreActivator;
import systems.dmx.core.storage.spi.DMXStorage;
import systems.dmx.core.util.JavaUtils;

import org.junit.After;
import org.junit.Before;

import java.util.logging.Logger;



public class CoreServiceTestEnvironment {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String DATABASE_FACTORY = "systems.dmx.storage.neo4j.Neo4jStorageFactory";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected CoreServiceImpl dmx;      // accessed by test subclasses
    protected ModelFactoryImpl mf;      // accessed by test subclasses

    private DMXStorage db;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Before
    public void setup() {
        db = CoreActivator.openDB(DATABASE_FACTORY, JavaUtils.createTempDirectory("dmx-test-").getAbsolutePath());
        mf = db.getModelFactory();
        dmx = new CoreServiceImpl(new AccessLayer(db), null);     // bundleContext=null
    }

    @After
    public void shutdown() {
        // copy in CoreActivator.stop()
        if (dmx != null) {
            dmx.shutdown();
        }
        if (db != null) {
            logger.info("### Shutting down the database");
            db.shutdown();
        }
    }
}
