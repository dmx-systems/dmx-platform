package de.deepamehta.config;

import de.deepamehta.core.JSONEnabled;

import org.codehaus.jettison.json.JSONObject;



class ConfigDefinitions implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private JSONObject json;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ConfigDefinitions(JSONObject json) {
        this.json = json;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        return json;
    }
}
