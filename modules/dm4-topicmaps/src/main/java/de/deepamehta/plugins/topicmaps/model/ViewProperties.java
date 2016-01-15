package de.deepamehta.plugins.topicmaps.model;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.util.DeepaMehtaUtils;

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
        DeepaMehtaUtils.toMap(viewProps, this.viewProps);
    }

    // ---

    /**
     * Convenience constructor that initializes the "dm4.topicmaps.x", "dm4.topicmaps.y", and "dm4.topicmaps.visibility"
     * standard view properties.
     */
    public ViewProperties(int x, int y, boolean visibility) {
        initPos(x, y);
        initVisibility(visibility);
    }

    /**
     * Convenience constructor that initializes the "dm4.topicmaps.x" and "dm4.topicmaps.y" standard view properties.
     */
    public ViewProperties(int x, int y) {
        initPos(x, y);
    }

    /**
     * Convenience constructor that initializes the "dm4.topicmaps.visibility" standard view property.
     */
    public ViewProperties(boolean visibility) {
        initVisibility(visibility);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public Object get(String propUri) {
        return viewProps.get(propUri);
    }

    public void put(String propUri, Object value) {
        viewProps.put(propUri, value);
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

    // ------------------------------------------------------------------------------------------------ Private  Methods

    private void initPos(int x, int y) {
        put("dm4.topicmaps.x", x);
        put("dm4.topicmaps.y", y);
    }

    private void initVisibility(boolean visibility) {
        put("dm4.topicmaps.visibility", visibility);
    }
}
