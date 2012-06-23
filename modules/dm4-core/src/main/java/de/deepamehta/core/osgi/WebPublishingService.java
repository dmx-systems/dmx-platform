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

    private ServletContainer servlet;

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

    // Note: synchronizing this method prevents creation of multiple Jersey servlet instances due to parallel plugin
    // initialization.
    public synchronized void addResource(Object resource) {
        singletons.add(resource);
        // Note: we must register the Jersey servlet lazily, that is not before any resource is added. Registering
        // a Jersey servlet with an "empty" application would fail (com.sun.jersey.api.container.ContainerException:
        // The ResourceConfig instance does not contain any root resource classes).
        registerServlet();
        //
        servlet.reload();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void registerServlet() {
        if (servlet != null) {
            return;
        }
        try {
            logger.info("########## Registering Jersey servlet at HTTP service");
            servlet = new ServletContainer(new RootApplication());
            httpService.registerServlet(APPLICATION_ROOT, servlet, null, null);
        } catch (Exception e) {
            // unregister...();     // ### TODO?
            throw new RuntimeException("Registering Jersey servlet at HTTP service failed", e);
        }            
    }

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
