package de.deepamehta.plugins.topicmaps.model;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.awt.Point;

import java.util.HashMap;
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
public class Topicmap implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected Topic topicmapTopic;
    protected Map<Long, TopicmapTopic> topics = new HashMap<Long, TopicmapTopic>();
    protected Map<Long, TopicmapAssociation> assocs = new HashMap<Long, TopicmapAssociation>();

    protected DeepaMehtaService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Loads a topicmap from the DB.
     */
    public Topicmap(long topicmapId, DeepaMehtaService dms, ClientState clientState) {
        this.topicmapTopic = dms.getTopic(topicmapId, true, clientState);   // fetchComposite=true
        this.dms = dms;
        //
        logger.info("Loading topicmap " + getId());
        loadTopics(clientState);
        loadAssociations();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public long getId() {
        return topicmapTopic.getId();
    }

    // ---

    public boolean containsTopic(long topicId) {
        return topics.get(topicId) != null;
    }

    public boolean containsAssociation(long assocId) {
        return assocs.get(assocId) != null;
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject topicmap = new JSONObject();
            topicmap.put("info", topicmapTopic.toJSON());
            topicmap.put("topics", DeepaMehtaUtils.objectsToJSON(topics.values()));
            topicmap.put("assocs", DeepaMehtaUtils.objectsToJSON(assocs.values()));
            return topicmap;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    @Override
    public String toString() {
        return "topicmap " + getId();
    }

    // -------------------------------------------------------------------------------------------- Public Inner Classes

    // Note: there is a client-side equivalent in canvas.js (deepamehta-client plugin)
    public class GridPositioning {

        // Settings
        private final int GRID_DIST_X = 220;    // LABEL_MAX_WIDTH + 20 pixel padding
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

    private void loadTopics(ClientState clientState) {
        ResultSet<RelatedTopic> mapTopics = topicmapTopic.getRelatedTopics("dm4.topicmaps.topic_mapcontext",
            "dm4.core.default", "dm4.topicmaps.topicmap_topic", null, false, true, 0, clientState);
            // othersTopicTypeUri=null, fetchComposite=false, fetchRelatingComposite=true, maxResultSize=0
        for (RelatedTopic mapTopic : mapTopics) {
            CompositeValue visualizationProperties = mapTopic.getAssociation().getCompositeValue();
            addTopic(new TopicmapTopic(mapTopic.getModel(), visualizationProperties));
        }
    }

    private void loadAssociations() {
        Set<RelatedAssociation> mapAssocs = topicmapTopic.getRelatedAssociations("dm4.topicmaps.association_mapcontext",
            "dm4.core.default", "dm4.topicmaps.topicmap_association", null, false, false);
        for (RelatedAssociation mapAssoc : mapAssocs) {
            addAssociation(new TopicmapAssociation(mapAssoc.getModel()));
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
