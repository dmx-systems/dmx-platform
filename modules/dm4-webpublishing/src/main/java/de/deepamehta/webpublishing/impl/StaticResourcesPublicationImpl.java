package de.deepamehta.webpublishing.impl;

import de.deepamehta.core.service.webpublishing.StaticResourcesPublication;



class StaticResourcesPublicationImpl implements StaticResourcesPublication {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String uriNamespace;

    private WebUnpublishing webUnpublishing;

    // ---------------------------------------------------------------------------------------------------- Constructors

    StaticResourcesPublicationImpl(String uriNamespace, WebUnpublishing webUnpublishing) {
        this.uriNamespace = uriNamespace;
        this.webUnpublishing = webUnpublishing;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void unpublish() {
        webUnpublishing.unpublishStaticResources(uriNamespace);
    }
}
