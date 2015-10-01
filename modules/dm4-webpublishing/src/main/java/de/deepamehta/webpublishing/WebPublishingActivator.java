package de.deepamehta.webpublishing;

import de.deepamehta.webpublishing.impl.WebPublishingServiceImpl;

import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.service.webpublishing.WebPublishingService;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

import java.util.logging.Level;
import java.util.logging.Logger;



public class WebPublishingActivator implements BundleActivator {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private static BundleContext bundleContext;

    // consumed services
    private DeepaMehtaService dms;
    private HttpService httpService;

    private ServiceTracker coreServiceTracker;
    private ServiceTracker httpServiceTracker;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************
    // *** BundleActivator Implementation ***
    // **************************************



    @Override
    public void start(BundleContext bundleContext) {
        try {
            logger.info("========== Starting \"DeepaMehta 4 WebPublishing\" ==========");
            this.bundleContext = bundleContext;
            //
            (coreServiceTracker = createServiceTracker(DeepaMehtaService.class)).open();
            (httpServiceTracker = createServiceTracker(HttpService.class)).open();
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "An error occurred while starting \"DeepaMehta 4 WebPublishing\":", e);
            // Note: here we catch anything, also errors (like NoClassDefFoundError).
            // If thrown through the OSGi container it would not print out the stacktrace.
            // File Install would retry to start the bundle endlessly.
        }
    }

    @Override
    public void stop(BundleContext bundleContext) {
        try {
            logger.info("========== Stopping \"DeepaMehta 4 WebPublishing\" ==========");
            coreServiceTracker.close();
            httpServiceTracker.close();
            //
            // Note: we do not shutdown the DB here.
            // The DB shuts down itself through the storage bundle's stop() method.
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "An error occurred while stopping \"DeepaMehta 4 WebPublishing\":", e);
            // Note: here we catch anything, also errors (like NoClassDefFoundError).
            // If thrown through the OSGi container it would not print out the stacktrace.
        }
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
                        " to \"DeepaMehta 4 WebPublishing\":", e);
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
                        " from \"DeepaMehta 4 WebPublishing\":", e);
                    // Note: here we catch anything, also errors (like NoClassDefFoundError).
                    // If thrown through the OSGi container it would not print out the stacktrace.
                }
            }
        };
    }

    // ---

    private void addService(Object service) {
        if (service instanceof DeepaMehtaService) {
            logger.info("Adding DeepaMehta 4 core service to DeepaMehta 4 WebPublishing");
            dms = (DeepaMehtaService) service;
            checkRequirementsForActivation();
        } else if (service instanceof HttpService) {
            logger.info("Adding HTTP service to DeepaMehta 4 WebPublishing");
            httpService = (HttpService) service;
            checkRequirementsForActivation();
        }
    }

    private void removeService(Object service) {
        if (service == dms) {
            logger.info("Removing DeepaMehta 4 core service from DeepaMehta 4 WebPublishing");
            dms = null;
        } else if (service == httpService) {
            logger.info("Removing HTTP service from DeepaMehta 4 WebPublishing");
            // ### TODO: unregister WebPublishingService
            httpService = null;
        }
    }

    // ---

    private void checkRequirementsForActivation() {
        if (dms != null && httpService != null) {
            WebPublishingService wpService = new WebPublishingServiceImpl(dms, httpService);
            logger.info("Registering DeepaMehta 4 WebPublishing service at OSGi framework");
            bundleContext.registerService(WebPublishingService.class.getName(), wpService, null);
        }
    }
}
