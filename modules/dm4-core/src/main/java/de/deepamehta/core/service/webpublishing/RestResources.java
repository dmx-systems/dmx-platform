package de.deepamehta.core.service.webpublishing;

import java.util.List;



public class RestResources {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    public List<Object> singletons;
    public List<Class<?>> classes;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public RestResources(List<Object> singletons, List<Class<?>> classes) {
        this.singletons = singletons;
        this.classes = classes;
    }
}
