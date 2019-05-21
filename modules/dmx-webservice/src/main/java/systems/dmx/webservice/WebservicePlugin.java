package systems.dmx.webservice;

import systems.dmx.core.Association;
import systems.dmx.core.AssociationType;
import systems.dmx.core.DMXObject;
import systems.dmx.core.JSONEnabled;
import systems.dmx.core.RelatedAssociation;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.model.AssociationTypeModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicTypeModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.DirectivesResponse;
import systems.dmx.core.service.PluginInfo;
import systems.dmx.core.service.Transactional;
import systems.dmx.core.util.IdList;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import javax.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * REST API for {@link systems.dmx.core.service.CoreService}.
 */
@Path("/core")
@Consumes("application/json")
@Produces("application/json")
public class WebservicePlugin extends PluginActivator {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Context
    private HttpServletRequest request;

    private Messenger me = new Messenger("systems.dmx.webclient");

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Topics ===

    // Note: the "include_childs" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topic/{id}")
    public Topic getTopic(@PathParam("id") long topicId) {
        return dmx.getTopic(topicId);
    }

    // Note: the "include_childs" query parameter is handled by the core's JerseyResponseFilter
    // ### TODO: change URI template to "/topic/uri/{uri}"
    @GET
    @Path("/topic/by_uri/{uri}")
    public Topic getTopicByUri(@PathParam("uri") String uri) {
        return dmx.getTopicByUri(uri);
    }

    // Note: the "include_childs" query parameter is handled by the core's JerseyResponseFilter
    // ### TODO: change URI template
    @GET
    @Path("/topic/by_value/{key}/{value}")
    public Topic getTopicByValue(@PathParam("key") String key, @PathParam("value") SimpleValue value) {
        return dmx.getTopicByValue(key, value);
    }

    // Note: the "include_childs" query parameter is handled by the core's JerseyResponseFilter
    // ### TODO: change URI template
    @GET
    @Path("/topic/multi/by_value/{key}/{value}")
    public List<Topic> getTopicsByValue(@PathParam("key") String key, @PathParam("value") SimpleValue value) {
        return dmx.getTopicsByValue(key, value);
    }

    // Note: the "include_childs" query parameter is handled by the core's JerseyResponseFilter
    // ### TODO: change URI template
    @GET
    @Path("/topic/by_type/{topic_type_uri}")
    public List<Topic> getTopicsByType(@PathParam("topic_type_uri") String topicTypeUri) {
        return dmx.getTopicsByType(topicTypeUri);
    }

    // Note: the "include_childs" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topic")
    public List<Topic> searchTopics(@QueryParam("search") String searchTerm, @QueryParam("field") String fieldUri) {
        return dmx.searchTopics(searchTerm, fieldUri);
    }

    @POST
    @Path("/topic")
    @Transactional
    public DirectivesResponse createTopic(TopicModel model) {
        return new DirectivesResponse(dmx.createTopic(model));
    }

    @PUT
    @Path("/topic/{id}")
    @Transactional
    public DirectivesResponse updateTopic(@PathParam("id") long topicId, TopicModel model) {
        if (model.getId() != -1 && topicId != model.getId()) {
            throw new RuntimeException("ID mismatch in update request");
        }
        model.setId(topicId);
        dmx.updateTopic(model);
        return new DirectivesResponse();
    }

    @DELETE
    @Path("/topic/{id}")
    @Transactional
    public DirectivesResponse deleteTopic(@PathParam("id") long topicId) {
        dmx.deleteTopic(topicId);
        return new DirectivesResponse();
    }



    // === Associations ===

    // Note: the "include_childs" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/association/{id}")
    public Association getAssociation(@PathParam("id") long assocId) {
        return dmx.getAssociation(assocId);
    }

    // Note: the "include_childs" query parameter is handled by the core's JerseyResponseFilter
    // ### TODO: change URI template
    @GET
    @Path("/assoc/by_value/{key}/{value}")
    public Association getAssociationByValue(@PathParam("key") String key, @PathParam("value") SimpleValue value) {
        return dmx.getAssociationByValue(key, value);
    }

