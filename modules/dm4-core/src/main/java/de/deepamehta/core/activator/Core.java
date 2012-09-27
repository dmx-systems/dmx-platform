package de.deepamehta.core.activator;

import de.deepamehta.core.impl.service.EmbeddedService;
import de.deepamehta.core.impl.service.WebPublishingService;
import de.deepamehta.core.impl.storage.MGStorageBridge;
import de.deepamehta.core.service.DeepaMehtaService;

import de.deepamehta.mehtagraph.MehtaGraph;
import de.deepamehta.mehtagraph.MehtaGraphFactory;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

import java.util.logging.Logger;



public class Core implements BundleActivator {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String DATABASE_PATH = System.getProperty("dm4.database.path");

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************
    // *** BundleActivator Implementation ***
    // **************************************



    @Override
    public void start(BundleContext context) {
        try {
            logger.info("========== Starting \"DeepaMehta 4 Core\" ==========");
            dms = new EmbeddedService(new MGStorageBridge(openDB()), context);
            dms.setupDB();
            //
            logger.info("Registering DeepaMehta 4 core service at OSGi framework");
            context.registerService(DeepaMehtaService.class.getName(), dms, null);
            //
            new HttpServiceTracker(context);
        } catch (Exception e) {
            logger.severe("Starting \"DeepaMehta 4 Core\" failed:");
            e.printStackTrace();
            // Note: we don't throw through the OSGi container here. It would not print out the stacktrace.
            // File Install would retry to start the bundle endlessly.
        }
    }

    @Override
    public void stop(BundleContext context) {
        try {
            logger.info("========== Stopping \"DeepaMehta 4 Core\" ==========");
            if (dms != null) {
                dms.shutdown();
            }
        } catch (Exception e) {
            logger.severe("Stopping \"DeepaMehta 4 Core\" failed:");
            e.printStackTrace();
            // Note: we don't throw through the OSGi container here. It would not print out the stacktrace.
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private MehtaGraph openDB() {
        try {
            logger.info("Creating database and indexing services (path=" + DATABASE_PATH + ")");
            return MehtaGraphFactory.createInstance(DATABASE_PATH);
        } catch (Exception e) {
            throw new RuntimeException("Opening database failed (path=" + DATABASE_PATH + ")", e);
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Classes

    private class HttpServiceTracker extends ServiceTracker {

        private HttpService httpService;

        private Logger logger = Logger.getLogger(getClass().getName());

        private HttpServiceTracker(BundleContext context) {
            super(context, HttpService.class.getName(), null);
            open();
        }

        @Override
        public Object addingService(ServiceReference serviceRef) {
            // ### TODO: should we catch exceptions here?
            // ### If HttpService is available on open() the exception is catched in start() -> OK.
            // ### If HttpService is not available on open() the exception is thrown through the OSGi container
            // ### and the stacktrace is not logged.
            Object service = super.addingService(serviceRef);
            if (service instanceof HttpService) {
                logger.info("Adding HTTP service to DeepaMehta 4 Core");
                httpService = (HttpService) service;
                //
                WebPublishingService wpService = new WebPublishingService(dms, httpService);
                logger.info("Registering Web Publishing service at OSGi framework");
                context.registerService(WebPublishingService.class.getName(), wpService, null);
            }
            return service;
        }

        @Override
        public void removedService(ServiceReference ref, Object service) {
            if (service == httpService) {
                logger.info("Removing HTTP service from DeepaMehta 4 Core");
                // ### TODO: unregister WebPublishingService
                httpService = null;
            }
            super.removedService(ref, service);
        }
    }
}
