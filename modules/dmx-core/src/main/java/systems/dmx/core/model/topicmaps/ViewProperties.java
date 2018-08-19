package systems.dmx.core.model.topicmaps;

import systems.dmx.core.JSONEnabled;



public interface ViewProperties extends Iterable<String>, JSONEnabled {

    Object get(String propUri);

    ViewProperties put(String propUri, Object value);

    // ---

    /**
     * Convenience getter.
     */
    int getInt(String propUri);

    /**
     * Convenience getter.
     */
    boolean getBoolean(String propUri);
}