    // Note: the "include_childs" query parameter is handled by the core's JerseyResponseFilter
    // ### TODO: change URI template
    @GET
    @Path("/assoc/multi/by_value/{key}/{value}")
    public List<Association> getAssociationsByValue(@PathParam("key") String key,
                                                    @PathParam("value") SimpleValue value) {
        return dmx.getAssociationsByValue(key, value);
    }

    // Note: the "include_childs" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/association/{assoc_type_uri}/{topic1_id}/{topic2_id}/{role_type1_uri}/{role_type2_uri}")
    public Association getAssociation(@PathParam("assoc_type_uri") String assocTypeUri,
                   @PathParam("topic1_id") long topic1Id, @PathParam("topic2_id") long topic2Id,
                   @PathParam("role_type1_uri") String roleTypeUri1, @PathParam("role_type2_uri") String roleTypeUri2) {
        return dmx.getAssociation(assocTypeUri, topic1Id, topic2Id, roleTypeUri1, roleTypeUri2);
    }

    // ---

    // Note: the "include_childs" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/association/multiple/{topic1_id}/{topic2_id}")
    public List<Association> getAssociations(@PathParam("topic1_id") long topic1Id,
                                             @PathParam("topic2_id") long topic2Id) {
        return dmx.getAssociations(topic1Id, topic2Id);
    }

    // Note: the "include_childs" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/association/multiple/{topic1_id}/{topic2_id}/{assoc_type_uri}")
    public List<Association> getAssociations(@PathParam("topic1_id") long topic1Id,
                                             @PathParam("topic2_id") long topic2Id,
                                             @PathParam("assoc_type_uri") String assocTypeUri) {
        return dmx.getAssociations(topic1Id, topic2Id, assocTypeUri);
    }

    // ---

    @POST
    @Path("/association")
    @Transactional
    public DirectivesResponse createAssociation(AssociationModel model) {
        return new DirectivesResponse(dmx.createAssociation(model));
    }

    @PUT
    @Path("/association/{id}")
    @Transactional
    public DirectivesResponse updateAssociation(@PathParam("id") long assocId, AssociationModel model) {
        if (model.getId() != -1 && assocId != model.getId()) {
            throw new RuntimeException("ID mismatch in update request");
        }
        model.setId(assocId);
        dmx.updateAssociation(model);
        return new DirectivesResponse();
    }

    @DELETE
    @Path("/association/{id}")
    @Transactional
    public DirectivesResponse deleteAssociation(@PathParam("id") long assocId) {
        dmx.deleteAssociation(assocId);
        return new DirectivesResponse();
    }



    // === Topic Types ===

    @GET
    @Path("/topictype/{uri}")
    public TopicType getTopicType(@PathParam("uri") String uri) {
        return dmx.getTopicType(uri);
    }

    @GET
    @Path("/topictype/topic/{id}")
    public TopicType getTopicTypeImplicitly(@PathParam("id") long topicId) {
        return dmx.getTopicTypeImplicitly(topicId);
    }

    @GET
    @Path("/topictype/all")
    public List<TopicType> getAllTopicTypes() {
        return dmx.getAllTopicTypes();
    }

    @POST
    @Path("/topictype")
    @Transactional
    public TopicType createTopicType(TopicTypeModel model) {
        TopicType topicType = dmx.createTopicType(model);
        me.newTopicType(topicType);
        return topicType;
    }

    @PUT
    @Path("/topictype")
    @Transactional
    public DirectivesResponse updateTopicType(TopicTypeModel model) {
        dmx.updateTopicType(model);
        return new DirectivesResponse();
    }

    @DELETE
    @Path("/topictype/{uri}")
    @Transactional
    public DirectivesResponse deleteTopicType(@PathParam("uri") String uri) {
        dmx.deleteTopicType(uri);
        return new DirectivesResponse();
    }



    // === Association Types ===

    @GET
    @Path("/assoctype/{uri}")
    public AssociationType getAssociationType(@PathParam("uri") String uri) {
        return dmx.getAssociationType(uri);
    }

    @GET
    @Path("/assoctype/assoc/{id}")
    public AssociationType getAssociationTypeImplicitly(@PathParam("id") long assocId) {
        return dmx.getAssociationTypeImplicitly(assocId);
    }

