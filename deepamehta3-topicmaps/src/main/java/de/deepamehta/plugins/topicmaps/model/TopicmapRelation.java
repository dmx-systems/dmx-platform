package de.deepamehta.plugins.topicmaps.model;

import de.deepamehta.core.model.Relation;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;



/**
 * A relation as contained in a topicmap.
 */
public class TopicmapRelation extends Relation {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private long refId;

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicmapRelation(Relation relation, long refId) {
        super(relation);
        this.refId = refId;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            o.put("ref_id", refId);
            return o;
        } catch (JSONException e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }
}
