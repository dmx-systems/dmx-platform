package de.deepamehta.core.osgi;

import de.deepamehta.core.impl.EmbeddedService;
import de.deepamehta.core.impl.StorageDecorator;
import de.deepamehta.core.impl.WebPublishingService;
import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.storage.spi.DeepaMehtaStorage;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

import java.util.logging.Level;
import java.util.logging.Logger;



public class CoreActivator implements BundleActivator {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String DATABASE_PATH    = System.getProperty("dm4.database.path");

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private BundleContext bundleContext;

    // consumed services
    private DeepaMehtaStorage storageService;
    private HttpService httpService;

    private ServiceTracker storageServiceTracker;
    private ServiceTracker httpServiceTracker;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************
    // *** BundleActivator Implementation ***
    // **************************************



    @Override
    public void start(BundleContext bundleContext) {
        try {
            logger.info("========== Starting \"DeepaMehta 4 Core\" ==========");
            this.bundleContext = bundleContext;
            //
            storageServiceTracker = createServiceTracker(DeepaMehtaStorage.class);
            httpServiceTracker = createServiceTracker(HttpService.class);
            //
            storageServiceTracker.open();
            httpServiceTracker.open();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Starting \"DeepaMehta 4 Core\" failed", e);
            // Note: we don't throw through the OSGi container here. It would not print out the stacktrace.
            // File Install would retry to start the bundle endlessly.
        }
    }

    @Override
    public void stop(BundleContext bundleContext) {
        try {
            logger.info("========== Stopping \"DeepaMehta 4 Core\" ==========");
            storageServiceTracker.close();
            httpServiceTracker.close();
            //
            // Note: we do not shutdown the DB here.
            // The DB shuts down itself through the storage bundle's stop() method.
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Stopping \"DeepaMehta 4 Core\" failed", e);
            // Note: we don't throw through the OSGi container here. It would not print out the stacktrace.
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private ServiceTracker createServiceTracker(final Class serviceInterface) {
        //
        return new ServiceTracker(bundleContext, serviceInterface.getName(), null) {

            @Override
            public Object addingService(ServiceReference serviceRef) {
                // ### TODO: should we catch exceptions here?
                // ### If HttpService is available on open() the exception is catched in start() -> OK.
                // ### If HttpService is not available on open() the exception is thrown through the OSGi container
                // ### and the stacktrace is not logged.
                Object service = null;
                try {
                    service = super.addingService(serviceRef);
                    addService(service);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Adding service " + serviceInterface.getName() +
                        " to \"DeepaMehta 4 Core\" failed", e);
                    // Note: we don't throw through the OSGi container here. It would not print out the stacktrace.
                }
                return service;
            }

            @Override
            public void removedService(ServiceReference ref, Object service) {
                try {
                    removeService(service);
                    super.removedService(ref, service);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Removing service " + serviceInterface.getName() +
                        " from \"DeepaMehta 4 Core\" failed", e);
                    // Note: we don't throw through the OSGi container here. It would not print out the stacktrace.
                }
            }
        };
    }

    // ---

    private void addService(Object service) {
        if (service instanceof DeepaMehtaStorage) {
            logger.info("Adding storage service to DeepaMehta 4 Core");
            storageService = (DeepaMehtaStorage) service;
            checkRequirementsForActivation();
        } else if (service instanceof HttpService) {
            logger.info("Adding HTTP service to DeepaMehta 4 Core");
            httpService = (HttpService) service;
            checkRequirementsForActivation();
        }
    }

    private void removeService(Object service) {
        if (service == storageService) {
            logger.info("Removing storage service from DeepaMehta 4 Core");
            storageService = null;
        } else if (service == httpService) {
            logger.info("Removing HTTP service from DeepaMehta 4 Core");
            // ### TODO: unregister WebPublishingService
            httpService = null;
        }
    }

    // ---

    private void checkRequirementsForActivation() {
        if (storageService != null && httpService != null) {
            EmbeddedService dms = new EmbeddedService(new StorageDecorator(storageService), bundleContext);
            logger.info("Registering DeepaMehta 4 core service at OSGi framework");
            bundleContext.registerService(DeepaMehtaService.class.getName(), dms, null);
            //
            WebPublishingService wpService = new WebPublishingService(dms, httpService);
            logger.info("Registering Web Publishing service at OSGi framework");
            bundleContext.registerService(WebPublishingService.class.getName(), wpService, null);
        }
    }
}
