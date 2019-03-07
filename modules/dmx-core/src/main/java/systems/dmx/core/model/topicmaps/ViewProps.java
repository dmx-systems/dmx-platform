package systems.dmx.core.model.topicmaps;

import systems.dmx.core.DMXObject;
import systems.dmx.core.JSONEnabled;



// Note: ViewTopic/ViewAssoc extends a core model class. Core model classes can only be instantiated through the model
// factory. The newViewTopic/Assoc calls expect a ViewProps object. That's why ViewProps is specified in the Core as
// well despite ViewProps does not extend a core model class.
public interface ViewProps extends Iterable<String>, JSONEnabled {

    Object get(String propUri);

    ViewProps put(String propUri, Object value);

    void store(DMXObject object);

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
