package de.deepamehta.core.storage.spi;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;



public interface DeepaMehtaStorage {



    // === Topics ===

    TopicModel fetchTopic(long id);

    TopicModel fetchTopic(String key, Object value);

    // ### TODO: unify all List and Set results as Collection
    List<TopicModel> fetchTopics(String key, Object value);

    List<TopicModel> queryTopics(Object value);

    List<TopicModel> queryTopics(String key, Object value);

    Iterator<TopicModel> fetchAllTopics();

    // ---

    void storeTopic(TopicModel topicModel);

    void storeTopicUri(long topicId, String uri);

    void storeTopicTypeUri(long topicId, String topicTypeUri);

    /**
     * @param   indexValue  Optional: the value to be indexed. If indexValue is not specified, value is used.
     */
    void storeTopicValue(long topicId, SimpleValue value, Collection<IndexMode> indexModes, String indexKey,
                                                                                            SimpleValue indexValue);
    // ---

    void deleteTopic(long topicId);



    // === Associations ===

    AssociationModel fetchAssociation(long id);

    Set<AssociationModel> fetchAssociations(String assocTypeUri, long topicId1, long topicId2, String roleTypeUri1,
                                                                                               String roleTypeUri2);

    Set<AssociationModel> fetchAssociationsBetweenTopicAndAssociation(String assocTypeUri, long topicId, long assocId,
                                                                      String topicRoleTypeUri, String assocRoleTypeUri);

    Iterator<AssociationModel> fetchAllAssociations();

    // ---

    void storeAssociation(AssociationModel assocModel);

    void storeAssociationUri(long assocId, String uri);

    void storeAssociationTypeUri(long assocId, String assocTypeUri);

    /**
     * @param   indexValue  Optional: the value to be indexed. If indexValue is not specified, value is used.
     */
    void storeAssociationValue(long assocId, SimpleValue value, Collection<IndexMode> indexModes, String indexKey,
                                                                                                SimpleValue indexValue);

    void storeRoleTypeUri(long assocId, long playerId, String roleTypeUri);

    // ---

    void deleteAssociation(long assocId);



    // === Traversal ===

    Set<AssociationModel> fetchTopicAssociations(long topicId);

    Set<AssociationModel> fetchAssociationAssociations(long assocId);

    // ---

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    Set<RelatedTopicModel> fetchTopicRelatedTopics(long topicId, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersTopicTypeUri);

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    Set<RelatedAssociationModel> fetchTopicRelatedAssociations(long topicId, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri);

    // ---

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    Set<RelatedTopicModel> fetchAssociationRelatedTopics(long assocId, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersTopicTypeUri);

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    Set<RelatedAssociationModel> fetchAssociationRelatedAssociations(long assocId, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri);

    // ---

    /**
     * @param   id                  id of a topic or an association
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    Set<RelatedTopicModel> fetchRelatedTopics(long id, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersTopicTypeUri);

    /**
     * @param   id                  id of a topic or an association
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    Set<RelatedAssociationModel> fetchRelatedAssociations(long id, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri);



    // === Properties ===

    Object fetchTopicProperty(long topicId, String propUri);

    Object fetchAssociationProperty(long assocId, String propUri);

    // ---

    Collection<TopicModel> fetchTopicsByProperty(String propUri, Object propValue);

    Collection<TopicModel> fetchTopicsByPropertyRange(String propUri, Number from, Number to);

    Collection<AssociationModel> fetchAssociationsByProperty(String propUri, Object propValue);

    Collection<AssociationModel> fetchAssociationsByPropertyRange(String propUri, Number from, Number to);

    // ---

    void storeTopicProperty(long topicId, String propUri, Object propValue, boolean addToIndex);

    void storeAssociationProperty(long assocId, String propUri, Object propValue, boolean addToIndex);

    // ---

    boolean hasTopicProperty(long topicId, String propUri);

    boolean hasAssociationProperty(long assocId, String propUri);

    // ---

    void deleteTopicProperty(long topicId, String propUri);

    void deleteAssociationProperty(long assocId, String propUri);



    // === DB ===

    DeepaMehtaTransaction beginTx();

    boolean setupRootNode();

    void shutdown();
}
