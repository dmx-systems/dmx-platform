package de.deepamehta.storage.neo4j;

import de.deepamehta.core.storage.spi.DeepaMehtaStorage;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.logging.Level;
import java.util.logging.Logger;



public class Neo4jStorageActivator implements BundleActivator {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String DATABASE_PATH = System.getProperty("dm4.database.path");

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DeepaMehtaStorage storage;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************
    // *** BundleActivator Implementation ***
    // **************************************



    @Override
    public void start(BundleContext context) {
        try {
            logger.info("========== Starting \"DeepaMehta 4 Storage - Neo4j\" ==========");
            storage = new Neo4jStorage(DATABASE_PATH);
            //
            logger.info("Registering DeepaMehta 4 storage service - Neo4j - at OSGi framework");
            context.registerService(DeepaMehtaStorage.class.getName(), storage, null);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Starting \"DeepaMehta 4 Storage - Neo4j\" failed", e);
            // Note: we don't throw through the OSGi container here. It would not print out the stacktrace.
            // File Install would retry to start the bundle endlessly.
        }
    }

    @Override
    public void stop(BundleContext context) {
        try {
            logger.info("========== Stopping \"DeepaMehta 4 Storage - Neo4j\" ==========");
            if (storage != null) {
                storage.shutdown();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Stopping \"DeepaMehta 4 Storage - Neo4j\" failed", e);
            // Note: we don't throw through the OSGi container here. It would not print out the stacktrace.
        }
    }
}
