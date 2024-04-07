package systems.dmx.core.model.topicmaps;

import systems.dmx.core.DMXObject;
import systems.dmx.core.JSONEnabled;



/**
 * A mapping of prop URIs (strings) to prop values (objects). Used to hold and transport view properties.
 * There are (de)serialzation and store-to-DB facilities.
 * <p>
 * Note: ViewTopic/ViewAssoc extends a core model class. Core model classes can only be instantiated through the model
 * factory. The newViewTopic/Assoc calls expect a ViewProps object. That's why ViewProps is specified in the Core as
 * well despite ViewProps does not extend a core model class.
 */
public interface ViewProps extends Iterable<String>, JSONEnabled {

    /**
     * Returns the prop value mapped to the given prop URI, or <code>null</code> if no value is mapped.
     */
    Object get(String propUri);

    /**
     * Convenience getter.
     */
    String getString(String propUri);

    /**
     * Convenience getter.
     */
    int getInt(String propUri);

    /**
     * Convenience getter.
     */
    boolean getBoolean(String propUri);

    ViewProps set(String propUri, Object value);

    void store(DMXObject object);
}
