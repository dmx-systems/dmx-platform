package de.deepamehta.plugins.webservice;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.DirectivesResponse;
import de.deepamehta.core.service.PluginInfo;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.service.Transactional;

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

import java.util.List;
import java.util.logging.Logger;



@Path("/core")
@Consumes("application/json")
@Produces("application/json")
public class WebservicePlugin extends PluginActivator {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Topics ===

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topic/{id}")
    public Topic getTopic(@PathParam("id") long topicId) {
        return dm4.getTopic(topicId);
    }

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topic/by_value/{key}/{value}")
    public Topic getTopic(@PathParam("key") String key, @PathParam("value") SimpleValue value) {
        return dm4.getTopic(key, value);
    }

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topic/multi/by_value/{key}/{value}")
    public List<Topic> getTopics(@PathParam("key") String key, @PathParam("value") SimpleValue value) {
        return dm4.getTopics(key, value);
    }

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topic/by_type/{topic_type_uri}")
    public List<Topic> getTopics(@PathParam("topic_type_uri") String topicTypeUri) {
        return dm4.getTopics(topicTypeUri);
    }

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topic")
    public List<Topic> searchTopics(@QueryParam("search") String searchTerm, @QueryParam("field") String fieldUri) {
        return dm4.searchTopics(searchTerm, fieldUri);
    }

    @POST
    @Path("/topic")
    @Transactional
    public DirectivesResponse createTopic(TopicModel model) {
        return new DirectivesResponse(dm4.createTopic(model));
    }

    @PUT
    @Path("/topic/{id}")
    @Transactional
    public DirectivesResponse updateTopic(@PathParam("id") long topicId, TopicModel model) {
        if (model.getId() != -1 && topicId != model.getId()) {
            throw new RuntimeException("ID mismatch in update request");
        }
        model.setId(topicId);
        dm4.updateTopic(model);
        return new DirectivesResponse();
    }

    @DELETE
    @Path("/topic/{id}")
    @Transactional
    public DirectivesResponse deleteTopic(@PathParam("id") long topicId) {
        dm4.deleteTopic(topicId);
        return new DirectivesResponse();
    }



    // === Associations ===

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/association/{id}")
    public Association getAssociation(@PathParam("id") long assocId) {
        return dm4.getAssociation(assocId);
    }

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/assoc/by_value/{key}/{value}")
    public Association getAssociation(@PathParam("key") String key, @PathParam("value") SimpleValue value) {
        return dm4.getAssociation(key, value);
    }

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/assoc/multi/by_value/{key}/{value}")
    public List<Association> getAssociations(@PathParam("key") String key, @PathParam("value") SimpleValue value) {
        return dm4.getAssociations(key, value);
    }

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/association/{assoc_type_uri}/{topic1_id}/{topic2_id}/{role_type1_uri}/{role_type2_uri}")
    public Association getAssociation(@PathParam("assoc_type_uri") String assocTypeUri,
                   @PathParam("topic1_id") long topic1Id, @PathParam("topic2_id") long topic2Id,
                   @PathParam("role_type1_uri") String roleTypeUri1, @PathParam("role_type2_uri") String roleTypeUri2) {
        return dm4.getAssociation(assocTypeUri, topic1Id, topic2Id, roleTypeUri1, roleTypeUri2);
    }

    // ---

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/association/multiple/{topic1_id}/{topic2_id}")
    public List<Association> getAssociations(@PathParam("topic1_id") long topic1Id,
                                             @PathParam("topic2_id") long topic2Id) {
        return dm4.getAssociations(topic1Id, topic2Id);
    }

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/association/multiple/{topic1_id}/{topic2_id}/{assoc_type_uri}")
    public List<Association> getAssociations(@PathParam("topic1_id") long topic1Id,
                                             @PathParam("topic2_id") long topic2Id,
                                             @PathParam("assoc_type_uri") String assocTypeUri) {
        return dm4.getAssociations(topic1Id, topic2Id, assocTypeUri);
    }

    // ---

    @POST
    @Path("/association")
    @Transactional
    public DirectivesResponse createAssociation(AssociationModel model) {
        return new DirectivesResponse(dm4.createAssociation(model));
    }

    @PUT
    @Path("/association/{id}")
    @Transactional
    public DirectivesResponse updateAssociation(@PathParam("id") long assocId, AssociationModel model) {
        if (model.getId() != -1 && assocId != model.getId()) {
            throw new RuntimeException("ID mismatch in update request");
        }
        model.setId(assocId);
        dm4.updateAssociation(model);
        return new DirectivesResponse();
    }

    @DELETE
    @Path("/association/{id}")
    @Transactional
    public DirectivesResponse deleteAssociation(@PathParam("id") long assocId) {
        dm4.deleteAssociation(assocId);
        return new DirectivesResponse();
    }



    // === Topic Types ===

    @GET
    @Path("/topictype")
    public List<String> getTopicTypeUris() {
        return dm4.getTopicTypeUris();
    }

    @GET
    @Path("/topictype/{uri}")
    public TopicType getTopicType(@PathParam("uri") String uri) {
        return dm4.getTopicType(uri);
    }

    @GET
    @Path("/topictype/all")
    public List<TopicType> getAllTopicTypes() {
        return dm4.getAllTopicTypes();
    }

    @POST
    @Path("/topictype")
    @Transactional
    public TopicType createTopicType(TopicTypeModel model) {
        return dm4.createTopicType(model);
    }

