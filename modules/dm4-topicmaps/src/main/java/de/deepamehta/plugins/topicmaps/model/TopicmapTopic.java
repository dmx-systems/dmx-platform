package de.deepamehta.plugins.topicmaps.model;

import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.TopicModel;

import org.codehaus.jettison.json.JSONObject;



/**
 * A topic as contained in a topicmap.
 * That is a generic topic enriched by visualization properties ("x", "y", "visibility").
 */
public class TopicmapTopic extends TopicModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private CompositeValue visualizationProperties;

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicmapTopic(TopicModel topic, CompositeValue visualizationProperties) {
        super(topic);
        this.visualizationProperties = visualizationProperties;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            o.put("visualization", visualizationProperties.toJSON());
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // ---

    public int getX() {
        // Note: coordinates can have both formats: double (through JavaScript) and integer (programmatically placed).
        // ### TODO: store coordinates always as integers
        Object x = visualizationProperties.get("x");
        return x instanceof Double ? ((Double) x).intValue() : (Integer) x;
    }

    public int getY() {
        // Note: coordinates can have both formats: double (through JavaScript) and integer (programmatically placed).
        // ### TODO: store coordinates always as integers
        Object y = visualizationProperties.get("y");
        return y instanceof Double ? ((Double) y).intValue() : (Integer) y;
    }
}
