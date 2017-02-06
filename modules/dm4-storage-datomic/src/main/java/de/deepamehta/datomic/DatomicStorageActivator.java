package de.deepamehta.datomic;

import de.deepamehta.core.service.ModelFactory;
import de.deepamehta.core.storage.spi.DeepaMehtaStorage;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import java.util.logging.Level;
import java.util.logging.Logger;



public class DatomicStorageActivator implements BundleActivator {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String DATABASE_URI = System.getProperty("dm4.database.path", "datomic:mem://deepamehta-db");
    // Note: the default value is required in case no config file is in effect. This applies when DM is started
    // via feature:install from Karaf. The default value must match the value defined in project POM.

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private static BundleContext bundleContext;

    // consumed service
    ModelFactory mf;

    // provided service
    private DeepaMehtaStorage storage;

    private ServiceTracker modelFactoryTracker;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************
    // *** BundleActivator Implementation ***
    // **************************************



    @Override
    public void start(BundleContext bundleContext) {
        try {
            logger.info("========== Starting \"DeepaMehta 4 Storage - Datomic\" ==========");
            this.bundleContext = bundleContext;
            //
            (modelFactoryTracker = createServiceTracker(ModelFactory.class)).open();
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "An error occurred while starting \"DeepaMehta 4 Storage - Datomic\":", e);
            // Note: here we catch anything, also errors (like NoClassDefFoundError).
            // If thrown through the OSGi container it would not print out the stacktrace.
            // File Install would retry to start the bundle endlessly.
        }
    }

    @Override
    public void stop(BundleContext bundleContext) {
        try {
            logger.info("========== Stopping \"DeepaMehta 4 Storage - Datomic\" ==========");
            modelFactoryTracker.close();
            //
            if (storage != null) {
                storage.shutdown();
            }
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "An error occurred while stopping \"DeepaMehta 4 Storage - Datomic\":", e);
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
                        " to \"DeepaMehta 4 Storage - Datomic\":", e);
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
                        " from \"DeepaMehta 4 Storage - Datomic\":", e);
                    // Note: here we catch anything, also errors (like NoClassDefFoundError).
                    // If thrown through the OSGi container it would not print out the stacktrace.
                }
            }
        };
    }

    // ---

    private void addService(Object service) {
        if (service instanceof ModelFactory) {
            logger.info("Adding ModelFactory service to DeepaMehta 4 Storage - Datomic");
            mf = (ModelFactory) service;
            checkRequirementsForActivation();
        }
    }

    private void removeService(Object service) {
        if (service == mf) {
            logger.info("Removing ModelFactory service from DeepaMehta 4 Storage - Datomic");
            mf = null;
        }
    }

    // ---

    private void checkRequirementsForActivation() {
        if (mf != null) {
            storage = new DatomicStorage(DATABASE_URI, mf);
            //
            logger.info("Registering DeepaMehta 4 storage service - Datomic - at OSGi framework");
            bundleContext.registerService(DeepaMehtaStorage.class.getName(), storage, null);
        }
    }
}
