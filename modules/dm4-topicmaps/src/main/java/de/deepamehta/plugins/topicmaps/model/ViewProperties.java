package de.deepamehta.plugins.topicmaps.model;

import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;



public class ViewProperties extends HashMap<String, Object> {

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Note: invoked from JAX-RS message body reader (see Webservice's ObjectProvider.java).
     */
    public ViewProperties(JSONObject viewProps) {
        DeepaMehtaUtils.toMap(viewProps, this);
    }

    // ---

    /**
     * Convenience constructor that initializes the "dm4.topicmaps.x", "dm4.topicmaps.y", and "dm4.topicmaps.visibility"
     * standard view properties.
     */
    public ViewProperties(int x, int y, boolean visibility) {
        put(x, y);
        put(visibility);
    }

    /**
     * Convenience constructor that initializes the "dm4.topicmaps.x" and "dm4.topicmaps.y" standard view properties.
     */
    public ViewProperties(int x, int y) {
        put(x, y);
    }

    /**
     * Convenience constructor that initializes the "dm4.topicmaps.visibility" standard view property.
     */
    public ViewProperties(boolean visibility) {
        put(visibility);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public int getInt(String propUri) {
        return (Integer) get(propUri);
    }

    public boolean getBoolean(String propUri) {
        return (Boolean) get(propUri);
    }

    // ---

    public Iterable<String> propUris() {
        return keySet();
    }

    // ------------------------------------------------------------------------------------------------ Private  Methods

    private void put(int x, int y) {
        put("dm4.topicmaps.x", x);
        put("dm4.topicmaps.y", y);
    }

    private void put(boolean visibility) {
        put("dm4.topicmaps.visibility", visibility);
    }
}
