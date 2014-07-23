package de.deepamehta.core.impl;

import java.util.List;



class RestResources {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    List<Object> singletons;
    List<Class<?>> classes;

    // ---------------------------------------------------------------------------------------------------- Constructors

    RestResources(List<Object> singletons, List<Class<?>> classes) {
        this.singletons = singletons;
        this.classes = classes;
    }
}
