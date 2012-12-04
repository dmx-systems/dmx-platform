package de.deepamehta.plugins.geomaps.model;

import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.TopicModel;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.Map;



/**
 * A topic as contained in a topicmap. ### FIXDOC
 * That is a generic topic enriched by visualization properties ("x", "y", "visibility").
 * <p>
 * Note: the topic's own properties are not initialized.
 */
public class GeomapTopic extends TopicModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private long refId;

    // ---------------------------------------------------------------------------------------------------- Constructors

    GeomapTopic(TopicModel topic, long refId) {
        super(topic);
        this.refId = refId;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            o.put("ref_id", refId);
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // ---

    /* ### public int getX() {
        Object x = visualizationProperties.get("x");
        return x instanceof Double ? ((Double) x).intValue() : (Integer) x;
    } */

    /* ### public int getY() {
        Object y = visualizationProperties.get("y");
        return y instanceof Double ? ((Double) y).intValue() : (Integer) y;
    } */
}
