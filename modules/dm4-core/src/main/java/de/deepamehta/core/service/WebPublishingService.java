package de.deepamehta.core.service;

import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

import javax.ws.rs.Path;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;



// ### TODO: should not be a public class.
public class WebPublishingService {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String APPLICATION_ROOT = "/";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DefaultResourceConfig rootApplication = new DefaultResourceConfig();
    private int classCount = 0;         // counts DM root resource and provider classes
    private int singletonCount = 0;     // counts DM root resource and provider instances
    // Note: we count DM resources separately as Jersey adds its own ones to the application.
    // Once the total DM resource count reaches 0 the Jersey servlet is unregistered.

    private ServletContainer jerseyServlet = new ServletContainer(rootApplication);
    private boolean isJerseyServletRegistered = false;

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

    // ----------------------------------------------------------------------------------------- Package Private Methods

    // Note: synchronizing this method prevents creation of multiple Jersey servlet instances due to parallel plugin
    // initialization.
    synchronized RestResource addResource(Object resource, Set<Class<?>> providerClasses) {
        addSingleton(resource);
        addClasses(providerClasses);
        logResourceInfo();
        //
        // Note: we must register the Jersey servlet lazily, that is not before any resources or providers
        // are added. An "empty" application would fail (com.sun.jersey.api.container.ContainerException:
        // The ResourceConfig instance does not contain any root resource classes).
        if (!isJerseyServletRegistered) {
            registerJerseyServlet();
        } else {
            reloadJerseyServlet();
        }
        //
        return new RestResource(resource, providerClasses);
    }

    synchronized void removeResource(RestResource restResource) {
        removeSingleton(restResource.resource);
        removeClasses(restResource.providerClasses);
        logResourceInfo();
        //
        // Note: once all resources and providers are removed we must unregister the Jersey servlet.
        // Reloading it with an "empty" application would fail (com.sun.jersey.api.container.ContainerException:
        // The ResourceConfig instance does not contain any root resource classes).
        if (!hasResources()) {
            unregisterJerseyServlet();
        } else {
            reloadJerseyServlet();
        }
    }

    // ---

    boolean isResource(Object object) {
        return object.getClass().isAnnotationPresent(Path.class);
    }

    String getUriPath(Object object) {
        Path path = object.getClass().getAnnotation(Path.class);
        return path != null ? path.value() : null;
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private Set<Class<?>> getClasses() {
        return rootApplication.getClasses();
    }

    private Set<Object> getSingletons() {
        return rootApplication.getSingletons();
    }

    // ---

    private void addClasses(Set<Class<?>> classes) {
        getClasses().addAll(classes);
        classCount += classes.size();
    }

    private void addSingleton(Object singleton) {
        getSingletons().add(singleton);
        singletonCount++;
    }

    // ---

    private void removeClasses(Set<Class<?>> classes) {
        getClasses().removeAll(classes);
        classCount -= classes.size();
    }

    private void removeSingleton(Object singleton) {
        getSingletons().remove(singleton);
        singletonCount--;
    }

    // ---

    private boolean hasResources() {
        return classCount + singletonCount > 0;
    }

    private void logResourceInfo() {
        logger.fine("##### DM Classes: " + classCount + ", All: " + getClasses().size() + " " + getClasses());
        logger.fine("##### DM Singletons: " + singletonCount + ", All: " + getSingletons().size() + " " +
            getSingletons());
    }

    // ---

    private void registerJerseyServlet() {
        try {
            logger.fine("########## Registering Jersey servlet at HTTP service (namespace=\"" + APPLICATION_ROOT +
                "\")");
            httpService.registerServlet(APPLICATION_ROOT, jerseyServlet, null, null);  // javax.servlet.ServletException
            isJerseyServletRegistered = true;
        } catch (Exception e) {
            // unregister...();     // ### TODO?
            throw new RuntimeException("Registering Jersey servlet at HTTP service failed", e);
        }
    }

    private void unregisterJerseyServlet() {
        logger.fine("########## Unregistering Jersey servlet at HTTP service (namespace=\"" + APPLICATION_ROOT + "\")");
        httpService.unregister(APPLICATION_ROOT);
        isJerseyServletRegistered = false;
    }

    // ---

    private void reloadJerseyServlet() {
        logger.fine("##### Reloading Jersey servlet");
        jerseyServlet.reload();
    }
}
