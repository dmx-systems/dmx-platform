package de.deepamehta.webpublishing.impl;

import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.service.webpublishing.RestResourcesPublication;
import de.deepamehta.core.service.webpublishing.StaticResourcesPublication;
import de.deepamehta.core.service.webpublishing.WebPublishingService;
import de.deepamehta.core.util.UniversalExceptionMapper;

import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;



public class WebPublishingServiceImpl implements WebPublishingService, WebUnpublishing {

    // ------------------------------------------------------------------------------------------------------- Constants

    // Note: OPS4J Pax Web needs "/*". Felix HTTP Jetty in contrast needs "/".
    private static final String ROOT_APPLICATION_PATH = "/";

    // Note: actually the class WebPublishingEvents does not need to be instantiated as it contains only statics.
    // But if not instantiated OSGi apparently does not load the class at all.
    static {
        new WebPublishingEvents();
    }

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

    public WebPublishingServiceImpl(DeepaMehtaService dms, HttpService httpService) {
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

    @Override
    public StaticResourcesPublication publishWebResources(String uriNamespace, Bundle bundle)
                                                                                          throws NamespaceException {
        httpService.registerResources(uriNamespace, "/web", new BundleHTTPContext(bundle));
        return new StaticResourcesPublicationImpl(uriNamespace, this);
    }

    @Override
    public StaticResourcesPublication publishFileSystem(String uriNamespace, String path) throws NamespaceException {
        httpService.registerResources(uriNamespace, "/", new FileSystemHTTPContext(path));
        return new StaticResourcesPublicationImpl(uriNamespace, this);
    }

    @Override
    public void unpublishStaticResources(String uriNamespace) {
        httpService.unregister(uriNamespace);
    }



    // === REST Resources ===

    // Note: synchronizing prevents creation of multiple Jersey servlet instances due to parallel plugin initialization
    @Override
    public synchronized RestResourcesPublication publishRestResources(List<Object> singletons, List<Class<?>> classes) {
        try {
            addToApplication(singletons, classes);
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
            return new RestResourcesPublicationImpl(singletons, classes, this);
        } catch (Exception e) {
            unpublishRestResources(singletons, classes);
            throw new RuntimeException("Adding classes/singletons to Jersey application failed", e);
        }
    }

    @Override
    public synchronized void unpublishRestResources(List<Object> singletons, List<Class<?>> classes) {
        removeFromApplication(singletons, classes);
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

    @Override
    public boolean isRootResource(Object object) {
        return getUriNamespace(object) != null;
    }

    @Override
    public String getUriNamespace(Object object) {
        Path path = object.getClass().getAnnotation(Path.class);
        return path != null ? path.value() : null;
    }

    @Override
    public boolean isProviderClass(Class clazz) {
        return clazz.isAnnotationPresent(Provider.class);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Jersey application ===

    private void addToApplication(List<Object> singletons, List<Class<?>> classes) {
        getClasses().addAll(classes);
        getSingletons().addAll(singletons);
        //
        classCount     += classes.size();
        singletonCount += singletons.size();
        //
        logResourceInfo();
    }

    private void removeFromApplication(List<Object> singletons, List<Class<?>> classes) {
        getClasses().removeAll(classes);
        getSingletons().removeAll(singletons);
        //
        classCount -= classes.size();
        singletonCount -= singletons.size();
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
            throw new RuntimeException("Registering Jersey servlet at HTTP service failed (URI namespace=\"" +
                ROOT_APPLICATION_PATH + "\")", e);
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
            dms.fireEvent(WebPublishingEvents.RESOURCE_REQUEST_FILTER, request);
            return true;
        } catch (Throwable e) {
            // Note: resourceRequestFilter() is called from an OSGi HTTP service static resource HttpContext.
            // JAX-RS is not involved here. No JAX-RS exception mapper kicks in. Though the application's
            // ResourceRequestFilterListener can throw a WebApplicationException (which is JAX-RS API)
            // in order to provide error response info.
            new UniversalExceptionMapper(e, request).initResponse(response);
            return false;
        }
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

    private class FileSystemHTTPContext implements HttpContext {

        private String path;

        /**
         * @param   path    An absolute path to a directory.
         */
        private FileSystemHTTPContext(String path) {
            this.path = path;
        }

        // ---

        @Override
        public URL getResource(String name) {
            try {
                URL url = new URL("file:" + path + "/" + name);     // throws java.net.MalformedURLException
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
