package systems.dmx.webservice;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.Assoc;
import systems.dmx.core.AssocType;
import systems.dmx.core.DMXObject;
import systems.dmx.core.DMXType;
import systems.dmx.core.JSONEnabled;
import systems.dmx.core.QueryResult;
import systems.dmx.core.RelatedAssoc;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.AssocTypeModel;
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

import java.util.Iterator;
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

    // Note: the "children" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topic/{id}")
    public Topic getTopic(@PathParam("id") long topicId) {
        return dmx.getTopic(topicId);
    }

    // Note: the "children" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topic/uri/{uri}")
    public Topic getTopicByUri(@PathParam("uri") String uri) {
        return dmx.getTopicByUri(uri);
    }

    // Note: the "children" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topics/type/{uri}")
    public List<Topic> getTopicsByType(@PathParam("uri") String topicTypeUri) {
        return dmx.getTopicsByType(topicTypeUri);
    }

    // Note: the "children" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topic/type/{uri}/{value}")
    public Topic getTopicByValue(@PathParam("uri") String typeUri, @PathParam("value") SimpleValue value) {
        return dmx.getTopicByValue(typeUri, value);
    }

    // Note: the "children" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topics/type/{uri}/{value}")
    public List<Topic> getTopicsByValue(@PathParam("uri") String typeUri, @PathParam("value") SimpleValue value) {
        return dmx.getTopicsByValue(typeUri, value);
    }

    // Note: the "children" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topics/type/{uri}/query/{value}")
    public List<Topic> queryTopics(@PathParam("uri") String typeUri, @PathParam("value") SimpleValue value) {
        return dmx.queryTopics(typeUri, value);
    }

    // Note: the "children" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topics/query/{query}")
    public QueryResult queryTopicsFulltext(@PathParam("query") String query,
                                           @QueryParam("topic_type_uri") String topicTypeUri,
                                           @QueryParam("search_child_topics") boolean searchChildTopics) {
        return dmx.queryTopicsFulltext(query, topicTypeUri, searchChildTopics);
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

    // Note: the "children" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/assoc/{id}")
    public Assoc getAssoc(@PathParam("id") long assocId) {
        return dmx.getAssoc(assocId);
    }

    // Note: the "children" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/assoc/type/{uri}/{value}")
    public Assoc getAssocByValue(@PathParam("uri") String typeUri, @PathParam("value") SimpleValue value) {
        return dmx.getAssocByValue(typeUri, value);
    }

    // Note: the "children" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/assocs/type/{uri}/query/{value}")
    public List<Assoc> queryAssocs(@PathParam("uri") String typeUri, @PathParam("value") SimpleValue value) {
        return dmx.queryAssocs(typeUri, value);
    }

    // Note: the "children" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/assoc/{assoc_type_uri}/{topic1_id}/{topic2_id}/{role_type1_uri}/{role_type2_uri}")
    public Assoc getAssoc(@PathParam("assoc_type_uri") String assocTypeUri,
                   @PathParam("topic1_id") long topic1Id, @PathParam("topic2_id") long topic2Id,
                   @PathParam("role_type1_uri") String roleTypeUri1, @PathParam("role_type2_uri") String roleTypeUri2) {
        return dmx.getAssoc(assocTypeUri, topic1Id, topic2Id, roleTypeUri1, roleTypeUri2);
    }

    // ---

    // Note: the "children" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/association/multiple/{topic1_id}/{topic2_id}")
    public List<Assoc> getAssocs(@PathParam("topic1_id") long topic1Id, @PathParam("topic2_id") long topic2Id) {
        return dmx.getAssocs(topic1Id, topic2Id);
    }

    // Note: the "children" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/association/multiple/{topic1_id}/{topic2_id}/{assoc_type_uri}")
    public List<Assoc> getAssocs(@PathParam("topic1_id") long topic1Id, @PathParam("topic2_id") long topic2Id,
                                 @PathParam("assoc_type_uri") String assocTypeUri) {
        return dmx.getAssocs(topic1Id, topic2Id, assocTypeUri);
    }

    // ---

    @POST
    @Path("/association")
    @Transactional
    public DirectivesResponse createAssoc(AssocModel model) {
        return new DirectivesResponse(dmx.createAssoc(model));
    }

    @PUT
    @Path("/association/{id}")
    @Transactional
    public DirectivesResponse updateAssoc(@PathParam("id") long assocId, AssocModel model) {
        if (model.getId() != -1 && assocId != model.getId()) {
            throw new RuntimeException("ID mismatch in update request");
        }
        model.setId(assocId);
        dmx.updateAssoc(model);
        return new DirectivesResponse();
    }

    @DELETE
    @Path("/association/{id}")
    @Transactional
    public DirectivesResponse deleteAssoc(@PathParam("id") long assocId) {
        dmx.deleteAssoc(assocId);
        return new DirectivesResponse();
    }



    // === Topic Types ===

    // ### TODO: change URI templates to "/topic_type" / "/assoc_type"

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



    // === Assoc Types ===

    @GET
    @Path("/assoctype/{uri}")
    public AssocType getAssocType(@PathParam("uri") String uri) {
        return dmx.getAssocType(uri);
    }

    @GET
    @Path("/assoctype/assoc/{id}")
    public AssocType getAssocTypeImplicitly(@PathParam("id") long assocId) {
        return dmx.getAssocTypeImplicitly(assocId);
    }

    @GET
    @Path("/assoctype/all")
    public List<AssocType> getAllAssocTypes() {
        return dmx.getAllAssocTypes();
    }

    @POST
    @Path("/assoctype")
    @Transactional
    public AssocType createAssocType(AssocTypeModel model) {
        AssocType assocType = dmx.createAssocType(model);
        me.newAssocType(assocType);
        return assocType;
    }

    @PUT
    @Path("/assoctype")
    @Transactional
    public DirectivesResponse updateAssocType(AssocTypeModel model) {
        dmx.updateAssocType(model);
        return new DirectivesResponse();
    }

    @DELETE
    @Path("/assoctype/{uri}")
    @Transactional
    public DirectivesResponse deleteAssocType(@PathParam("uri") String uri) {
        dmx.deleteAssocType(uri);
        return new DirectivesResponse();
    }



    // === Role Types ===

    @POST
    @Path("/roletype")
    @Transactional
    public Topic createRoleType(TopicModel model) {
        Topic roleType = dmx.createRoleType(model);
        me.newRoleType(roleType);
        return roleType;
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



    // Note: the "children" query parameter is handled by the core's JerseyResponseFilter
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

    // Note: the "children" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topic/{id}/related_assocs")
    public List<RelatedAssoc> getTopicRelatedAssocs(@PathParam("id")                     long topicId,
                                                    @QueryParam("assoc_type_uri")        String assocTypeUri,
                                                    @QueryParam("my_role_type_uri")      String myRoleTypeUri,
                                                    @QueryParam("others_role_type_uri")  String othersRoleTypeUri,
                                                    @QueryParam("others_assoc_type_uri") String othersAssocTypeUri) {
        Topic topic = dmx.getTopic(topicId);
        return getRelatedAssocs(topic, "topic", assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersAssocTypeUri);
    }



    // ****************************
    // *** Association REST API ***
    // ****************************



    // Note: the "children" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/association/{id}/related_topics")
    public List<RelatedTopic> getAssocRelatedTopics(@PathParam("id")                     long assocId,
                                                    @QueryParam("assoc_type_uri")        String assocTypeUri,
                                                    @QueryParam("my_role_type_uri")      String myRoleTypeUri,
                                                    @QueryParam("others_role_type_uri")  String othersRoleTypeUri,
                                                    @QueryParam("others_topic_type_uri") String othersTopicTypeUri) {
        Assoc assoc = dmx.getAssoc(assocId);
        return getRelatedTopics(assoc, "association", assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri);
    }

    // Note: the "children" query parameter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/association/{id}/related_assocs")
    public List<RelatedAssoc> getAssocRelatedAssocs(@PathParam("id")                     long assocId,
                                                    @QueryParam("assoc_type_uri")        String assocTypeUri,
                                                    @QueryParam("my_role_type_uri")      String myRoleTypeUri,
                                                    @QueryParam("others_role_type_uri")  String othersRoleTypeUri,
                                                    @QueryParam("others_assoc_type_uri") String othersAssocTypeUri) {
        Assoc assoc = dmx.getAssoc(assocId);
        return getRelatedAssocs(assoc, "association", assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri);
    }



    // ***********************
    // *** Object REST API ***
    // ***********************



    @GET
    @Path("/object/{id}/related_topics")
    public List<RelatedTopic> getRelatedTopicsWithoutChilds(@PathParam("id") long objectId) {
        DMXObject object = dmx.getObject(objectId);
        List<RelatedTopic> relTopics = object.getRelatedTopics();
        Iterator<RelatedTopic> i = relTopics.iterator();
        int removed = 0;
        while (i.hasNext()) {
            RelatedTopic relTopic = i.next();
            if (isDirectModelledChildTopic(object, relTopic)) {
                i.remove();
                removed++;
            }
        }
        logger.fine("### " + removed + " topics are removed from result set of object " + objectId);
        return relTopics;
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
            dmx.deleteAssoc(id);
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
                                                String myRoleTypeUri, String othersRoleTypeUri,
                                                String othersTopicTypeUri) {
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

    private List<RelatedAssoc> getRelatedAssocs(DMXObject object, String objectInfo, String assocTypeUri,
                                                String myRoleTypeUri, String othersRoleTypeUri,
                                                String othersAssocTypeUri) {
        String operation = "Fetching related associations of " + objectInfo + " " + object.getId();
        String paramInfo = "(assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri +
            "\", othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersAssocTypeUri=\"" + othersAssocTypeUri + "\")";
        try {
            logger.fine(operation + " " + paramInfo);
            return object.getRelatedAssocs(assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersAssocTypeUri);
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed " + paramInfo, e);
        }
    }

    // ---

    private boolean isDirectModelledChildTopic(DMXObject parentObject, RelatedTopic childTopic) {
        // check comp def and role types
        return hasCompDef(parentObject, childTopic) && childTopic.getRelatingAssoc().matches(
            PARENT, parentObject.getId(),
            CHILD, childTopic.getId()
        );
    }

    private boolean hasCompDef(DMXObject parentObject, RelatedTopic childTopic) {
        // Note: the user might have no explicit READ permission for the type.
        // DMXObject's getType() has *implicit* READ permission.
        DMXType parentType = parentObject.getType();
        String childTypeUri = childTopic.getTypeUri();
        String assocTypeUri = childTopic.getRelatingAssoc().getTypeUri();
        String compDefUri = childTypeUri + "#" + assocTypeUri;
        if (parentType.hasCompDef(compDefUri)) {
            return true;
        } else if (parentType.hasCompDef(childTypeUri)) {
            return parentType.getCompDef(childTypeUri).getInstanceLevelAssocTypeUri().equals(assocTypeUri);
        }
        return false;
    }

    // ---

    // TODO: move this logic to dmx.deleteTopic() so that it can delete types as well? (types ARE topics after all)
    private void deleteAnyTopic(long id) {
        Topic t = dmx.getTopic(id);
        String typeUri = t.getTypeUri();
        if (typeUri.equals(TOPIC_TYPE)) {
            dmx.deleteTopicType(t.getUri());
        } else if (typeUri.equals(ASSOC_TYPE)) {
            dmx.deleteAssocType(t.getUri());
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
            newType(topicType, "topicType", "newTopicType");
        }

        private void newAssocType(AssocType assocType) {
            newType(assocType, "assocType", "newAssocType");
        }

        private void newRoleType(Topic roleType) {
            newType(roleType, "roleType", "newRoleType");
        }

        // ---

        private void newType(Topic type, String argName, String messageType) {
            try {
                messageToAllButOne(new JSONObject()
                    .put("type", messageType)
                    .put("args", new JSONObject()
                        .put(argName, type.toJSON())
                    )
                );
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error while sending a \"" + messageType + "\" message:", e);
            }
        }

        private void messageToAllButOne(JSONObject message) {
            dmx.getWebSocketsService().messageToAllButOne(request, pluginUri, message.toString());
        }
    }
}
