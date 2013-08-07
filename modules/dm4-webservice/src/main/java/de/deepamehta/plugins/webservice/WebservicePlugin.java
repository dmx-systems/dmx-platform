package de.deepamehta.plugins.webservice;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.PluginInfo;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import java.util.Set;
import java.util.logging.Logger;



@Path("/core")
@Consumes("application/json")
@Produces("application/json")
public class WebservicePlugin extends PluginActivator {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Topics ===

    @GET
    @Path("/topic/{id}")
    public Topic getTopic(@PathParam("id") long topicId,
                          @QueryParam("fetch_composite") @DefaultValue("true") boolean fetchComposite) {
        return dms.getTopic(topicId, fetchComposite);
    }

    @GET
    @Path("/topic/by_value/{key}/{value}")
    public Topic getTopic(@PathParam("key") String key, @PathParam("value") SimpleValue value,
                          @QueryParam("fetch_composite") @DefaultValue("true") boolean fetchComposite) {
        return dms.getTopic(key, value, fetchComposite);
    }

    @GET
    @Path("/topic/by_type/{type_uri}")
    public ResultSet<RelatedTopic> getTopics(@PathParam("type_uri") String typeUri,
                                      @QueryParam("fetch_composite") @DefaultValue("false") boolean fetchComposite,
                                      @QueryParam("max_result_size") int maxResultSize) {
        return dms.getTopics(typeUri, fetchComposite, maxResultSize);
    }

    @GET
    @Path("/topic")
    public Set<Topic> searchTopics(@QueryParam("search") String searchTerm, @QueryParam("field")  String fieldUri) {
        return dms.searchTopics(searchTerm, fieldUri);
    }

    @POST
    @Path("/topic")
    public Topic createTopic(TopicModel model, @HeaderParam("Cookie") ClientState clientState) {
        return dms.createTopic(model, clientState);
    }

    @PUT
    @Path("/topic/{id}")
    public Directives updateTopic(@PathParam("id") long topicId, TopicModel model,
                                  @HeaderParam("Cookie") ClientState clientState) {
        if (model.getId() != -1 && topicId != model.getId()) {
            throw new RuntimeException("ID mismatch in update request");
        }
        model.setId(topicId);
        return dms.updateTopic(model, clientState);
    }

    @DELETE
    @Path("/topic/{id}")
    public Directives deleteTopic(@PathParam("id") long topicId) {
        return dms.deleteTopic(topicId);
    }



    // === Associations ===

    @GET
    @Path("/association/{id}")
    public Association getAssociation(@PathParam("id") long assocId,
                                      @QueryParam("fetch_composite") @DefaultValue("true") boolean fetchComposite) {
        return dms.getAssociation(assocId, fetchComposite);
    }

    @GET
    @Path("/association/{assoc_type_uri}/{topic1_id}/{topic2_id}/{role_type1_uri}/{role_type2_uri}")
    public Association getAssociation(@PathParam("assoc_type_uri") String assocTypeUri,
                   @PathParam("topic1_id") long topic1Id, @PathParam("topic2_id") long topic2Id,
                   @PathParam("role_type1_uri") String roleTypeUri1, @PathParam("role_type2_uri") String roleTypeUri2,
                   @QueryParam("fetch_composite") @DefaultValue("true") boolean fetchComposite) {
        return dms.getAssociation(assocTypeUri, topic1Id, topic2Id, roleTypeUri1, roleTypeUri2, fetchComposite);
    }

    // ---

    @GET
    @Path("/association/multiple/{topic1_id}/{topic2_id}")
    public Set<Association> getAssociations(@PathParam("topic1_id") long topic1Id,
                                            @PathParam("topic2_id") long topic2Id) {
        return dms.getAssociations(topic1Id, topic2Id);
    }

    @GET
    @Path("/association/multiple/{topic1_id}/{topic2_id}/{assoc_type_uri}")
    public Set<Association> getAssociations(@PathParam("topic1_id") long topic1Id,
                                            @PathParam("topic2_id") long topic2Id,
                                            @PathParam("assoc_type_uri") String assocTypeUri) {
        return dms.getAssociations(topic1Id, topic2Id, assocTypeUri);
    }

    // ---

    @POST
    @Path("/association")
    public Association createAssociation(AssociationModel model, @HeaderParam("Cookie") ClientState clientState) {
        return dms.createAssociation(model, clientState);
    }

    @PUT
    @Path("/association/{id}")
    public Directives updateAssociation(@PathParam("id") long assocId, AssociationModel model,
                                        @HeaderParam("Cookie") ClientState clientState) {
        if (model.getId() != -1 && assocId != model.getId()) {
            throw new RuntimeException("ID mismatch in update request");
        }
        model.setId(assocId);
        return dms.updateAssociation(model, clientState);
    }

