package de.deepamehta.core.service;

import de.deepamehta.core.util.UploadedFile;

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

    public String getString(String key) {
        return (String) params.get(key);
    }

    public int getInt(String key) {
        return (Integer) params.get(key);
    }

    public UploadedFile getFile(String key) {
        return (UploadedFile) params.get(key);
    }

    // ---

    @Override
    public String toString() {
        return params.toString();
    }
}
