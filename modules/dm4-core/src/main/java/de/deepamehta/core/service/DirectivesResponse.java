package de.deepamehta.core.service;

import de.deepamehta.core.JSONEnabled;

import org.codehaus.jettison.json.JSONObject;



public class DirectivesResponse implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private JSONEnabled object;
    private Directives directives;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public DirectivesResponse() {
        this.object = null;
        initDirectives();
    }

    public DirectivesResponse(JSONEnabled object) {
        this.object = object;
        initDirectives();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public Directives getDirectives() {
        return directives;
    }

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
