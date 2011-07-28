package de.deepamehta.plugins.topicmaps.model;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.DeepaMehtaService;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.awt.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



/**
 * An in-memory representation of a topicmap.
 * That is a collection of topics and associations plus their visualization information.
 * <p>
 * The constructor loads the topicmap from the DB.
 * There is a method to get a JSON representation of the topicmap.
 */
public class Topicmap {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected long topicmapId;
    protected DeepaMehtaService dms;

    protected Map<Long, TopicmapTopic> topics = new HashMap();
    protected Map<Long, TopicmapAssociation> assocs = new HashMap();

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Loads a topicmap from the DB.
     */
    public Topicmap(long topicmapId, DeepaMehtaService dms) {
        this.topicmapId = topicmapId;
        this.dms = dms;
        logger.info("Loading topicmap " + topicmapId);
        //
        loadTopics(topicmapId);
        loadAssociations(topicmapId);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public boolean containsTopic(long topicId) {
        return topics.get(topicId) != null;
    }

    public boolean containsAssociation(long assocId) {
        return assocs.get(assocId) != null;
    }

    // ---

    public JSONObject toJSON() throws JSONException {
        JSONArray topics = new JSONArray();
        for (TopicmapTopic topic : this.topics.values()) {
            topics.put(topic.toJSON());
        }
        //
        JSONArray assocs = new JSONArray();
        for (TopicmapAssociation assoc : this.assocs.values()) {
            assocs.put(assoc.toJSON());
        }
        //
        JSONObject topicmap = new JSONObject();
        topicmap.put("topics", topics);
        topicmap.put("assocs", assocs);
        return topicmap;
    }

    @Override
    public String toString() {
        return "topicmap " + topicmapId;
    }

    // -------------------------------------------------------------------------------------------- Public Inner Classes

    // Note: there is a client-side equivalent in canvas.js (deepamehta-client plugin)
    public class GridPositioning {

        // Settings
        private final int GRID_DIST_X = 180;           // 10em (see LABEL_MAX_WIDTH) + 20 pixel padding
        private final int GRID_DIST_Y = 80;
        private final int START_X;
        private final int START_Y = 50;
        private final int MIN_Y = -9999;

        private final int canvasWidth;
        private final int transX;

        private int gridX;
        private int gridY;

        // --- Constructors ---

        public GridPositioning(int canvasWidth, int transX) {
            this.canvasWidth = canvasWidth;
            this.transX = transX;
            START_X = 50 - transX;
            //
            Point startPos = findStartPostition();
            gridX = startPos.x;
            gridY = startPos.y;
        }

        // --- Public Methods ---

        public Point nextPosition() {
            Point pos = new Point(gridX, gridY);
            advancePosition();
            return pos;
        }

        // --- Private Methods ---

        private Point findStartPostition() {
            int maxY = MIN_Y;
            for (TopicmapTopic topic : topics.values()) {
                if (topic.getY() > maxY) {
                    maxY = topic.getY();
                }
            }
            int x = START_X;
            int y = maxY != MIN_Y ? maxY + GRID_DIST_Y : START_Y;
            return new Point(x, y);
        }

        private void advancePosition() {
            if (gridX + GRID_DIST_X + transX > canvasWidth) {
                gridX = START_X;
                gridY += GRID_DIST_Y;
            } else {
                gridX += GRID_DIST_X;
            }
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void loadTopics(long topicmapId) {
        Topic topicmapTopic = dms.getTopic(topicmapId, false, null);
        Set<RelatedTopic> mapTopics = topicmapTopic.getRelatedTopics("dm4.topicmaps.topic_mapcontext",
            null, "dm4.topicmaps.topicmap_topic", null, false, true);   // othersTopicTypeUri=null
                                                                        // fetchComposite=false
        for (RelatedTopic mapTopic : mapTopics) {
            Association refAssoc = mapTopic.getAssociation();
            addTopic(new TopicmapTopic(mapTopic.getModel(), refAssoc.getCompositeValue(), refAssoc.getId()));
        }
    }

    private void loadAssociations(long topicmapId) {
        Topic topicmapTopic = dms.getTopic(topicmapId, false, null);
        Set<RelatedAssociation> mapAssocs = topicmapTopic.getRelatedAssociations("dm4.topicmaps.association_mapcontext",
            null, "dm4.topicmaps.topicmap_association", null, false, false);
        for (RelatedAssociation mapAssoc : mapAssocs) {
            Association refAssoc = mapAssoc.getRelatingAssociation();
            addAssociation(new TopicmapAssociation(mapAssoc.getModel(), refAssoc.getId()));
        }
    }

    // ---

    private void addTopic(TopicmapTopic topic) {
        topics.put(topic.getId(), topic);
    }

    private void addAssociation(TopicmapAssociation assoc) {
        assocs.put(assoc.getId(), assoc);
    }
}
