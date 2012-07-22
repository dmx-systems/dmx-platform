package de.deepamehta.core.impl.service;

import java.util.Set;



class RestResource {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    Object resource;
    Set<Class<?>> providerClasses;

    // ---------------------------------------------------------------------------------------------------- Constructors

    RestResource(Object resource, Set<Class<?>> providerClasses) {
        this.resource = resource;
        this.providerClasses = providerClasses;
    }
}