    @PUT
    @Path("/topictype")
    @Transactional
    public DirectivesResponse updateTopicType(TopicTypeModel model) {
        dm4.updateTopicType(model);
        return new DirectivesResponse();
    }

    @DELETE
    @Path("/topictype/{uri}")
    @Transactional
    public DirectivesResponse deleteTopicType(@PathParam("uri") String uri) {
        dm4.deleteTopicType(uri);
        return new DirectivesResponse();
    }



    // === Association Types ===

    @GET
    @Path("/assoctype")
    public List<String> getAssociationTypeUris() {
        return dm4.getAssociationTypeUris();
    }

    @GET
    @Path("/assoctype/{uri}")
    public AssociationType getAssociationType(@PathParam("uri") String uri) {
        return dm4.getAssociationType(uri);
    }

    @GET
    @Path("/assoctype/all")
    public List<AssociationType> getAssociationAllTypes() {
        return dm4.getAllAssociationTypes();
    }

    @POST
    @Path("/assoctype")
    @Transactional
    public AssociationType createAssociationType(AssociationTypeModel model) {
        return dm4.createAssociationType(model);
    }

    @PUT
    @Path("/assoctype")
    @Transactional
    public DirectivesResponse updateAssociationType(AssociationTypeModel model) {
        dm4.updateAssociationType(model);
        return new DirectivesResponse();
    }

    @DELETE
    @Path("/assoctype/{uri}")
    @Transactional
    public DirectivesResponse deleteAssociationType(@PathParam("uri") String uri) {
        dm4.deleteAssociationType(uri);
        return new DirectivesResponse();
    }



    // === Role Types ===

    @POST
    @Path("/roletype")
    @Transactional
    public Topic createRoleType(TopicModel model) {
        return dm4.createRoleType(model);
    }



    // === Plugins ===

    @GET
    @Path("/plugin")
    public List<PluginInfo> getPluginInfo() {
        return dm4.getPluginInfo();
    }



    // **********************
    // *** Topic REST API ***
    // **********************



    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topic/{id}/related_topics")
    public ResultList<RelatedTopic> getTopicRelatedTopics(@PathParam("id")                    long topicId,
                                                       @QueryParam("assoc_type_uri")        String assocTypeUri,
                                                       @QueryParam("my_role_type_uri")      String myRoleTypeUri,
                                                       @QueryParam("others_role_type_uri")  String othersRoleTypeUri,
                                                       @QueryParam("others_topic_type_uri") String othersTopicTypeUri) {
        Topic topic = dm4.getTopic(topicId);
        return getRelatedTopics(topic, "topic", assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
    }

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topic/{id}/related_assocs")
    public ResultList<RelatedAssociation> getTopicRelatedAssociations(@PathParam("id")      long topicId,
                                                       @QueryParam("assoc_type_uri")        String assocTypeUri,
                                                       @QueryParam("my_role_type_uri")      String myRoleTypeUri,
                                                       @QueryParam("others_role_type_uri")  String othersRoleTypeUri,
                                                       @QueryParam("others_assoc_type_uri") String othersAssocTypeUri) {
        Topic topic = dm4.getTopic(topicId);
        return getRelatedAssociations(topic, "topic", assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri);
    }



    // ****************************
    // *** Association REST API ***
    // ****************************



    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/association/{id}/related_topics")
    public ResultList<RelatedTopic> getAssociationRelatedTopics(@PathParam("id")              long assocId,
                                                       @QueryParam("assoc_type_uri")        String assocTypeUri,
                                                       @QueryParam("my_role_type_uri")      String myRoleTypeUri,
                                                       @QueryParam("others_role_type_uri")  String othersRoleTypeUri,
                                                       @QueryParam("others_topic_type_uri") String othersTopicTypeUri) {
        Association assoc = dm4.getAssociation(assocId);
        return getRelatedTopics(assoc, "association", assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri);
    }

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/association/{id}/related_assocs")
    public ResultList<RelatedAssociation> getAssociationRelatedAssociations(@PathParam("id") long assocId,
                                                       @QueryParam("assoc_type_uri")        String assocTypeUri,
                                                       @QueryParam("my_role_type_uri")      String myRoleTypeUri,
                                                       @QueryParam("others_role_type_uri")  String othersRoleTypeUri,
                                                       @QueryParam("others_assoc_type_uri") String othersAssocTypeUri) {
        Association assoc = dm4.getAssociation(assocId);
        return getRelatedAssociations(assoc, "association", assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private ResultList<RelatedTopic> getRelatedTopics(DeepaMehtaObject object, String objectInfo, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersTopicTypeUri) {
        String operation = "Fetching related topics of " + objectInfo + " " + object.getId();
        String paramInfo = "(assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri +
            "\", othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersTopicTypeUri=\"" + othersTopicTypeUri + "\")";
        try {
            logger.info(operation + " " + paramInfo);
            return object.getRelatedTopics(assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed " + paramInfo, e);
        }
    }

    private ResultList<RelatedAssociation> getRelatedAssociations(DeepaMehtaObject object, String objectInfo,
                                                                  String assocTypeUri, String myRoleTypeUri,
                                                                  String othersRoleTypeUri, String othersAssocTypeUri) {
        String operation = "Fetching related associations of " + objectInfo + " " + object.getId();
        String paramInfo = "(assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri +
            "\", othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersAssocTypeUri=\"" + othersAssocTypeUri + "\")";
        try {
            logger.info(operation + " " + paramInfo);
            return object.getRelatedAssociations(assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersAssocTypeUri);
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed " + paramInfo, e);
        }
    }
}
