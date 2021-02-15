package systems.dmx.core;

import org.codehaus.jettison.json.JSONObject;



/**
 * A common interface for all entities that provide a JSON representation.
 */
public interface JSONEnabled {

    JSONObject toJSON();

    default public String dump() {
        try {
            return getClass().getSimpleName() + " " + toJSON().toString(4);
        } catch (Exception e) {
            throw new RuntimeException("Prettyprinting failed", e);
        }
    }
}
