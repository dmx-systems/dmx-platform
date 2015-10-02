package de.deepamehta.webpublishing.impl;

import de.deepamehta.core.service.webpublishing.RestResourcesPublication;

import java.util.List;



class RestResourcesPublicationImpl implements RestResourcesPublication {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private List<Object> singletons;
    private List<Class<?>> classes;

    private WebUnpublishing webUnpublishing;

    // ---------------------------------------------------------------------------------------------------- Constructors

    RestResourcesPublicationImpl(List<Object> singletons, List<Class<?>> classes, WebUnpublishing webUnpublishing) {
        this.singletons = singletons;
        this.classes = classes;
        this.webUnpublishing = webUnpublishing;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void unpublish() {
        webUnpublishing.unpublishRestResources(singletons, classes);
    }
}
