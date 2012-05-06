package de.deepamehta.plugins.topicmaps.model;

import de.deepamehta.core.model.AssociationModel;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;



/**
 * An association as contained in a topicmap.
 */
public class TopicmapAssociation extends AssociationModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private long refId;

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicmapAssociation(AssociationModel assoc, long refId) {
        super(assoc);
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
}
