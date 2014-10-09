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
        return dms.getTopic(topicId);
    }

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topic/by_value/{key}/{value}")
    public Topic getTopic(@PathParam("key") String key, @PathParam("value") SimpleValue value) {
        return dms.getTopic(key, value);
    }

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topic/multi/by_value/{key}/{value}")
    public List<Topic> getTopics(@PathParam("key") String key, @PathParam("value") SimpleValue value) {
        return dms.getTopics(key, value);
    }

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topic/by_type/{type_uri}")
    public ResultList<RelatedTopic> getTopics(@PathParam("type_uri") String typeUri,
                                              @QueryParam("max_result_size") int maxResultSize) {
        return dms.getTopics(typeUri, maxResultSize);
    }

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topic")
    public List<Topic> searchTopics(@QueryParam("search") String searchTerm, @QueryParam("field")  String fieldUri) {
        return dms.searchTopics(searchTerm, fieldUri);
    }

    @POST
    @Path("/topic")
    @Transactional
    public Topic createTopic(TopicModel model) {
        return dms.createTopic(model);
    }

    @PUT
    @Path("/topic/{id}")
    @Transactional
    public Directives updateTopic(@PathParam("id") long topicId, TopicModel model) {
        if (model.getId() != -1 && topicId != model.getId()) {
            throw new RuntimeException("ID mismatch in update request");
        }
        model.setId(topicId);
        dms.updateTopic(model);
        return Directives.get();
    }

    @DELETE
    @Path("/topic/{id}")
    @Transactional
    public Directives deleteTopic(@PathParam("id") long topicId) {
        dms.deleteTopic(topicId);
        return Directives.get();
    }



    // === Associations ===

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/association/{id}")
    public Association getAssociation(@PathParam("id") long assocId) {
        return dms.getAssociation(assocId);
    }

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/association/{assoc_type_uri}/{topic1_id}/{topic2_id}/{role_type1_uri}/{role_type2_uri}")
    public Association getAssociation(@PathParam("assoc_type_uri") String assocTypeUri,
                   @PathParam("topic1_id") long topic1Id, @PathParam("topic2_id") long topic2Id,
                   @PathParam("role_type1_uri") String roleTypeUri1, @PathParam("role_type2_uri") String roleTypeUri2) {
        return dms.getAssociation(assocTypeUri, topic1Id, topic2Id, roleTypeUri1, roleTypeUri2);
    }

    // ---

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/association/multiple/{topic1_id}/{topic2_id}")
    public List<Association> getAssociations(@PathParam("topic1_id") long topic1Id,
                                             @PathParam("topic2_id") long topic2Id) {
        return dms.getAssociations(topic1Id, topic2Id);
    }

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/association/multiple/{topic1_id}/{topic2_id}/{assoc_type_uri}")
    public List<Association> getAssociations(@PathParam("topic1_id") long topic1Id,
                                             @PathParam("topic2_id") long topic2Id,
                                             @PathParam("assoc_type_uri") String assocTypeUri) {
        return dms.getAssociations(topic1Id, topic2Id, assocTypeUri);
    }

    // ---

    @POST
    @Path("/association")
    @Transactional
    public Association createAssociation(AssociationModel model) {
        return dms.createAssociation(model);
    }

    @PUT
    @Path("/association/{id}")
    @Transactional
    public Directives updateAssociation(@PathParam("id") long assocId, AssociationModel model) {
        if (model.getId() != -1 && assocId != model.getId()) {
            throw new RuntimeException("ID mismatch in update request");
        }
        model.setId(assocId);
        dms.updateAssociation(model);
        return Directives.get();
    }

    @DELETE
    @Path("/association/{id}")
    @Transactional
    public Directives deleteAssociation(@PathParam("id") long assocId) {
        dms.deleteAssociation(assocId);
        return Directives.get();
    }



    // === Topic Types ===

    @GET
    @Path("/topictype")
    public List<String> getTopicTypeUris() {
        return dms.getTopicTypeUris();
    }

    @GET
    @Path("/topictype/{uri}")
    public TopicType getTopicType(@PathParam("uri") String uri) {
        return dms.getTopicType(uri);
    }

    @GET
    @Path("/topictype/all")
    public List<TopicType> getAllTopicTypes() {
        return dms.getAllTopicTypes();
    }

    @POST
    @Path("/topictype")
    @Transactional
    public TopicType createTopicType(TopicTypeModel topicTypeModel) {
        return dms.createTopicType(topicTypeModel);
    }

    @PUT
    @Path("/topictype")
    @Transactional
    public Directives updateTopicType(TopicTypeModel model) {
        dms.updateTopicType(model);
        return Directives.get();
    }

    @DELETE
    @Path("/topictype/{uri}")
    @Transactional
    public Directives deleteTopicType(@PathParam("uri") String uri) {
        dms.deleteTopicType(uri);
        return Directives.get();
    }



    // === Association Types ===

    @GET
    @Path("/assoctype")
    public List<String> getAssociationTypeUris() {
        return dms.getAssociationTypeUris();
    }

    @GET
    @Path("/assoctype/{uri}")
    public AssociationType getAssociationType(@PathParam("uri") String uri) {
        return dms.getAssociationType(uri);
    }

    @GET
    @Path("/assoctype/all")
    public List<AssociationType> getAssociationAllTypes() {
        return dms.getAllAssociationTypes();
    }

    @POST
    @Path("/assoctype")
    @Transactional
    public AssociationType createAssociationType(AssociationTypeModel assocTypeModel) {
        return dms.createAssociationType(assocTypeModel);
    }

    @PUT
    @Path("/assoctype")
    @Transactional
    public Directives updateAssociationType(AssociationTypeModel model) {
        dms.updateAssociationType(model);
        return Directives.get();
    }

    @DELETE
    @Path("/assoctype/{uri}")
    @Transactional
    public Directives deleteAssociationType(@PathParam("uri") String uri) {
        dms.deleteAssociationType(uri);
        return Directives.get();
    }



    // === Plugins ===

    @GET
    @Path("/plugin")
    public List<PluginInfo> getPluginInfo() {
        return dms.getPluginInfo();
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
                                                         @QueryParam("others_topic_type_uri") String othersTopicTypeUri,
                                                         @QueryParam("max_result_size")       int maxResultSize) {
        Topic topic = dms.getTopic(topicId);
        return getRelatedTopics(topic, "topic", assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri, maxResultSize);
    }

    // Note: the "include_childs" query paramter is handled by the core's JerseyResponseFilter
    @GET
    @Path("/topic/{id}/related_assocs")
    public ResultList<RelatedAssociation> getTopicRelatedAssociations(@PathParam("id")      long topicId,
                                                       @QueryParam("assoc_type_uri")        String assocTypeUri,
                                                       @QueryParam("my_role_type_uri")      String myRoleTypeUri,
                                                       @QueryParam("others_role_type_uri")  String othersRoleTypeUri,
                                                       @QueryParam("others_assoc_type_uri") String othersAssocTypeUri) {
        Topic topic = dms.getTopic(topicId);
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
                                                         @QueryParam("others_topic_type_uri") String othersTopicTypeUri,
                                                         @QueryParam("max_result_size")       int maxResultSize) {
        Association assoc = dms.getAssociation(assocId);
        return getRelatedTopics(assoc, "association", assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri, maxResultSize);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private ResultList<RelatedTopic> getRelatedTopics(DeepaMehtaObject object, String objectInfo, String assocTypeUri,
                                                      String myRoleTypeUri, String othersRoleTypeUri,
                                                      String othersTopicTypeUri, int maxResultSize) {
        String operation = "Fetching related topics of " + objectInfo + " " + object.getId();
        String paramInfo = "(assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri +
            "\", othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersTopicTypeUri=\"" + othersTopicTypeUri +
            "\", maxResultSize=" + maxResultSize + ")";
        try {
            logger.info(operation + " " + paramInfo);
            return object.getRelatedTopics(assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri,
                maxResultSize);
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
