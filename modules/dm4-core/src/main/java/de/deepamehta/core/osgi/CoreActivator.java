package de.deepamehta.core.osgi;

import de.deepamehta.core.impl.EmbeddedService;
import de.deepamehta.core.impl.StorageDecorator;
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

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private static BundleContext bundleContext;

    // consumed services
    private DeepaMehtaStorage storageService;
    private static HttpService httpService;

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
            (storageServiceTracker = createServiceTracker(DeepaMehtaStorage.class)).open();
            (httpServiceTracker = createServiceTracker(HttpService.class)).open();
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "An error occurred while starting \"DeepaMehta 4 Core\":", e);
            // Note: here we catch anything, also errors (like NoClassDefFoundError).
            // If thrown through the OSGi container it would not print out the stacktrace.
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
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "An error occurred while stopping \"DeepaMehta 4 Core\":", e);
            // Note: here we catch anything, also errors (like NoClassDefFoundError).
            // If thrown through the OSGi container it would not print out the stacktrace.
        }
    }

    // ---

    public static DeepaMehtaService getDeepaMehtaService() {
        return getService(DeepaMehtaService.class);
    }

    public static <S> S getService(Class<S> clazz) {
        S serviceObject = bundleContext.getService(bundleContext.getServiceReference(clazz));
        if (serviceObject == null) {
            throw new RuntimeException("Service \"" + clazz.getName() + "\" is not available");
        }
        return serviceObject;
    }

    // ---

    public static HttpService getHttpService() {
        return httpService;
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private ServiceTracker createServiceTracker(final Class serviceInterface) {
        //
        return new ServiceTracker(bundleContext, serviceInterface.getName(), null) {

            @Override
            public Object addingService(ServiceReference serviceRef) {
                Object service = null;
                try {
                    service = super.addingService(serviceRef);
                    addService(service);
                } catch (Throwable e) {
                    logger.log(Level.SEVERE, "An error occurred while adding service " + serviceInterface.getName() +
                        " to \"DeepaMehta 4 Core\":", e);
                    // Note: here we catch anything, also errors (like NoClassDefFoundError).
                    // If thrown through the OSGi container it would not print out the stacktrace.
                }
                return service;
            }

            @Override
            public void removedService(ServiceReference ref, Object service) {
                try {
                    removeService(service);
                    super.removedService(ref, service);
                } catch (Throwable e) {
                    logger.log(Level.SEVERE, "An error occurred while removing service " + serviceInterface.getName() +
                        " from \"DeepaMehta 4 Core\":", e);
                    // Note: here we catch anything, also errors (like NoClassDefFoundError).
                    // If thrown through the OSGi container it would not print out the stacktrace.
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
            httpService = null;
        }
    }

    // ---

    private void checkRequirementsForActivation() {
        if (storageService != null && httpService != null) {
            DeepaMehtaService dms = new EmbeddedService(new StorageDecorator(storageService), bundleContext);
            logger.info("Registering DeepaMehta 4 core service at OSGi framework");
            bundleContext.registerService(DeepaMehtaService.class.getName(), dms, null);
        }
    }
}
