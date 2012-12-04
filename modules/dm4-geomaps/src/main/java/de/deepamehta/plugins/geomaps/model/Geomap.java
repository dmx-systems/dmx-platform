package de.deepamehta.plugins.geomaps.model;

import de.deepamehta.core.Association;
import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONArray;
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

    protected Topic geomapTopic;
    protected Map<Long, GeomapTopic> topics = new HashMap();

    protected DeepaMehtaService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Loads a topicmap from the DB.
     */
    public Geomap(long geomapId, DeepaMehtaService dms) {
        this.geomapTopic = dms.getTopic(geomapId, true, null);      // fetchComposite=true, clientContext=null
        this.dms = dms;
        //
        logger.info("Loading geomap " + getId());
        loadTopics();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public long getId() {
        return geomapTopic.getId();
    }

    // ---

    public static ResultSet<RelatedTopic> fetchGeomapTopics(long geomapId, DeepaMehtaService dms) {
        Topic geomapTopic = dms.getTopic(geomapId, false, null);    // fetchComposite=false, clientContext=null
        return fetchGeomapTopics(geomapTopic, false);               // fetchComposite=false
    }

    public boolean containsTopic(long topicId) {
        return topics.get(topicId) != null;
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject topicmap = new JSONObject();
            topicmap.put("info", geomapTopic.toJSON());
            topicmap.put("topics", DeepaMehtaUtils.objectsToJSON(topics.values()));
            return topicmap;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    @Override
    public String toString() {
        return "geomap " + getId();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void loadTopics() {
        ResultSet<RelatedTopic> mapTopics = fetchGeomapTopics(geomapTopic, true);     // fetchComposite=true
        for (RelatedTopic mapTopic : mapTopics) {
            Association refAssoc = mapTopic.getAssociation();
            addTopic(new GeomapTopic(mapTopic.getModel(), refAssoc.getId()));
        }
    }

    private static ResultSet<RelatedTopic> fetchGeomapTopics(Topic geomapTopic, boolean fetchComposite) {
        return geomapTopic.getRelatedTopics("dm4.geomaps.geotopic_mapcontext", "dm4.core.default",
            "dm4.topicmaps.topicmap_topic", "dm4.geomaps.geo_coordinate", fetchComposite, false, 0, null);
            // fetchRelatingComposite=false, maxResultSize=0, clientContext=null
    }

    private void addTopic(GeomapTopic topic) {
        topics.put(topic.getId(), topic);
    }
}
