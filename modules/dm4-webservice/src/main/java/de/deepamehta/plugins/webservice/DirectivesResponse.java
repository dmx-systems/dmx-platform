package de.deepamehta.plugins.webservice;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONObject;



class DirectivesResponse implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    JSONEnabled object;
    Directives directives;

    // ---------------------------------------------------------------------------------------------------- Constructors

    DirectivesResponse() {
        this.object = null;
        initDirectives();
    }

    DirectivesResponse(JSONEnabled object) {
        this.object = object;
        initDirectives();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // *** JSONEnabled Implementation ***

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject json = object != null ? object.toJSON() : new JSONObject();
            json.put("directives", directives.toJSONArray());
            return json;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void initDirectives() {
        directives = Directives.get();
    }
}
