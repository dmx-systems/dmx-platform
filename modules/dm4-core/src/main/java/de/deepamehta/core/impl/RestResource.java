package de.deepamehta.core.impl;

import java.util.List;



class RestResource {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    List<Object> singletons;
    List<Class<?>> providerClasses;

    // ---------------------------------------------------------------------------------------------------- Constructors

    RestResource(List<Object> singletons, List<Class<?>> providerClasses) {
        this.singletons = singletons;
        this.providerClasses = providerClasses;
    }
}
