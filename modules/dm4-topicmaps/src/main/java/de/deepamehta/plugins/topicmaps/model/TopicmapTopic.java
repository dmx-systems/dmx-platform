package de.deepamehta.plugins.topicmaps.model;

import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.TopicModel;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.Map;



/**
 * A topic viewmodel as contained in a topicmap viewmodel.
 * That is a generic topic model enriched by view properties ("x", "y", "visibility").
 *
 * ### TODO: could be renamed to "TopicViewmodel"
 */
public class TopicmapTopic extends TopicModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private CompositeValueModel viewProps;

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicmapTopic(TopicModel topic, CompositeValueModel viewProps) {
        super(topic);
        this.viewProps = viewProps;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            o.put("view", viewProps.toJSON());
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // ---

    public int getX() {
        // Note: coordinates can have both formats: double (through JavaScript) and integer (programmatically placed).
        // ### TODO: store coordinates always as integers
        Object x = viewProps.getObject("x");
        return x instanceof Double ? ((Double) x).intValue() : (Integer) x;
    }

    public int getY() {
        // Note: coordinates can have both formats: double (through JavaScript) and integer (programmatically placed).
        // ### TODO: store coordinates always as integers
        Object y = viewProps.getObject("y");
        return y instanceof Double ? ((Double) y).intValue() : (Integer) y;
    }
}
