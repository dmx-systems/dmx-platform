package de.deepamehta.core.impl;



class StaticResourcesPublication {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String uriNamespace;

    private WebPublishingService wpService;

    // ---------------------------------------------------------------------------------------------------- Constructors

    StaticResourcesPublication(String uriNamespace, WebPublishingService wpService) {
        this.uriNamespace = uriNamespace;
        this.wpService = wpService;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    void unpublish() {
        wpService.unpublishStaticResources(uriNamespace);
    }
}
