package de.deepamehta.core.osgi;

import de.deepamehta.core.service.CoreService;
import de.deepamehta.core.service.impl.EmbeddedService;
import de.deepamehta.core.storage.impl.HGStorageBridge;

import de.deepamehta.hypergraph.HyperGraph;
import de.deepamehta.hypergraph.impl.Neo4jHyperGraph;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Map;
import java.util.logging.Logger;



public class Activator implements BundleActivator, FrameworkListener {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String DATABASE_PATH = System.getProperty("deepamehta3.database.path");

    // ------------------------------------------------------------------------------------------------- Class Variables

    private static CoreService dms;

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************
    // *** BundleActivator Implementation ***
    // **************************************



    @Override
    public void start(BundleContext context) {
        try {
            logger.info("========== Starting bundle \"DeepaMehta 3 Core\" ==========");
            dms = new EmbeddedService(new HGStorageBridge(openDB()));
            dms.setupDB();
            //
            logger.info("Registering DeepaMehta core service at OSGi framework");
            context.registerService(CoreService.class.getName(), dms, null);
            //
            context.addFrameworkListener(this);
        } catch (Exception e) {
            logger.severe("Activation of DeepaMehta core service failed:");
            e.printStackTrace();
            // Note: an exception thrown from here is swallowed by the container without reporting
            // and let File Install retry to start the bundle endlessly.
        }
    }

    @Override
    public void stop(BundleContext context) {
        logger.info("========== Stopping bundle \"DeepaMehta 3 Core\" ==========");
        if (dms != null) {
            dms.shutdown();
        }
    }



    // ****************************************
    // *** FrameworkListener Implementation ***
    // ****************************************



    @Override
    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.STARTED) {
            logger.info("########## OSGi framework STARTED ##########");
            dms.pluginsReady();
        }
    }



    // **************
    // *** Helper ***
    // **************



    public static CoreService getService() {
        // CoreService dms = (CoreService) deepamehtaServiceTracker.getService();
        if (dms == null) {
            throw new RuntimeException("DeepaMehta core service is not yet available");
        }
        return dms;
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private HyperGraph openDB() {
        GraphDatabaseService neo4j = null;
        try {
            neo4j = new EmbeddedGraphDatabase(DATABASE_PATH);
            return new Neo4jHyperGraph(neo4j);
        } catch (Exception e) {
            logger.info("Shutdown DB");
            if (neo4j != null) {
                neo4j.shutdown();
            }
            throw new RuntimeException("Opening database failed (path=" + DATABASE_PATH + ")", e);
        }
    }
}
