package de.deepamehta.plugins.geomaps.model;

import de.deepamehta.core.Association;
import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.util.JSONHelper;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.awt.Point;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



/**
 * A topicmap model: a collection of topics and associations plus their visualization information.
 * <p>
 * Features:
 * - load from DB (by constructor).
 * - Serialization to JSON.
 */
public class Geomap implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected Topic topicmapTopic;
    protected Map<Long, GeomapTopic> topics = new HashMap();

    protected DeepaMehtaService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Loads a topicmap from the DB.
     */
    public Geomap(long topicmapId, DeepaMehtaService dms) {
        this.topicmapTopic = dms.getTopic(topicmapId, true, null);  // fetchComposite=true
        this.dms = dms;
        //
        logger.info("Loading geomap " + getId());
        loadTopics();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public long getId() {
        return topicmapTopic.getId();
    }

    // ---

    public boolean containsTopic(long topicId) {
        return topics.get(topicId) != null;
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject topicmap = new JSONObject();
            topicmap.put("info", topicmapTopic.toJSON());
            topicmap.put("topics", JSONHelper.objectsToJSON(topics.values()));
            return topicmap;
        } catch (JSONException e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    @Override
    public String toString() {
        return "geomap " + getId();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void loadTopics() {
        ResultSet<RelatedTopic> mapTopics = topicmapTopic.getRelatedTopics("dm4.geomaps.geotopic_mapcontext",
            "dm4.core.default", "dm4.topicmaps.topicmap_topic", null, true, false, 0);  // othersTopicTypeUri=null
                                                                                        // fetchComposite=true
                                                                                        // fetchRelatingComposite=false
                                                                                        // maxResultSize=0
        for (RelatedTopic mapTopic : mapTopics) {
            Association refAssoc = mapTopic.getAssociation();
            addTopic(new GeomapTopic(mapTopic.getModel(), refAssoc.getId()));
        }
    }

    private void addTopic(GeomapTopic topic) {
        topics.put(topic.getId(), topic);
    }
}
