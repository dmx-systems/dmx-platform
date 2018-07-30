package systems.dmx.core.model.topicmaps;

import systems.dmx.core.JSONEnabled;
import systems.dmx.core.util.DMXUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;



public class ViewProperties implements Iterable<String>, JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, Object> viewProps = new HashMap();

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Note: invoked from JAX-RS message body reader (see Webservice's ObjectProvider.java).
     */
    public ViewProperties(JSONObject viewProps) {
        DMXUtils.toMap(viewProps, this.viewProps);
    }

    // ---

    public ViewProperties () {
    }

    /**
     * Convenience constructor that initializes the "dmx.topicmaps.x", "dmx.topicmaps.y", "dmx.topicmaps.visibility",
     * and "dmx.topicmaps.pinned" standard view properties.
     */
    public ViewProperties(int x, int y, boolean visibility, boolean pinned) {
        initPos(x, y);
        initVisibility(visibility);
        initPinned(pinned);
    }

    /**
     * Convenience constructor that initializes the "dmx.topicmaps.x" and "dmx.topicmaps.y" standard view properties.
     */
    public ViewProperties(int x, int y) {
        initPos(x, y);
    }

    /**
     * Convenience constructor that initializes the "dmx.topicmaps.visibility" standard view property.
     */
    public ViewProperties(boolean visibility) {
        initVisibility(visibility);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public Object get(String propUri) {
        return viewProps.get(propUri);
    }

    public ViewProperties put(String propUri, Object value) {
        viewProps.put(propUri, value);
        return this;
    }

    // ---

    /**
     * Convenience getter.
     */
    public int getInt(String propUri) {
        return (Integer) get(propUri);
    }

    /**
     * Convenience getter.
     */
    public boolean getBoolean(String propUri) {
        return (Boolean) get(propUri);
    }

    // ---

    @Override
    public Iterator<String> iterator() {
        return viewProps.keySet().iterator();
    }

    @Override
    public JSONObject toJSON() {
        return new JSONObject(viewProps);
    }

    @Override
    public String toString() {
        return viewProps.toString();
    }

    // ------------------------------------------------------------------------------------------------ Private  Methods

    private void initPos(int x, int y) {
        put("dmx.topicmaps.x", x);
        put("dmx.topicmaps.y", y);
    }

    private void initVisibility(boolean visibility) {
        put("dmx.topicmaps.visibility", visibility);
    }

    private void initPinned(boolean pinned) {
        put("dmx.topicmaps.pinned", pinned);
    }
}
