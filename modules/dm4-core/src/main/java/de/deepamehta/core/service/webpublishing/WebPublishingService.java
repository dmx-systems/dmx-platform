package de.deepamehta.core.service.webpublishing;

import org.osgi.framework.Bundle;
import org.osgi.service.http.NamespaceException;

import java.util.List;



public interface WebPublishingService {



    // === Static Resources ===

    /**
     * Publishes the bundle's web resources.
     * Web resources are found in the bundle's /web directory.
     */
    StaticResourcesPublication publishWebResources(String uriNamespace, Bundle bundle) throws NamespaceException;

    /**
     * Publishes a directory of the server's file system.
     *
     * @param   path    An absolute path to the directory to be published.
     */
    StaticResourcesPublication publishFileSystem(String uriNamespace, String path) throws NamespaceException;



    // === REST Resources ===

    /**
     * Publishes REST resources. This is done by adding JAX-RS root resource and provider classes/singletons
     * to the Jersey application and reloading the Jersey servlet.
     *
     * @param   singletons  the set of root resource and provider singletons, may be empty.
     * @param   classes     the set of root resource and provider classes, may be empty.
     */
    RestResourcesPublication publishRestResources(List<Object> singletons, List<Class<?>> classes);

    // ---

    boolean isRootResource(Object object);

    String getUriNamespace(Object object);

    boolean isProviderClass(Class clazz);
}
