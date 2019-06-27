package systems.dmx.core.osgi;

import systems.dmx.core.impl.AccessLayer;
import systems.dmx.core.impl.CoreServiceImpl;
import systems.dmx.core.impl.ModelFactoryImpl;
import systems.dmx.core.service.CoreService;
import systems.dmx.core.service.ModelFactory;
import systems.dmx.core.storage.spi.DMXStorage;
import systems.dmx.core.storage.spi.DMXStorageFactory;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

import java.util.logging.Level;
import java.util.logging.Logger;



public class CoreActivator implements BundleActivator {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String DATABASE_FACTORY = "systems.dmx.storage.neo4j.Neo4jStorageFactory";

    private static final String DATABASE_PATH = System.getProperty("dmx.database.path", "dmx-db");
    // Note: the default value is required in case no config file is in effect. This applies when DM is started
    // via feature:install from Karaf. The default value must match the value defined in project POM.

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private static BundleContext bundleContext;

    private DMXStorage db;
    private static ModelFactory mf = new ModelFactoryImpl();    // FIXME: instantiate along with db?

    // consumed service
    private static HttpService httpService;

    private ServiceTracker httpServiceTracker;

    // provided service
    private CoreServiceImpl dmx;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************
    // *** BundleActivator Implementation ***
    // **************************************



    @Override
    public void start(BundleContext bundleContext) {
        try {
            logger.info("========== Starting \"DMX Core\" ==========");
            this.bundleContext = bundleContext;
            //
            db = openDB(DATABASE_PATH);
            //
            (httpServiceTracker = createServiceTracker(HttpService.class)).open();
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "An error occurred while starting \"DMX Core\":", e);
            // Note: here we catch anything, also errors (like NoClassDefFoundError).
            // If thrown through the OSGi container it would not print out the stacktrace.
            // File Install would retry to start the bundle endlessly.
        }
    }

    @Override
    public void stop(BundleContext bundleContext) {
        try {
            logger.info("========== Stopping \"DMX Core\" ==========");
            httpServiceTracker.close();
            //
            if (dmx != null) {
                dmx.shutdown();
            }
            if (db != null) {
                logger.info("### Shutting down the database");
                db.shutdown();
            }
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "An error occurred while stopping \"DMX Core\":", e);
            // Note: here we catch anything, also errors (like NoClassDefFoundError).
            // If thrown through the OSGi container it would not print out the stacktrace.
        }
    }

    // ---

    public static CoreService getCoreService() {
        return getService(CoreService.class);
    }

    public static ModelFactory getModelFactory() {
        return mf;
    }

    public static HttpService getHttpService() {
        return httpService;
    }

    // ---

    public static <S> S getService(Class<S> clazz) {
        S serviceObject = bundleContext.getService(bundleContext.getServiceReference(clazz));
        if (serviceObject == null) {
            throw new RuntimeException("Service \"" + clazz.getName() + "\" is not available");
        }
        return serviceObject;
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    // copy in CoreServiceTestEnvironment (dmx-test)
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

    // ---

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
                        " to \"DMX Core\":", e);
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
                        " from \"DMX Core\":", e);
                    // Note: here we catch anything, also errors (like NoClassDefFoundError).
                    // If thrown through the OSGi container it would not print out the stacktrace.
                }
            }
        };
    }

    // ---

    private void addService(Object service) {
        if (service instanceof HttpService) {
            logger.info("Adding HTTP service to DMX Core");
            httpService = (HttpService) service;
            checkRequirementsForActivation();
        }
    }

    private void removeService(Object service) {
        if (service == httpService) {
            logger.info("Removing HTTP service from DMX Core");
            httpService = null;
        }
    }

    // ---

    private void checkRequirementsForActivation() {
        if (httpService != null) {
            dmx = new CoreServiceImpl(new AccessLayer(db), bundleContext);
            //
            logger.info("Registering DMX core service at OSGi framework");
            bundleContext.registerService(CoreService.class.getName(), dmx, null);
        }
    }
}
