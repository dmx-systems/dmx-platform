package systems.dmx.core.model.topicmaps;

import systems.dmx.core.JSONEnabled;



public interface ViewProps extends Iterable<String>, JSONEnabled {

    Object get(String propUri);

    ViewProps put(String propUri, Object value);

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
