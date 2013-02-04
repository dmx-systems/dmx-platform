package de.deepamehta.plugins.topicmaps.model;

import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.TopicModel;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.Map;



/**
 * A topic as contained in a topicmap.
 * That is a generic topic enriched by visualization properties ("x", "y", "visibility").
 */
public class TopicmapTopic extends TopicModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private ChildTopicsModel visualizationProps;

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicmapTopic(TopicModel topic, ChildTopicsModel visualizationProps) {
        super(topic);
        this.visualizationProps = visualizationProps;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            o.put("visualization", visualizationProps.toJSON());
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // ---

    public int getX() {
        // Note: coordinates can have both formats: double (through JavaScript) and integer (programmatically placed).
        // ### TODO: store coordinates always as integers
        Object x = visualizationProps.getObject("x");
        return x instanceof Double ? ((Double) x).intValue() : (Integer) x;
    }

    public int getY() {
        // Note: coordinates can have both formats: double (through JavaScript) and integer (programmatically placed).
        // ### TODO: store coordinates always as integers
        Object y = visualizationProps.getObject("y");
        return y instanceof Double ? ((Double) y).intValue() : (Integer) y;
    }
}
