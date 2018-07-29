package systems.dmx.core.service;

import systems.dmx.core.DMXObject;
import systems.dmx.core.JSONEnabled;

import org.codehaus.jettison.json.JSONObject;



/**
 * A {@link systems.dmx.core.DMXObject}/{@link Directives} pair to be sent as response.
 * <p>
 * The DMXObject is injected via constructor. It is optional.
 * The Directives are the thread-local ones assembled while request processing.
 */
public class DirectivesResponse implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DMXObject object;
    private Directives directives;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public DirectivesResponse() {
        this.object = null;
        initDirectives();
    }

    public DirectivesResponse(DMXObject object) {
        this.object = object;
        initDirectives();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public DMXObject getObject() {
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
            throw new RuntimeException("Serialization failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void initDirectives() {
        directives = Directives.get();
    }
}
