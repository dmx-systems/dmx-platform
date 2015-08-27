package de.deepamehta.core.impl;

import de.deepamehta.core.service.DeepaMehtaService;

import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;



public class WebPublishingService {

    // ------------------------------------------------------------------------------------------------------- Constants

    // Note: OPS4J Pax Web needs "/*". Felix HTTP Jetty in contrast needs "/".
    private static final String ROOT_APPLICATION_PATH = "/*";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private ResourceConfig jerseyApplication;
    private int classCount = 0;         // counts DM root resource and provider classes
    private int singletonCount = 0;     // counts DM root resource and provider singletons
    // Note: we count DM resources separately as Jersey adds its own ones to the application.
    // Once the total DM resource count reaches 0 the Jersey servlet is unregistered.

    private ServletContainer jerseyServlet;
    private boolean isJerseyServletRegistered = false;

    private HttpService httpService;

    private DeepaMehtaService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public WebPublishingService(DeepaMehtaService dms, HttpService httpService) {
        try {
            logger.info("Setting up the Web Publishing service");
            this.dms = dms;
            //
            // create Jersey application
            this.jerseyApplication = new DefaultResourceConfig();
            //
            // setup container filters
            Map<String, Object> properties = jerseyApplication.getProperties();
            properties.put(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS, new JerseyRequestFilter(dms));
            properties.put(ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS, new JerseyResponseFilter(dms));
            properties.put(ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES, new TransactionFactory(dms));
            //
            // deploy Jersey application in container
            this.jerseyServlet = new ServletContainer(jerseyApplication);
            this.httpService = httpService;
        } catch (Exception e) {
            // unregister...();     // ### TODO?
            throw new RuntimeException("Setting up the Web Publishing service failed", e);
        }
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Static Resources ===

    /**
     * Publishes the static resources of the given bundle's /web directory.
     */
    StaticResources publishStaticResources(Bundle bundle, String uriNamespace) {
        try {
            // Note: registerResources() throws org.osgi.service.http.NamespaceException
            httpService.registerResources(uriNamespace, "/web", new BundleHTTPContext(bundle));
            return new StaticResources(uriNamespace);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void unpublishStaticResources(StaticResources staticResources) {
        httpService.unregister(staticResources.uriNamespace);
    }

    // ---

    /**
     * Publishes a directory of the server's file system.
     */
    StaticResources publishStaticResources(String directoryPath, String uriNamespace) {
        try {
            // Note: registerResources() throws org.osgi.service.http.NamespaceException
            httpService.registerResources(uriNamespace, "/", new DirectoryHTTPContext(directoryPath));
            return new StaticResources(uriNamespace);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    // === REST Resources ===

    /**
     * Publishes REST resources. This is done by adding JAX-RS root resource and provider classes/singletons
     * to the Jersey application and reloading the Jersey servlet.
     * <p>
     * Note: synchronizing this method prevents creation of multiple Jersey servlet instances due to parallel plugin
     * initialization.
     *
     * @param   singletons  the set of root resource and provider singletons, may be empty.
     * @param   classes     the set of root resource and provider classes, may be empty.
     */
    synchronized RestResources publishRestResources(List<Object> singletons, List<Class<?>> classes) {
        RestResources restResources = new RestResources(singletons, classes);
        try {
            addToApplication(restResources);
            //
            // Note: we must register the Jersey servlet lazily, that is not before any root resources are added.
            // An "empty" application would fail (com.sun.jersey.api.container.ContainerException:
            // The ResourceConfig instance does not contain any root resource classes).
            if (!isJerseyServletRegistered) {
                // Note: we must not register the servlet as long as no root resources are added yet.
                // A plugin may contain just provider classes.
                if (hasRootResources()) {
                    registerJerseyServlet();
                }
            } else {
                reloadJerseyServlet();
            }
            //
            return restResources;
        } catch (Exception e) {
            unpublishRestResources(restResources);
            throw new RuntimeException("Adding classes/singletons to Jersey application failed", e);
        }
    }

    synchronized void unpublishRestResources(RestResources restResources) {
        removeFromApplication(restResources);
        //
        // Note: once all root resources are removed we must unregister the Jersey servlet.
        // Reloading it with an "empty" application would fail (com.sun.jersey.api.container.ContainerException:
        // The ResourceConfig instance does not contain any root resource classes).
        if (!hasRootResources()) {
            unregisterJerseyServlet();
        } else {
            reloadJerseyServlet();
        }
    }

    // ---

    boolean isRootResource(Object object) {
        return getUriNamespace(object) != null;
    }

    String getUriNamespace(Object object) {
        Path path = object.getClass().getAnnotation(Path.class);
        return path != null ? path.value() : null;
    }

    // ---

    boolean isProviderClass(Class clazz) {
        return clazz.isAnnotationPresent(Provider.class);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Jersey application ===

    private void addToApplication(RestResources restResources) {
        getClasses().addAll(restResources.classes);
        getSingletons().addAll(restResources.singletons);
        //
        classCount     += restResources.classes.size();
        singletonCount += restResources.singletons.size();
        //
        logResourceInfo();
    }

    private void removeFromApplication(RestResources restResources) {
        getClasses().removeAll(restResources.classes);
        getSingletons().removeAll(restResources.singletons);
        //
        classCount -= restResources.classes.size();
        singletonCount -= restResources.singletons.size();
        //
        logResourceInfo();
    }

    // ---

    private boolean hasRootResources() {
        return singletonCount > 0;
    }

    private void logResourceInfo() {
        logger.fine("##### DM Classes: " + classCount + ", All: " + getClasses().size() + " " + getClasses());
        logger.fine("##### DM Singletons: " + singletonCount + ", All: " + getSingletons().size() + " " +
            getSingletons());
    }

    // ---

    private Set<Class<?>> getClasses() {
        return jerseyApplication.getClasses();
    }

    private Set<Object> getSingletons() {
        return jerseyApplication.getSingletons();
    }



    // === Jersey Servlet ===

    private void registerJerseyServlet() {
        try {
            logger.fine("########## Registering Jersey servlet at HTTP service (URI namespace=\"" +
                ROOT_APPLICATION_PATH + "\")");
            // Note: registerServlet() throws javax.servlet.ServletException
            httpService.registerServlet(ROOT_APPLICATION_PATH, jerseyServlet, null, null);
            isJerseyServletRegistered = true;
        } catch (Exception e) {
            // unregister...();     // ### TODO?
            throw new RuntimeException("Registering Jersey servlet at HTTP service failed", e);
        }
    }

    private void unregisterJerseyServlet() {
        logger.fine("########## Unregistering Jersey servlet at HTTP service (URI namespace=\"" +
            ROOT_APPLICATION_PATH + "\")");
        httpService.unregister(ROOT_APPLICATION_PATH);
        isJerseyServletRegistered = false;
    }

    // ---

    private void reloadJerseyServlet() {
        logger.fine("##### Reloading Jersey servlet");
        jerseyServlet.reload();
    }



    // === Resource Request Filter ===

    private boolean resourceRequestFilter(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            dms.fireEvent(CoreEvent.RESOURCE_REQUEST_FILTER, request);
            return true;
        } catch (WebApplicationException e) {
            sendError(response, e.getResponse());
            return false;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Resource request filtering failed for " + request.getRequestURI(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return false;
        }
    }

    private void sendError(HttpServletResponse servletResponse, Response response) throws IOException {
        // transfer headers
        MultivaluedMap<String, Object> metadata = response.getMetadata();
        for (String header : metadata.keySet()) {
            for (Object value : metadata.get(header)) {
                servletResponse.addHeader(header, (String) value);
            }
        }
        //
        servletResponse.sendError(response.getStatus(), (String) response.getEntity());   // throws IOException
    }



    // ------------------------------------------------------------------------------------------------- Private Classes

    private class BundleHTTPContext implements HttpContext {

        private Bundle bundle;

        private BundleHTTPContext(Bundle bundle) {
            this.bundle = bundle;
        }

        // ---

        @Override
        public URL getResource(String name) {
            // 1) map "/" to "/index.html"
            //
            // Note: for the bundle's web root resource Pax Web passes "/web/" or "/web",
            // depending whether the request URL has a slash at the end or not.
            // Felix HTTP Jetty 2.2.0 in contrast passes "web/" and version 2.3.0 passes "/web/"
            // (regardless whether the request URL has a slash at the end or not).
            if (name.equals("/web") || name.equals("/web/")) {
                name = "/web/index.html";
            }
            // 2) access resource from context bundle
            return bundle.getResource(name);
        }

        @Override
        public String getMimeType(String name) {
            return null;
        }

        @Override
        public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response)
                                                                            throws java.io.IOException {
            return resourceRequestFilter(request, response);
        }
    }

    private class DirectoryHTTPContext implements HttpContext {

        private String directoryPath;

        private DirectoryHTTPContext(String directoryPath) {
            this.directoryPath = directoryPath;
        }

        // ---

        @Override
        public URL getResource(String name) {
            try {
                URL url = new URL("file:" + directoryPath + "/" + name);    // throws java.net.MalformedURLException
                logger.info("### Mapping resource name \"" + name + "\" to URL \"" + url + "\"");
                return url;
            } catch (Exception e) {
                throw new RuntimeException("Mapping resource name \"" + name + "\" to URL failed", e);
            }
        }

        @Override
        public String getMimeType(String name) {
            return null;
        }

        @Override
        public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response)
                                                                            throws java.io.IOException {
            return resourceRequestFilter(request, response);
        }
    }
}
