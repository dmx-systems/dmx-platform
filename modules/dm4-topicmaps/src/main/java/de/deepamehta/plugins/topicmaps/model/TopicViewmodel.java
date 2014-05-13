package de.deepamehta.plugins.topicmaps.model;

import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.TopicModel;

import org.codehaus.jettison.json.JSONObject;



/**
 * A topic viewmodel as contained in a topicmap viewmodel.
 * That is a generic topic model enriched by view properties ("x", "y", "visibility").
 */
public class TopicViewmodel extends TopicModel {

    // --- Instance Variables ---

    private CompositeValueModel viewProps;

    // --- Constructors ---

    public TopicViewmodel(TopicModel topic, CompositeValueModel viewProps) {
        super(topic);
        this.viewProps = viewProps;
    }

    // --- Public Methods ---

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

    // ---

    // ### not used
    int getX() {
        // Note: coordinates can be both: double (through JavaScript) and integer (programmatically placed).
        // ### TODO: store coordinates always as integers
        Object x = viewProps.getObject("x");
        return x instanceof Double ? ((Double) x).intValue() : (Integer) x;
    }

    // ### used by GridPositioning
    int getY() {
        // Note: coordinates can be both: double (through JavaScript) and integer (programmatically placed).
        // ### TODO: store coordinates always as integers
        Object y = viewProps.getObject("y");
        return y instanceof Double ? ((Double) y).intValue() : (Integer) y;
    }
}
