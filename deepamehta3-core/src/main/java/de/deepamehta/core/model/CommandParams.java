package de.deepamehta.core.model;

import de.deepamehta.core.util.JSONHelper;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



public class CommandParams {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, Object> params = new HashMap();

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public CommandParams(JSONObject params) {
        this.params = JSONHelper.toMap(params);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public Object get(String key) {
        return params.get(key);
    }
}
