package de.deepamehta.core.service.webpublishing;

import org.osgi.framework.Bundle;

import java.util.List;



public interface WebPublishingService {

    // === Static Resources ===

    StaticResources publishStaticResources(Bundle bundle, String uriNamespace);

    StaticResources publishDirectory(String path, String uriNamespace, DirectoryResourceMapper resourceMapper);

    void unpublishStaticResources(StaticResources staticResources);

    // === REST Resources ===

    RestResources publishRestResources(List<Object> singletons, List<Class<?>> classes);

    void unpublishRestResources(RestResources restResources);

    // ---

    boolean isRootResource(Object object);

    String getUriNamespace(Object object);

    boolean isProviderClass(Class clazz);
}
