package de.deepamehta.plugins.topicmaps;

import de.deepamehta.plugins.topicmaps.model.Topicmap;
import de.deepamehta.plugins.topicmaps.service.TopicmapsService;

import de.deepamehta.core.model.Properties;
import de.deepamehta.core.model.PropValue;
import de.deepamehta.core.model.Relation;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.service.Plugin;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



@Path("/")
@Consumes("application/json")
@Produces("application/json")
public class TopicmapsPlugin extends Plugin implements TopicmapsService {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************************
    // *** Core Hooks (called from DeepaMehta 3 Core) ***
    // **************************************************



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
            PropValue relation_id = dms.getTopicProperty(topic.id, "de/deepamehta/core/property/RelationID");
            topic.setProperty("de/deepamehta/core/property/RelationID", relation_id);
        }
    }

    @Override
    public void providePropertiesHook(Relation relation) {
        if (relation.typeId.equals("TOPICMAP_TOPIC")) {
            // transfer all relation properties
            Properties properties = dms.getRelation(relation.id).getProperties();
            for (String key : properties.keySet()) {
                relation.setProperty(key, properties.get(key));
            }
        }
    }



    // **********************
    // *** Plugin Service ***
    // **********************



    @GET
    @Path("/{id}")
    @Override
    public Topicmap getTopicmap(@PathParam("id") long topicmapId) {
        return new Topicmap(topicmapId, dms);
    }

    @PUT
    @Path("/{id}/topic/{topicId}/{x}/{y}")
    @Override
    public long addTopicToTopicmap(@PathParam("id") long topicmapId,
                                   @PathParam("topicId") long topicId, @PathParam("x") int x, @PathParam("y") int y) {
        Properties properties = new Properties();
        properties.put("x", x);
        properties.put("y", y);
        properties.put("visibility", true);
        Relation refRel = dms.createRelation("TOPICMAP_TOPIC", topicmapId, topicId, properties);
        return refRel.id;
    }

    @PUT
    @Path("/{id}/relation/{relationId}")
    @Override
    public long addRelationToTopicmap(@PathParam("id") long topicmapId, @PathParam("relationId") long relationId) {
        // FIME: do this in a transaction.
        Properties properties = new Properties();
        properties.put("de/deepamehta/core/property/RelationID", relationId);
        Topic refTopic = dms.createTopic("de/deepamehta/core/topictype/TopicmapRelationRef", properties, null);
        dms.createRelation("RELATION", topicmapId, refTopic.id, null);
        return refTopic.id;
    }

    @DELETE
    @Path("/{id}/relation/{relationId}/{refId}")
    @Override
    public void removeRelationFromTopicmap(@PathParam("id") long topicmapId,
                                           @PathParam("relationId") long relationId, @PathParam("refId") long refId) {
        removeRelationFromTopicmap(refId);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * @param   refId   ID of the "Topicmap Relation Ref" topic of the relation to remove.
     */
    public void removeRelationFromTopicmap(long refId) {
        dms.deleteTopic(refId);
    }
}
