package de.deepamehta.plugins.topicmaps.model;

import de.deepamehta.plugins.topicmaps.ViewmodelCustomizer;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.awt.Point;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * A topicmap viewmodel: a collection of topics and associations plus their view properties.
 * <p>
 * Features:
 * - load from DB (by constructor).
 * - Serialization to JSON.
 */
public class TopicmapViewmodel implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Topic topicmapTopic;
    private Map<Long, TopicViewmodel> topics = new HashMap();
    private Map<Long, AssociationViewmodel> assocs = new HashMap();

    private DeepaMehtaService dms;
    private List<ViewmodelCustomizer> customizers;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Loads a topicmap from the DB.
     */
    public TopicmapViewmodel(long topicmapId, DeepaMehtaService dms, List<ViewmodelCustomizer> customizers) {
        this.topicmapTopic = dms.getTopic(topicmapId, true);    // fetchComposite=true
        this.dms = dms;
        this.customizers = customizers;
        //
        logger.info("Loading topicmap " + getId());
        loadTopics();
        loadAssociations();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public long getId() {
        return topicmapTopic.getId();
    }

    // ---

    public Iterable<TopicViewmodel> getTopics() {
        return topics.values();
    }

    public Iterable<AssociationViewmodel> getAssociations() {
        return assocs.values();
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
            for (TopicViewmodel topic : topics.values()) {
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

    private void loadTopics() {
        ResultList<RelatedTopic> topics = topicmapTopic.getRelatedTopics("dm4.topicmaps.topic_mapcontext",
            "dm4.core.default", "dm4.topicmaps.topicmap_topic", null, false, true, 0);
            // othersTopicTypeUri=null, fetchComposite=false, fetchRelatingComposite=true, maxResultSize=0
        for (RelatedTopic topic : topics) {
            CompositeValueModel viewProps = topic.getRelatingAssociation().getCompositeValue().getModel();
            invokeViewmodelCustomizers(topic, viewProps);
            addTopic(new TopicViewmodel(topic.getModel(), viewProps));
        }
    }

    private void loadAssociations() {
        List<RelatedAssociation> assocs = topicmapTopic.getRelatedAssociations("dm4.topicmaps.association_mapcontext",
            "dm4.core.default", "dm4.topicmaps.topicmap_association", null, false, false);
        for (RelatedAssociation assoc : assocs) {
            addAssociation(new AssociationViewmodel(assoc.getModel()));
        }
    }

    // ---

    // ### There is a copy in TopicmapsPlugin
    private void invokeViewmodelCustomizers(Topic topic, CompositeValueModel viewProps) {
        for (ViewmodelCustomizer customizer : customizers) {
            invokeViewmodelCustomizer(customizer, topic, viewProps);
        }
    }

    // ### There is a principal copy in TopicmapsPlugin
    private void invokeViewmodelCustomizer(ViewmodelCustomizer customizer, Topic topic, CompositeValueModel viewProps) {
        try {
            customizer.enrichViewProperties(topic, viewProps);
        } catch (Exception e) {
            throw new RuntimeException("Invoking viewmodel customizer for topic " + topic.getId() + " failed " +
                "(customizer=\"" + customizer.getClass().getName() + "\", method=\"enrichViewProperties\")", e);
        }
    }

    // ---

    private void addTopic(TopicViewmodel topic) {
        topics.put(topic.getId(), topic);
    }

    private void addAssociation(AssociationViewmodel assoc) {
        assocs.put(assoc.getId(), assoc);
    }
}
