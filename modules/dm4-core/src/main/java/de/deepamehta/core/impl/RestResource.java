package de.deepamehta.core.impl;

import java.util.List;



class RestResource {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    List<Object> resources;
    List<Class<?>> providerClasses;

    // ---------------------------------------------------------------------------------------------------- Constructors

    RestResource(List<Object> resources, List<Class<?>> providerClasses) {
        this.resources = resources;
        this.providerClasses = providerClasses;
    }
}
