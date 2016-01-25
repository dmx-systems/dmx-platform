package de.deepamehta.core.service;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.JSONEnabled;

import org.codehaus.jettison.json.JSONObject;



public class DirectivesResponse implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DeepaMehtaObject object;
    private Directives directives;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public DirectivesResponse() {
        this.object = null;
        initDirectives();
    }

    public DirectivesResponse(DeepaMehtaObject object) {
        this.object = object;
        initDirectives();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public DeepaMehtaObject getObject() {
        return object;
    }

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
