package de.deepamehta.webpublishing.impl;

import java.util.List;



interface WebUnpublishing {

    void unpublishStaticResources(String uriNamespace);

    void unpublishRestResources(List<Object> singletons, List<Class<?>> classes);
}
