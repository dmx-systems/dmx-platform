package systems.dmx.core.impl;

import systems.dmx.core.osgi.CoreActivator;
import systems.dmx.core.service.CoreService;
import systems.dmx.core.service.WebSocketsService;
import systems.dmx.core.util.JavaUtils;
import systems.dmx.core.util.UniversalExceptionMapper;

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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;



class WebPublishingService {

    // ------------------------------------------------------------------------------------------------------- Constants

    // Note: OPS4J Pax Web needs "/*". Felix HTTP Jetty in contrast needs "/".
    private static final String ROOT_APPLICATION_PATH = System.getProperty("dmx.webservice.path", "/");
    // Note: the default value is required in case no config file is in effect. This applies when DM is started
    // via feature:install from Karaf. The default values must match the values defined in project POM.

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private ResourceConfig jerseyApplication;
    private int classCount = 0;         // counts DM root resource and provider classes
    private int singletonCount = 0;     // counts DM root resource and provider singletons
    // Note: we count DM resources separately as Jersey adds its own ones to the application.
    // Once the total DM resource count reaches 0 the Jersey servlet is unregistered.

    private ServletContainer jerseyServlet;
    private boolean isJerseyServletRegistered = false;

    private AccessLayer al;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    WebPublishingService(AccessLayer al, WebSocketsService ws) {
        try {
            logger.info("Setting up the WebPublishingService");
            this.al = al;
            //
            // create Jersey application
            this.jerseyApplication = new DefaultResourceConfig();
            //
            // setup container filters
            Map<String, Object> properties = jerseyApplication.getProperties();
            properties.put(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS, new JerseyRequestFilter(al.em));
            properties.put(ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS, new JerseyResponseFilter(al.em, ws));
            properties.put(ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES, new TransactionFactory(al));
            //
            // deploy Jersey application in container
            this.jerseyServlet = new ServletContainer(jerseyApplication);
        } catch (Exception e) {
            // unregister...();     // ### TODO?
            throw new RuntimeException("Setting up the WebPublishingService failed", e);
        }
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Static Resources ===

    /**
     * Publishes the bundle's web resources.
     * Web resources are found in the bundle's /web directory.
     */
    StaticResourcesPublication publishWebResources(String uriNamespace, Bundle bundle) throws NamespaceException {
        getHttpService().registerResources(uriNamespace, "/web", new BundleResourcesHTTPContext(bundle));
        return new StaticResourcesPublication(uriNamespace, this);
    }

    /**
     * Publishes a directory of the server's file system.
     *
     * @param   path    An absolute path to the directory to be published.
     */
    StaticResourcesPublication publishFileSystem(String uriNamespace, String path) throws NamespaceException {
        getHttpService().registerResources(uriNamespace, "/", new FileSystemHTTPContext(path));
        return new StaticResourcesPublication(uriNamespace, this);
    }

    void unpublishStaticResources(String uriNamespace) {
        HttpService httpService = getHttpService();
        if (httpService != null) {
            httpService.unregister(uriNamespace);
        } else {
            logger.warning("HTTP service is already gone");
        }
    }



    // === REST Resources ===

    /**
     * Publishes REST resources. This is done by adding JAX-RS root resource and provider classes/singletons
     * to the Jersey application and reloading the Jersey servlet.
     * <p>
     * Note: synchronizing prevents creation of multiple Jersey servlet instances due to parallel plugin initialization.
     *
     * @param   singletons  the set of root resource and provider singletons, may be empty.
     * @param   classes     the set of root resource and provider classes, may be empty.
     */
    synchronized RestResourcesPublication publishRestResources(List<Object> singletons, List<Class<?>> classes) {
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
            return new RestResourcesPublication(singletons, classes, this);
        } catch (Exception e) {
            unpublishRestResources(singletons, classes);
            throw new RuntimeException("Adding classes/singletons to Jersey application failed", e);
        }
    }

    synchronized void unpublishRestResources(List<Object> singletons, List<Class<?>> classes) {
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

    boolean isRootResource(Object object) {
        return getUriNamespace(object) != null;
    }

    String getUriNamespace(Object object) {
        Path path = object.getClass().getAnnotation(Path.class);
        return path != null ? path.value() : null;
    }

    boolean isProviderClass(Class clazz) {
        return clazz.isAnnotationPresent(Provider.class);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private HttpService getHttpService() {
        return CoreActivator.getHttpService();
    }



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
            getHttpService().registerServlet(ROOT_APPLICATION_PATH, jerseyServlet, null, null);
            isJerseyServletRegistered = true;
        } catch (Exception e) {
            throw new RuntimeException("Registering Jersey servlet at HTTP service failed (URI namespace=\"" +
                ROOT_APPLICATION_PATH + "\")", e);
        }
    }

    private void unregisterJerseyServlet() {
        logger.fine("########## Unregistering Jersey servlet at HTTP service (URI namespace=\"" +
            ROOT_APPLICATION_PATH + "\")");
        HttpService httpService = getHttpService();
        if (httpService != null) {
            httpService.unregister(ROOT_APPLICATION_PATH);
        } else {
            logger.warning("HTTP service is already gone");
        }
        isJerseyServletRegistered = false;
    }

    // ---

    private void reloadJerseyServlet() {
        logger.fine("##### Reloading Jersey servlet");
        jerseyServlet.reload();
    }



    // === Resource Request Filter ===

    private boolean staticResourceFilter(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            al.em.fireEvent(CoreEvent.STATIC_RESOURCE_FILTER, request, response);
            return true;
        } catch (Throwable e) {
            // Note: staticResourceFilter() is called from an OSGi HTTP service static resource HttpContext.
            // JAX-RS is not involved here. No JAX-RS exception mapper kicks in. Though the application's
            // StaticResourceFilter can throw a WebApplicationException (which is JAX-RS API) in order to
            // provide error response info.
            new UniversalExceptionMapper(e, request).initResponse(response);
            return false;
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Classes

    private class BundleResourcesHTTPContext implements HttpContext {

        private Bundle bundle;

        private BundleResourcesHTTPContext(Bundle bundle) {
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
            // 2) access bundle resource
            return bundle.getResource(name);
        }

        @Override
        public String getMimeType(String name) {
            return JavaUtils.getFileType(name);
        }

        @Override
        public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response)
                                                                            throws java.io.IOException {
            return staticResourceFilter(request, response);
        }
    }

    private class FileSystemHTTPContext implements HttpContext {

        private String basePath;

        /**
         * @param   basePath    An absolute path to a directory.
         */
        private FileSystemHTTPContext(String basePath) {
            this.basePath = basePath;
        }

        // ---

        @Override
        public URL getResource(String name) {
            try {
                File file = new File(basePath, name);
                // 1) map <dir> to <dir>/index.html
                if (file.isDirectory()) {
                    File index = new File(file, "index.html");
                    if (index.exists()) {
                        file = index;
                    }
                }
                // 2) access file system resource
                URL url = file.toURI().toURL();     // toURL() throws java.net.MalformedURLException
                logger.fine("### Mapping resource name \"" + name + "\" to URL \"" + url + "\"");
                return url;
            } catch (Exception e) {
                throw new RuntimeException("Mapping resource name \"" + name + "\" to URL failed", e);
            }
        }

        @Override
        public String getMimeType(String name) {
            return JavaUtils.getFileType(name);
        }

        @Override
        public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response)
                                                                            throws java.io.IOException {
            return staticResourceFilter(request, response);
        }
    }
}
