package de.deepamehta.core.model;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;



public class CommandParams {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, Object> params = new HashMap();

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public CommandParams(Map<String, Object> params) {
        this.params = params;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public Object get(String key) {
        return params.get(key);
    }

    @Override
    public String toString() {
        return params.toString();
    }
}
