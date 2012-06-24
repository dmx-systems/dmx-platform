package de.deepamehta.core.service;

import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;



// ### TODO: should not be a public class.
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

    public WebPublishingService(BundleContext context, HttpService httpService) {
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
    synchronized RestResource addResource(Object resource, Set<Class<?>> providerClasses) {
        singletons.add(resource);
        classes.addAll(providerClasses);
        // Note: we must create the Jersey servlet lazily, that is not before any resources or providers are added.
        // A Jersey servlet with an "empty" application would fail (com.sun.jersey.api.container.ContainerException:
        // The ResourceConfig instance does not contain any root resource classes).
        if (jerseyServlet == null) {
            createJerseyServlet();
        } else {
            reloadJerseyServlet();
        }
        //
        return new RestResource(resource, providerClasses);
    }

    synchronized void removeResource(RestResource restResource) {
        singletons.remove(restResource.resource);
        classes.removeAll(restResource.providerClasses);
        //
        reloadJerseyServlet();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void createJerseyServlet() {
        try {
            logger.info("########## Creating Jersey servlet and registering at HTTP service");
            jerseyServlet = new ServletContainer(new RootApplication());
            httpService.registerServlet(APPLICATION_ROOT, jerseyServlet, null, null);
        } catch (Exception e) {
            // unregister...();     // ### TODO?
            throw new RuntimeException("Creating Jersey servlet and registering at HTTP service failed", e);
        }
    }

    private void reloadJerseyServlet() {
        logger.info("##### Reloading Jersey servlet");
        jerseyServlet.reload();
    }

    // ------------------------------------------------------------------------------------------------- Private Classes

    private class RootApplication extends DefaultResourceConfig {

        @Override
        public Set<Class<?>> getClasses() {
            return classes;
        }

        @Override
        public Set<Object> getSingletons() {
            // logger.info("##### " + singletons.size() + " resources: " + singletons);
            return singletons;
        }
    }
}
