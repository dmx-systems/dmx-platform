package de.deepamehta.core.impl;

import java.util.List;



class RestResourcesPublication {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private List<Object> singletons;
    private List<Class<?>> classes;

    private WebPublishingService wpService;

    // ---------------------------------------------------------------------------------------------------- Constructors

    RestResourcesPublication(List<Object> singletons, List<Class<?>> classes, WebPublishingService wpService) {
        this.singletons = singletons;
        this.classes = classes;
        this.wpService = wpService;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    void unpublish() {
        wpService.unpublishRestResources(singletons, classes);
    }
}