    @GET
    @Path("/assoctype/all")
    public List<AssociationType> getAssociationAllTypes() {
        return dmx.getAllAssociationTypes();
    }

    @POST
    @Path("/assoctype")
    @Transactional
    public AssociationType createAssociationType(AssociationTypeModel model) {
        AssociationType assocType = dmx.createAssociationType(model);
        me.newAssocType(assocType);
        return assocType;
    }

    @PUT
    @Path("/assoctype")
    @Transactional
    public DirectivesResponse updateAssociationType(AssociationTypeModel model) {
        dmx.updateAssociationType(model);
        return new DirectivesResponse();
    }

    @DELETE
    @Path("/assoctype/{uri}")
    @Transactional
    public DirectivesResponse deleteAssociationType(@PathParam("uri") String uri) {
        dmx.deleteAssociationType(uri);
        return new DirectivesResponse();
    }



    // === Role Types ===

    @POST
    @Path("/roletype")
    @Transactional
    public Topic createRoleType(TopicModel model) {
        return dmx.createRoleType(model);
    }



    // === Plugins ===

    @GET
    @Path("/plugin")
    public List<PluginInfo> getPluginInfo() {
        return dmx.getPluginInfo();
    }



    // **********************
    // *** Topic REST API ***
    // **********************



    // Note: the "include_childs" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topic/{id}/related_topics")
    public List<RelatedTopic> getTopicRelatedTopics(@PathParam("id")                     long topicId,
                                                    @QueryParam("assoc_type_uri")        String assocTypeUri,
                                                    @QueryParam("my_role_type_uri")      String myRoleTypeUri,
                                                    @QueryParam("others_role_type_uri")  String othersRoleTypeUri,
                                                    @QueryParam("others_topic_type_uri") String othersTopicTypeUri) {
        Topic topic = dmx.getTopic(topicId);
        return getRelatedTopics(topic, "topic", assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
    }

    // Note: the "include_childs" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topic/{id}/related_assocs")
    public List<RelatedAssociation> getTopicRelatedAssociations(@PathParam("id")            long topicId,
                                                       @QueryParam("assoc_type_uri")        String assocTypeUri,
                                                       @QueryParam("my_role_type_uri")      String myRoleTypeUri,
                                                       @QueryParam("others_role_type_uri")  String othersRoleTypeUri,
                                                       @QueryParam("others_assoc_type_uri") String othersAssocTypeUri) {
        Topic topic = dmx.getTopic(topicId);
        return getRelatedAssociations(topic, "topic", assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri);
    }



    // ****************************
    // *** Association REST API ***
    // ****************************



    // Note: the "include_childs" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/association/{id}/related_topics")
    public List<RelatedTopic> getAssociationRelatedTopics(@PathParam("id")                  long assocId,
                                                       @QueryParam("assoc_type_uri")        String assocTypeUri,
                                                       @QueryParam("my_role_type_uri")      String myRoleTypeUri,
                                                       @QueryParam("others_role_type_uri")  String othersRoleTypeUri,
                                                       @QueryParam("others_topic_type_uri") String othersTopicTypeUri) {
        Association assoc = dmx.getAssociation(assocId);
        return getRelatedTopics(assoc, "association", assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri);
    }

    // Note: the "include_childs" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/association/{id}/related_assocs")
    public List<RelatedAssociation> getAssociationRelatedAssociations(@PathParam("id")      long assocId,
                                                       @QueryParam("assoc_type_uri")        String assocTypeUri,
                                                       @QueryParam("my_role_type_uri")      String myRoleTypeUri,
                                                       @QueryParam("others_role_type_uri")  String othersRoleTypeUri,
                                                       @QueryParam("others_assoc_type_uri") String othersAssocTypeUri) {
        Association assoc = dmx.getAssociation(assocId);
        return getRelatedAssociations(assoc, "association", assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri);
    }



    // **********************
    // *** Multi REST API ***
    // **********************



    @DELETE
    @Path("/topics/{topicIds}")
    @Transactional
    public DirectivesResponse deleteTopics(@PathParam("topicIds") IdList topicIds) {
        return deleteMulti(topicIds, new IdList());
    }

    @DELETE
    @Path("/assocs/{assocIds}")
    @Transactional
    public DirectivesResponse deleteAssocs(@PathParam("assocIds") IdList assocIds) {
        return deleteMulti(new IdList(), assocIds);
    }

    @DELETE
    @Path("/topics/{topicIds}/assocs/{assocIds}")
    @Transactional
    public DirectivesResponse deleteMulti(@PathParam("topicIds") IdList topicIds,
                                          @PathParam("assocIds") IdList assocIds) {
        logger.info("topicIds=" + topicIds + ", assocIds=" + assocIds);
        for (long id : topicIds) {
            deleteAnyTopic(id);
        }
        for (long id : assocIds) {
            dmx.deleteAssociation(id);
        }
        return new DirectivesResponse();
    }



    // ***************************
    // *** WebSockets REST API ***
    // ***************************



    @GET
    @Path("/websockets")
    public JSONEnabled getWebSocketsConfig() {
        return new JSONEnabled() {
            @Override
            public JSONObject toJSON() {
                try {
                    return new JSONObject().put("dmx.websockets.url", dmx.getWebSocketsService().getWebSocketsURL());
                } catch (JSONException e) {
                    throw new RuntimeException("Serializing the WebSockets configuration failed", e);
                }
            }
        };
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private List<RelatedTopic> getRelatedTopics(DMXObject object, String objectInfo, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersTopicTypeUri) {
        String operation = "Fetching related topics of " + objectInfo + " " + object.getId();
        String paramInfo = "(assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri +
            "\", othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersTopicTypeUri=\"" + othersTopicTypeUri + "\")";
        try {
            logger.fine(operation + " " + paramInfo);
            return object.getRelatedTopics(assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed " + paramInfo, e);
        }
    }

    private List<RelatedAssociation> getRelatedAssociations(DMXObject object, String objectInfo,
                       String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        String operation = "Fetching related associations of " + objectInfo + " " + object.getId();
        String paramInfo = "(assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri +
            "\", othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersAssocTypeUri=\"" + othersAssocTypeUri + "\")";
        try {
            logger.fine(operation + " " + paramInfo);
            return object.getRelatedAssociations(assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersAssocTypeUri);
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed " + paramInfo, e);
        }
    }

    // ---

    // TODO: move this logic to dmx.deleteTopic() so that it can delete types as well? (types ARE topics after all)
    private void deleteAnyTopic(long id) {
        Topic t = dmx.getTopic(id);
        String typeUri = t.getTypeUri();
        if (typeUri.equals("dmx.core.topic_type")) {
            dmx.deleteTopicType(t.getUri());
        } else if (typeUri.equals("dmx.core.assoc_type")) {
            dmx.deleteAssociationType(t.getUri());
        } else {
            dmx.deleteTopic(id);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Classes

    // Note: client-sync for new types is only performed when type is created through REST API, not when type is created
    // through Core Service (e.g. while running a migration). This is because message-to-all-but-one requires a request.
    // Technically the Core Service is not a JAX-RS root resource, so injection (e.g. the request) does not work there.
    private class Messenger {

        private String pluginUri;

        private Messenger(String pluginUri) {
            this.pluginUri = pluginUri;
        }

        // ---

        private void newTopicType(TopicType topicType) {
            try {
                messageToAllButOne(new JSONObject()
                    .put("type", "newTopicType")
                    .put("args", new JSONObject()
                        .put("topicType", topicType.toJSON())
                    )
                );
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error while sending a \"newTopicType\" message:", e);
            }
        }

        private void newAssocType(AssociationType assocType) {
            try {
                messageToAllButOne(new JSONObject()
                    .put("type", "newAssocType")
                    .put("args", new JSONObject()
                        .put("assocType", assocType.toJSON())
                    )
                );
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error while sending a \"newAssocType\" message:", e);
            }
        }

        // ---

        private void messageToAllButOne(JSONObject message) {
            dmx.getWebSocketsService().messageToAllButOne(request, pluginUri, message.toString());
        }
    }
}
