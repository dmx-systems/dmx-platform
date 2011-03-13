package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;



public class Composite {

    private JSONObject values;

    public Composite(String json) {
        try {
            this.values = new JSONObject(json);
        } catch (Exception e) {
            throw new RuntimeException("Constructing a Composite from a JSON string failed (\"" + json + "\")", e);
        }
    }
}
