package de.deepamehta.core.impl;

import java.util.Set;



class RestResource {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    Set<Object> resources;
    Set<Class<?>> providerClasses;

    // ---------------------------------------------------------------------------------------------------- Constructors

    RestResource(Set<Object> resources, Set<Class<?>> providerClasses) {
        this.resources = resources;
        this.providerClasses = providerClasses;
    }
}
