package systems.dmx.config;

import systems.dmx.core.JSONEnabled;

import org.codehaus.jettison.json.JSONObject;



class ConfigDefs implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private JSONObject json;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ConfigDefs(JSONObject json) {
        this.json = json;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        return json;
    }
}
