package de.deepamehta.plugins.topicmaps;

import de.deepamehta.plugins.topicmaps.model.Topicmap;
import de.deepamehta.plugins.topicmaps.service.TopicmapsService;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.TopicRoleModel;
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



    /* @Override
    public void postDeleteRelationHook(long relationId) {
        // remove the relation from all topicmaps
        List<Topic> refTopics = dms.getTopics("de/deepamehta/core/property/RelationID", relationId);
        logger.info("### Removing relation " + relationId + " from " + refTopics.size() + " topicmaps");
        for (Topic refTopic : refTopics) {
            removeAssociationFromTopicmap(refTopic.id);
        }
    } */

    // ---

    /* ### @Override
    public void providePropertiesHook(Topic topic) {
        if (topic.typeUri.equals("de/deepamehta/core/topictype/TopicmapRelationRef")) {
            PropValue relation_id = dms.getTopicProperty(topic.id, "de/deepamehta/core/property/RelationID");
            topic.setProperty("de/deepamehta/core/property/RelationID", relation_id);
        }
    } */

    /* ### @Override
    public void providePropertiesHook(Relation relation) {
        if (relation.typeId.equals("TOPICMAP_TOPIC")) {
            // transfer all relation properties
            Properties properties = dms.getRelation(relation.id).getProperties();
            for (String key : properties.keySet()) {
                relation.setProperty(key, properties.get(key));
            }
        }
    } */



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
    @Path("/{id}/topic/{topic_id}/{x}/{y}")
    @Override
    public long addTopicToTopicmap(@PathParam("id") long topicmapId, @PathParam("topic_id") long topicId,
                                   @PathParam("x") int x, @PathParam("y") int y) {
        AssociationModel model = new AssociationModel("dm3.topicmaps.topic_mapcontext",
            new TopicRoleModel(topicmapId, "dm3.topicmaps.topicmap"),
            new TopicRoleModel(topicId,    "dm3.topicmaps.topicmap_topic")
        );
        model.setComposite(new Composite()
            .put("dm3.topicmaps.x", x)
            .put("dm3.topicmaps.y", y)
            .put("dm3.topicmaps.visibility", true));
        //
        Association refAssoc = dms.createAssociation(model, null);     // FIXME: clientContext=null
        return refAssoc.getId();
    }

    /* @PUT
    @Path("/{id}/association/{assoc_id}")
    @Override
    public long addRelationToTopicmap(@PathParam("id") long topicmapId, @PathParam("assoc_id") long assocId) {
        // FIME: do this in a transaction.
        Properties properties = new Properties();
        properties.put("de/deepamehta/core/property/RelationID", assocId);
        Topic refTopic = dms.createTopic("de/deepamehta/core/topictype/TopicmapRelationRef", properties, null);
        dms.createRelation("RELATION", topicmapId, refTopic.id, null);
        return refTopic.id;
    } */

    @DELETE
    @Path("/{id}/association/{assoc_id}/{refId}")
    @Override
    public void removeAssociationFromTopicmap(@PathParam("id") long topicmapId,
                                              @PathParam("assoc_id") long assocId,
                                              @PathParam("refId") long refId) {
        removeAssociationFromTopicmap(refId);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * @param   refId   ID of the "Association Mapcontext" association that relates to the association to remove.
     */
    private void removeAssociationFromTopicmap(long refId) {
        dms.deleteAssociation(refId, null);     // clientContext=null
    }
}
