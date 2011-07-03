package de.deepamehta.plugins.topicmaps.model;

import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.TopicModel;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.Map;



/**
 * A topic as contained in a topicmap.
 * That is a generic topic enriched by visualization properties ("x", "y", "visibility").
 * <p>
 * Note: the topic's own properties are not initialized.
 */
public class TopicmapTopic extends TopicModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Composite visualizationProperties;
    private long refId;

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicmapTopic(TopicModel topic, Composite visualizationProperties, long refId) {
        super(topic);
        this.visualizationProperties = visualizationProperties;
        this.refId = refId;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            o.put("visualization", visualizationProperties.toJSON());
            o.put("ref_id", refId);
            return o;
        } catch (JSONException e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // ---

    public int getX() {
        // Note: coordinates can have both formats: double (through JavaScript) and integer (programmatically placed).
        // TODO: store coordinates always as integers
        Object x = visualizationProperties.get("x");
        return x instanceof Double ? ((Double) x).intValue() : (Integer) x;
    }

    public int getY() {
        // Note: coordinates can have both formats: double (through JavaScript) and integer (programmatically placed).
        // TODO: store coordinates always as integers
        Object y = visualizationProperties.get("y");
        return y instanceof Double ? ((Double) y).intValue() : (Integer) y;
    }
}
