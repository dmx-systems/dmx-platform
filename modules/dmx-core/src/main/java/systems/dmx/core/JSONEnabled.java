package systems.dmx.core;

import org.codehaus.jettison.json.JSONObject;



/**
 * A common interface for all entities that provide a JSON representation.
 */
public interface JSONEnabled {

    JSONObject toJSON();
}
