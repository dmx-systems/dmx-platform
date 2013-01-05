package de.deepamehta.core.storage.spi;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;

import java.util.List;
import java.util.Set;



/**
 * The MehtaGraph service. ### FIXDOC
 * It provides methods for creation and retrieval of {@link MehtaNode}s and {@link MehtaEdge}s.
 * <p>
 * To obtain a MehtaGraph service instance call {@link MehtaGraphFactory#createInstance}. ### FIXDOC
 */
public interface DeepaMehtaStorage {



    // === Topics ===

    void storeTopic(TopicModel topicModel);

    TopicModel fetchTopic(long id);
    TopicModel fetchTopic(String key, Object value);

    List<TopicModel> fetchTopics(String key, Object value);

    List<TopicModel> queryTopics(Object value);
    List<TopicModel> queryTopics(String key, Object value);

    void storeTopicUri(long topicId, String uri);
    void storeTopicValue(long topicId, SimpleValue value, Set<IndexMode> indexModes, String indexKey);

    void deleteTopic(long topicId);



    // === Associations ===

    void storeAssociation(AssociationModel assocModel);

    AssociationModel fetchAssociation(long id);

    Set<AssociationModel> fetchAssociations(String assocTypeUri, long topicId1, long topicId2, String roleTypeUri1,
                                                                                               String roleTypeUri2);

    Set<AssociationModel> fetchAssociationsBetweenTopicAndAssociation(String assocTypeUri, long topicId, long assocId,
                                                                      String topicRoleTypeUri, String assocRoleTypeUri);

    // ---

    void storeAssociationUri(long assocId, String uri);
    void storeAssociationValue(long assocId, SimpleValue value, Set<IndexMode> indexModes, String indexKey);

    void storeRoleTypeUri(long assocId, long playerId, String roleTypeUri);

    void deleteAssociation(long assocId);



    // === Traversal ===

    Set<AssociationModel> fetchTopicAssociations(long topicId);

    Set<AssociationModel> fetchAssociationAssociations(long assocId);

    // ---

    Set<RelatedTopicModel> fetchTopicRelatedTopics(long topicId, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersTopicTypeUri);

    Set<RelatedAssociationModel> fetchTopicRelatedAssociations(long topicId, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri);

    // ---

    Set<RelatedTopicModel> fetchAssociationRelatedTopics(long assocId, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersTopicTypeUri);

    Set<RelatedAssociationModel> fetchAssociationRelatedAssociations(long assocId, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri);



    // === Properties ===

    Object fetchProperty(long id, String key);

    void storeProperty(long id, String key, Object value);

    boolean hasProperty(long id, String key);



    // === DB ===

    DeepaMehtaTransaction beginTx();

    boolean setupRootNode();

    void shutdown();
}
