package de.deepamehta.plugins.topicmaps.model;

import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.TopicModel;

import org.codehaus.jettison.json.JSONObject;



/**
 * A topic viewmodel as contained in a topicmap viewmodel.
 * <p>
 * That is a generic topic model enriched by view properties. Standard view properties are "dm4.topicmaps.x",
 * "dm4.topicmaps.y", and "dm4.topicmaps.visibility". Additional view properties can be added by plugins (by
 * implementing a Viewmodel Customizer).
 */
public class TopicViewmodel extends TopicModel {

    // --- Instance Variables ---

    private ChildTopicsModel viewProps;

    // --- Constructors ---

    public TopicViewmodel(TopicModel topic, ChildTopicsModel viewProps) {
        super(topic);
        this.viewProps = viewProps;
    }

    // --- Public Methods ---

    public ChildTopicsModel getViewProperties() {
        return viewProps;
    }

    // ---

    /**
     * Convencience method to access the "dm4.topicmaps.x" standard view property.
     */
    public int getX() {
        // Note: coordinates can be both: double (through JavaScript) and integer (programmatically placed).
        // ### TODO: store coordinates always as integers
        Object x = viewProps.getObject("dm4.topicmaps.x");
        return x instanceof Double ? ((Double) x).intValue() : (Integer) x;
    }

    /**
     * Convencience method to access the "dm4.topicmaps.y" standard view property.
     */
    public int getY() {
        // Note: coordinates can be both: double (through JavaScript) and integer (programmatically placed).
        // ### TODO: store coordinates always as integers
        Object y = viewProps.getObject("dm4.topicmaps.y");
        return y instanceof Double ? ((Double) y).intValue() : (Integer) y;
    }

    /**
     * Convencience method to access the "dm4.topicmaps.visibility" standard view property.
     */
    public boolean getVisibility() {
        return viewProps.getBoolean("dm4.topicmaps.visibility");
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            o.put("view_props", viewProps.toJSON());
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }
}
