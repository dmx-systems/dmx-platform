package de.deepamehta.plugins.topicmaps;

import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.Relation;
import de.deepamehta.core.service.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



public class TopicmapsPlugin extends Plugin {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************
    // *** Overriding Hooks ***
    // ************************



    @Override
    public void postDeleteRelationHook(long relationId) {
        // remove the relation from all topicmaps
        List<Topic> refTopics = dms.getTopics("de/deepamehta/core/property/RelationID", relationId);
        logger.info("### Removing relation " + relationId + " from " + refTopics.size() + " topicmaps");
        for (Topic refTopic : refTopics) {
            removeRelationFromTopicmap(refTopic.id);
        }
    }

    // ---

    @Override
    public void providePropertiesHook(Topic topic) {
        if (topic.typeUri.equals("de/deepamehta/core/topictype/TopicmapRelationRef")) {
            Object relation_id = dms.getTopicProperty(topic.id, "de/deepamehta/core/property/RelationID");
            topic.setProperty("de/deepamehta/core/property/RelationID", relation_id);
        }
    }

    @Override
    public void providePropertiesHook(Relation relation) {
        if (relation.typeId.equals("TOPICMAP_TOPIC")) {
            // transfer all relation properties
            Map<String, Object> properties = dms.getRelation(relation.id).getProperties();
            for (String key : properties.keySet()) {
                relation.setProperty(key, properties.get(key));
            }
        }
    }



    // *****************
    // *** Utilities ***
    // *****************



    public long addTopicToTopicmap(long topicId, int x, int y, long topicmapId) {
        Map properties = new HashMap();
        properties.put("x", x);
        properties.put("y", y);
        properties.put("visibility", true);
        Relation refRel = dms.createRelation("TOPICMAP_TOPIC", topicmapId, topicId, properties);
        return refRel.id;
    }

    public long addRelationToTopicmap(long relationId, long topicmapId) {
        // TODO: do this in a transaction. Extend the core service to let the caller begin a transaction.
        Map properties = new HashMap();
        properties.put("de/deepamehta/core/property/RelationID", relationId);
        Topic refTopic = dms.createTopic("de/deepamehta/core/topictype/TopicmapRelationRef", properties, null);
        dms.createRelation("RELATION", topicmapId, refTopic.id, null);
        return refTopic.id;
    }

    /**
     * @param   refTopicId  ID of the "Topicmap Relation Ref" topic of the relation to remove.
     */
    public void removeRelationFromTopicmap(long refTopicId) {
        dms.deleteTopic(refTopicId);
    }
}
