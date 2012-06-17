package de.deepamehta.plugins.webservice;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.CommandParams;
import de.deepamehta.core.service.CommandResult;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Hook;
import de.deepamehta.core.service.Plugin;
import de.deepamehta.core.service.PluginInfo;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import java.util.Set;
import java.util.logging.Logger;



@Path("/")
@Consumes("application/json")
@Produces("application/json")
public class WebservicePlugin extends Plugin {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Topics ===

    @GET
    @Path("/topic/{id}")
    public Topic getTopic(@PathParam("id") long topicId,
                          @QueryParam("fetch_composite") @DefaultValue("true") boolean fetchComposite,
                          @HeaderParam("Cookie") ClientState clientState) {
        try {
            Topic topic = dms.getTopic(topicId, fetchComposite, clientState);
            //
            triggerPreSend(topic, clientState);
            //
            return topic;
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/topic/by_value/{key}/{value}")
    public Topic getTopic(@PathParam("key") String key, @PathParam("value") SimpleValue value,
                          @QueryParam("fetch_composite") @DefaultValue("true") boolean fetchComposite,
                          @HeaderParam("Cookie") ClientState clientState) {
        try {
            return dms.getTopic(key, value, fetchComposite, clientState);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/topic/by_type/{type_uri}")
    public ResultSet<Topic> getTopics(@PathParam("type_uri") String typeUri,
                                      @QueryParam("fetch_composite") @DefaultValue("false") boolean fetchComposite,
                                      @QueryParam("max_result_size") int maxResultSize,
                                      @HeaderParam("Cookie") ClientState clientState) {
        try {
            return dms.getTopics(typeUri, fetchComposite, maxResultSize, clientState);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/topic")
    public Set<Topic> searchTopics(@QueryParam("search")    String searchTerm,
                                   @QueryParam("field")     String fieldUri,
                                   @QueryParam("wholeword") boolean wholeWord,
                                   @HeaderParam("Cookie")   ClientState clientState) {
        try {
            return dms.searchTopics(searchTerm, fieldUri, wholeWord, clientState);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @POST
    @Path("/topic")
    public Topic createTopic(TopicModel model, @HeaderParam("Cookie") ClientState clientState) {
        try {
            Topic topic = dms.createTopic(model, clientState);
            //
            triggerPreSend(topic, clientState);
            //
            return topic;
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @PUT
    @Path("/topic")
    public Directives updateTopic(TopicModel model, @HeaderParam("Cookie") ClientState clientState) {
        try {
            Directives directives = dms.updateTopic(model, clientState);
            //
            triggerPreSend(directives, clientState);
            //
            return directives;
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @DELETE
    @Path("/topic/{id}")
    public Directives deleteTopic(@PathParam("id") long topicId, @HeaderParam("Cookie") ClientState clientState) {
        try {
            return dms.deleteTopic(topicId, clientState);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }



    // === Associations ===

    @GET
    @Path("/association/{id}")
    public Association getAssociation(@PathParam("id") long assocId,
                                      @QueryParam("fetch_composite") @DefaultValue("true") boolean fetchComposite,
                                      @HeaderParam("Cookie") ClientState clientState) {
        try {
            return dms.getAssociation(assocId, fetchComposite, clientState);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/association/{assoc_type_uri}/{topic1_id}/{topic2_id}/{role_type1_uri}/{role_type2_uri}")
    public Association getAssociation(@PathParam("assoc_type_uri") String assocTypeUri,
                   @PathParam("topic1_id") long topic1Id, @PathParam("topic2_id") long topic2Id,
                   @PathParam("role_type1_uri") String roleTypeUri1, @PathParam("role_type2_uri") String roleTypeUri2,
                   @QueryParam("fetch_composite") @DefaultValue("true") boolean fetchComposite,
                   @HeaderParam("Cookie") ClientState clientState) {
        try {
            return dms.getAssociation(assocTypeUri, topic1Id, topic2Id, roleTypeUri1, roleTypeUri2, fetchComposite,
                clientState);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    // ---

    @GET
    @Path("/association/multiple/{topic1_id}/{topic2_id}")
    public Set<Association> getAssociations(@PathParam("topic1_id") long topic1Id,
                                            @PathParam("topic2_id") long topic2Id) {
        try {
            return dms.getAssociations(topic1Id, topic2Id);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/association/multiple/{topic1_id}/{topic2_id}/{assoc_type_uri}")
    public Set<Association> getAssociations(@PathParam("topic1_id") long topic1Id,
                                            @PathParam("topic2_id") long topic2Id,
                                            @PathParam("assoc_type_uri") String assocTypeUri) {
        try {
            return dms.getAssociations(topic1Id, topic2Id, assocTypeUri);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    // ---

    @POST
    @Path("/association")
    public Association createAssociation(AssociationModel model, @HeaderParam("Cookie") ClientState clientState) {
        try {
            return dms.createAssociation(model, clientState);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @PUT
    @Path("/association")
    public Directives updateAssociation(AssociationModel model, @HeaderParam("Cookie") ClientState clientState) {
        try {
            return dms.updateAssociation(model, clientState);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @DELETE
    @Path("/association/{id}")
    public Directives deleteAssociation(@PathParam("id") long assocId, @HeaderParam("Cookie") ClientState clientState) {
        try {
            return dms.deleteAssociation(assocId, clientState);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }



    // === Topic Types ===

    @GET
    @Path("/topictype")
    public Set<String> getTopicTypeUris() {
        try {
            return dms.getTopicTypeUris();
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/topictype/{uri}")
    public TopicType getTopicType(@PathParam("uri") String uri, @HeaderParam("Cookie") ClientState clientState) {
        try {
            TopicType topicType = dms.getTopicType(uri, clientState);
            //
            triggerPreSend(topicType, clientState);
            //
            return topicType;
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @POST
    @Path("/topictype")
    public TopicType createTopicType(TopicTypeModel topicTypeModel, @HeaderParam("Cookie") ClientState clientState) {
        try {
            return dms.createTopicType(topicTypeModel, clientState);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @PUT
    @Path("/topictype")
    public Directives updateTopicType(TopicTypeModel model, @HeaderParam("Cookie") ClientState clientState) {
        try {
            return dms.updateTopicType(model, clientState);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }



    // === Association Types ===

    @GET
    @Path("/assoctype")
    public Set<String> getAssociationTypeUris() {
        try {
            return dms.getAssociationTypeUris();
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/assoctype/{uri}")
    public AssociationType getAssociationType(@PathParam("uri") String uri,
                                              @HeaderParam("Cookie") ClientState clientState) {
        try {
            return dms.getAssociationType(uri, clientState);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @POST
    @Path("/assoctype")
    public AssociationType createAssociationType(AssociationTypeModel assocTypeModel,
                                                 @HeaderParam("Cookie") ClientState clientState) {
        try {
            return dms.createAssociationType(assocTypeModel, clientState);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }



    // === Commands ===

    @POST
    @Path("/command/{command}")
    @Consumes("application/json, multipart/form-data")
    public CommandResult executeCommand(@PathParam("command") String command, CommandParams params,
                                        @HeaderParam("Cookie") ClientState clientState) {
        try {
            return dms.executeCommand(command, params, clientState);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }



    // === Plugins ===

    @GET
    @Path("/plugin")
    public Set<PluginInfo> getPluginInfo() {
        try {
            return dms.getPluginInfo();
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }



    // **********************
    // *** Topic REST API ***
    // **********************



    @GET
    @Path("/topic/{id}/related_topics")
    public ResultSet<RelatedTopic> getRelatedTopics(@PathParam("id")                     long topicId,
                                                    @QueryParam("assoc_type_uri")        String assocTypeUri,
                                                    @QueryParam("my_role_type_uri")      String myRoleTypeUri,
                                                    @QueryParam("others_role_type_uri")  String othersRoleTypeUri,
                                                    @QueryParam("others_topic_type_uri") String othersTopicTypeUri,
                                                    @QueryParam("max_result_size")       int maxResultSize,
                                                    @HeaderParam("Cookie")               ClientState clientState) {
        logger.info("topicId=" + topicId + ", assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri +
            "\", othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersTopicTypeUri=\"" + othersTopicTypeUri +
            "\", maxResultSize=" + maxResultSize);
        try {
            return dms.getTopic(topicId, false, clientState).getRelatedTopics(assocTypeUri, myRoleTypeUri,
                othersRoleTypeUri, othersTopicTypeUri, false, false, maxResultSize, clientState);
        } catch (Exception e) {
            throw new WebApplicationException(new RuntimeException("Retrieving related topics of topic " + topicId +
                " failed (assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri +
                "\", othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersTopicTypeUri=\"" + othersTopicTypeUri +
                "\", maxResultSize=" + maxResultSize + ")", e));
        }
    }



    // ****************************
    // *** Association REST API ***
    // ****************************

    // ### TODO



    // ------------------------------------------------------------------------------------------------- Private Methods

    private void triggerPreSend(Topic topic, ClientState clientState) {
        dms.triggerHook(Hook.PRE_SEND_TOPIC, topic, clientState);
    }

    private void triggerPreSend(TopicType topicType, ClientState clientState) {
        dms.triggerHook(Hook.PRE_SEND_TOPIC_TYPE, topicType, clientState);
    }

    private void triggerPreSend(Directives directives, ClientState clientState) {
        for (Directives.Entry entry : directives) {
            switch (entry.dir) {
            case UPDATE_TOPIC:
                triggerPreSend((Topic) entry.arg, clientState);
                break;
            case UPDATE_TOPIC_TYPE:
                triggerPreSend((TopicType) entry.arg, clientState);
                break;
            }
        }
    }
}
