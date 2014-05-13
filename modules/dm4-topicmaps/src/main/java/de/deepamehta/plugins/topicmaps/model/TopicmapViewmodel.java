package de.deepamehta.plugins.topicmaps.model;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.awt.Point;

import java.util.List;
import java.util.logging.Logger;



/**
 * A topicmap viewmodel: a collection of topics and associations plus their view properties.
 * <p>
 * Features:
 * - Serialization to JSON.
 */
public class TopicmapViewmodel implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private TopicModel topicmapTopic;
    private List<TopicViewmodel> topics;
    private List<AssociationViewmodel> assocs;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicmapViewmodel(TopicModel topicmapTopic, List<TopicViewmodel> topics, List<AssociationViewmodel> assocs) {
        this.topicmapTopic = topicmapTopic;
        this.topics = topics;
        this.assocs = assocs;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public long getId() {
        return topicmapTopic.getId();
    }

    // ---

    public Iterable<TopicViewmodel> getTopics() {
        return topics;
    }

    public Iterable<AssociationViewmodel> getAssociations() {
        return assocs;
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject topicmap = new JSONObject();
            topicmap.put("info", topicmapTopic.toJSON());
            topicmap.put("topics", DeepaMehtaUtils.objectsToJSON(topics));
            topicmap.put("assocs", DeepaMehtaUtils.objectsToJSON(assocs));
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

    // Note: there is a client-side equivalent in canvas_view.js (deepamehta-webclient plugin)
    public class GridPositioning {

        // Settings
        private final int GRID_DIST_X = 220;    // MAX_TOPIC_LABEL_WIDTH + 20 pixel padding
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
            for (TopicViewmodel topic : topics) {
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
}
