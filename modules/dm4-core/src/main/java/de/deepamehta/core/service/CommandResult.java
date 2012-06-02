package de.deepamehta.core.service;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



public class CommandResult {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private JSONObject result;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public CommandResult() {
        this.result = new JSONObject();
    }

    public CommandResult(JSONObject result) {
        this.result = result;
    }

    public CommandResult(String key, Object value) {
        this();
        put(key, value);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public void put(String key, Object value) {
        try {
            result.put(key, value);
        } catch (Exception e) {
            throw new RuntimeException("Putting entry in " + this + " failed", e);
        }
    }

    public JSONObject toJSON() {
        return result;
    }
}
