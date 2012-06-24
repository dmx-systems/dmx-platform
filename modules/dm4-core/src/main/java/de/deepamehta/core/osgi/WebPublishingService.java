package de.deepamehta.core.osgi;

import javax.ws.rs.core.Application;
import com.sun.jersey.spi.container.servlet.ServletContainer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;



public class WebPublishingService {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String APPLICATION_ROOT = "/";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    // Root resource and provider classes
    private Set<Class<?>> classes = new HashSet<Class<?>>();
    // Root resource and provider instances
    private Set<Object> singletons = new HashSet<Object>();

    private ServletContainer jerseyServlet;

    private HttpService httpService;
    private ServiceRegistration registration;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    WebPublishingService(BundleContext context, HttpService httpService) {
        try {
            logger.info("Registering Web Publishing service at OSGi framework");
            this.httpService = httpService;
            this.registration = context.registerService(getClass().getName(), this, null);
        } catch (Exception e) {
            // unregister...();     // ### TODO?
            throw new RuntimeException("Registering Web Publishing service at OSGi framework failed", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // ### Note: the root resource and provider classes must be added in the same method along with reloading the
    // ### Jersey servlet. Otherwise the provider classes will not work. Furthermore this works only with synchronous
    // ### bundle starts (felix.fileinstall.noInitialDelay=true in global pom). This all is really strange.
    public synchronized void addResource(Object resource, Set<Class<?>> providerClasses) {
        singletons.add(resource);
        classes.addAll(providerClasses);
        // ### registerServlet();
        if (jerseyServlet == null) {
            try {
                logger.info("########## Registering Jersey servlet at HTTP service");
                jerseyServlet = new ServletContainer(new RootApplication());
                httpService.registerServlet(APPLICATION_ROOT, jerseyServlet, null, null);
            } catch (Exception e) {
                // unregister...();     // ### TODO?
                throw new RuntimeException("Registering Jersey servlet at HTTP service failed", e);
            }
        } else {
            jerseyServlet.reload();
        }
    }

    /* public synchronized void addProviderClasses(Set<Class<?>> providerClasses) {
        classes.addAll(providerClasses);
        registerServlet();
        //
        jerseyServlet.reload();
    } */

    /* public synchronized void refresh() {
        // Note: we must register the Jersey servlet lazily, that is not before any resources or providers are added.
        // A Jersey servlet with an "empty" application would fail (com.sun.jersey.api.container.ContainerException:
        // The ResourceConfig instance does not contain any root resource classes).
        registerServlet();
        //
        jerseyServlet.reload();
    } */

    // ------------------------------------------------------------------------------------------------- Private Methods

    // Note: synchronizing this method prevents creation of multiple Jersey servlet instances due to parallel plugin
    // initialization.
    /* private synchronized void registerServlet() {
        if (jerseyServlet != null) {
            return;
        }
        try {
            logger.info("########## Registering Jersey servlet at HTTP service");
            jerseyServlet = new ServletContainer(new RootApplication());
            httpService.registerServlet(APPLICATION_ROOT, jerseyServlet, null, null);
        } catch (Exception e) {
            // unregister...();     // ### TODO?
            throw new RuntimeException("Registering Jersey servlet at HTTP service failed", e);
        }
    } */

    // ------------------------------------------------------------------------------------------------- Private Classes

    private class RootApplication extends Application {

        @Override
        public Set<Class<?>> getClasses() {
            return classes;
        }

        @Override
        public Set<Object> getSingletons() {
            return singletons;
        }
    }
}
