package de.deepamehta.core.osgi;

import de.deepamehta.core.impl.service.EmbeddedService;
import de.deepamehta.core.impl.service.WebPublishingService;
import de.deepamehta.core.impl.storage.MGStorageBridge;
import de.deepamehta.core.service.DeepaMehtaService;

import de.deepamehta.mehtagraph.spi.MehtaGraph;
import de.deepamehta.mehtagraph.spi.MehtaGraphFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Map;
import java.util.logging.Logger;



public class CoreActivator implements BundleActivator {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String DATABASE_PATH    = System.getProperty("dm4.database.path");
    private static final String DATABASE_BUNDLE  = System.getProperty("dm4.database.bundle");
    private static final String DATABASE_FACTORY = System.getProperty("dm4.database.factory");

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private BundleContext context;
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
            this.context = context;
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

    // ### TODO: copy exists in CoreServiceTestEnvironment
    private MehtaGraph openDB() {
        try {
            logger.info("Instantiating the MehtaGraph storage engine\n    " +
                "dm4.database.path=\"" + DATABASE_PATH + "\"\n    " +
                "dm4.database.bundle=\"" + DATABASE_BUNDLE + "\"\n    " +
                "dm4.database.factory=\"" + DATABASE_FACTORY + "\"");
            // Note: we must load the factory class through the storage provider bundle's class loader
            Class factoryClass = getBundle(DATABASE_BUNDLE).loadClass(DATABASE_FACTORY);
            MehtaGraphFactory factory = (MehtaGraphFactory) factoryClass.newInstance();
            return factory.createInstance(DATABASE_PATH);
        } catch (Exception e) {
            throw new RuntimeException("Instantiating the MehtaGraph storage engine failed", e);
        }
    }

    private Bundle getBundle(String symbolicName) {
        for (Bundle bundle : context.getBundles()) {
            if (bundle.getSymbolicName().equals(symbolicName)) {
                return bundle;
            }
        }
        throw new RuntimeException("Bundle not found (symbolicName=\"" + symbolicName + "\")");
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