    @DELETE
    @Path("/association/{id}")
    public Directives deleteAssociation(@PathParam("id") long assocId) {
        return dms.deleteAssociation(assocId);
    }



    // === Topic Types ===

    @GET
    @Path("/topictype")
    public Set<String> getTopicTypeUris() {
        return dms.getTopicTypeUris();
    }

    @GET
    @Path("/topictype/{uri}")
    public TopicType getTopicType(@PathParam("uri") String uri) {
        return dms.getTopicType(uri);
    }

    @GET
    @Path("/topictype/all")
    public Set<TopicType> getAllTopicTypes() {
        return dms.getAllTopicTypes();
    }

    @POST
    @Path("/topictype")
    public TopicType createTopicType(TopicTypeModel topicTypeModel, @HeaderParam("Cookie") ClientState clientState) {
        return dms.createTopicType(topicTypeModel, clientState);
    }

    @PUT
    @Path("/topictype")
    public Directives updateTopicType(TopicTypeModel model, @HeaderParam("Cookie") ClientState clientState) {
        return dms.updateTopicType(model, clientState);
    }



    // === Association Types ===

    @GET
    @Path("/assoctype")
    public Set<String> getAssociationTypeUris() {
        return dms.getAssociationTypeUris();
    }

    @GET
    @Path("/assoctype/{uri}")
    public AssociationType getAssociationType(@PathParam("uri") String uri) {
        return dms.getAssociationType(uri);
    }

    @GET
    @Path("/assoctype/all")
    public Set<AssociationType> getAssociationAllTypes() {
        return dms.getAllAssociationTypes();
    }

    @POST
    @Path("/assoctype")
    public AssociationType createAssociationType(AssociationTypeModel assocTypeModel,
                                                 @HeaderParam("Cookie") ClientState clientState) {
        return dms.createAssociationType(assocTypeModel, clientState);
    }

    @PUT
    @Path("/assoctype")
    public Directives updateAssociationType(AssociationTypeModel model,
                                            @HeaderParam("Cookie") ClientState clientState) {
        return dms.updateAssociationType(model, clientState);
    }



    // === Plugins ===

    @GET
    @Path("/plugin")
    public Set<PluginInfo> getPluginInfo() {
        return dms.getPluginInfo();
    }



    // **********************
    // *** Topic REST API ***
    // **********************



    @GET
    @Path("/topic/{id}/related_topics")
    public ResultSet<RelatedTopic> getTopicRelatedTopics(@PathParam("id")                     long topicId,
                                                         @QueryParam("assoc_type_uri")        String assocTypeUri,
                                                         @QueryParam("my_role_type_uri")      String myRoleTypeUri,
                                                         @QueryParam("others_role_type_uri")  String othersRoleTypeUri,
                                                         @QueryParam("others_topic_type_uri") String othersTopicTypeUri,
                                                         @QueryParam("max_result_size")       int maxResultSize) {
        Topic topic = dms.getTopic(topicId, false);
        return getRelatedTopics(topic, "topic", assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri, maxResultSize);
    }



    // ****************************
    // *** Association REST API ***
    // ****************************



    @GET
    @Path("/association/{id}/related_topics")
    public ResultSet<RelatedTopic> getAssociationRelatedTopics(@PathParam("id")               long assocId,
                                                         @QueryParam("assoc_type_uri")        String assocTypeUri,
                                                         @QueryParam("my_role_type_uri")      String myRoleTypeUri,
                                                         @QueryParam("others_role_type_uri")  String othersRoleTypeUri,
                                                         @QueryParam("others_topic_type_uri") String othersTopicTypeUri,
                                                         @QueryParam("max_result_size")       int maxResultSize) {
        Association assoc = dms.getAssociation(assocId, false);
        return getRelatedTopics(assoc, "association", assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri, maxResultSize);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private ResultSet<RelatedTopic> getRelatedTopics(DeepaMehtaObject object, String objectInfo, String assocTypeUri,
                                                     String myRoleTypeUri, String othersRoleTypeUri,
                                                     String othersTopicTypeUri, int maxResultSize) {
        String operation = "Fetching related topics of " + objectInfo + " " + object.getId();
        String paramInfo = "(assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri +
            "\", othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersTopicTypeUri=\"" + othersTopicTypeUri +
            "\", maxResultSize=" + maxResultSize + ")";
        try {
            logger.info(operation + " " + paramInfo);
            return object.getRelatedTopics(assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri,
                false, false, maxResultSize);
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed " + paramInfo, e);
        }
    }
}
