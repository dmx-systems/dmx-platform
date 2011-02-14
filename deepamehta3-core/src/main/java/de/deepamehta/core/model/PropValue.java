package de.deepamehta.core.model;

import java.util.HashMap;
import java.util.Map;



public class PropValue {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Object value;

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
      * Called by JAX-RS container to create a Role from a @PathParam or @QueryParam
      */
    public PropValue(String value) {
        this.value = value;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public Object value() {
        return value;
    }
}
