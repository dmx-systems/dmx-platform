package de.deepamehta.core.storage.spi;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ModelFactory;

import java.util.Iterator;
import java.util.List;



public interface DeepaMehtaStorage {



    // === Topics ===

    TopicModel fetchTopic(long topicId);

    TopicModel fetchTopic(String key, Object value);

    List<TopicModel> fetchTopics(String key, Object value);

    List<TopicModel> queryTopics(Object value);

    List<TopicModel> queryTopics(String key, Object value);

    Iterator<TopicModel> fetchAllTopics();

    // ---

    /**
     * Stores a rudimentary topic in the DB.
     * <p>
     * Only the topic URI and the topic type URI are stored.
     * The topic value (simple or composite) is <i>not</i> stored.
     * The "Instantiation" association is <i>not</i> stored.
     * <p>
     * An URI uniqueness check is performed. If the DB already contains a topic or an association with
     * the URI passed, an exception is thrown and nothing is stored.
     *
     * @param   topicModel  The topic to store. Once the method returns the topic model contains:
     *                          - the ID of the stored topic.
     *                          - an empty URI ("") in case <code>null</code> was passed.
     *                          - an empty simple value ("") in case <code>null</code> was passed.
     */
    void storeTopic(TopicModel topicModel);

    void storeTopicUri(long topicId, String uri);

    void storeTopicTypeUri(long topicId, String topicTypeUri);

    /**
     * Stores and indexes a topic value.
     *
     * @param   indexValue  Optional: the value to be indexed. If indexValue is not specified, value is used.
     */
    void storeTopicValue(long topicId, SimpleValue value, List<IndexMode> indexModes, String indexKey,
                                                                                      SimpleValue indexValue);

    void indexTopicValue(long topicId, IndexMode indexMode, String indexKey, SimpleValue indexValue);

    // ---

    void deleteTopic(long topicId);



    // === Associations ===

    AssociationModel fetchAssociation(long assocId);

    AssociationModel fetchAssociation(String key, Object value);

    List<AssociationModel> fetchAssociations(String key, Object value);

    List<AssociationModel> fetchAssociations(String assocTypeUri, long topicId1, long topicId2, String roleTypeUri1,
                                                                                                String roleTypeUri2);

    List<AssociationModel> fetchAssociationsBetweenTopicAndAssociation(String assocTypeUri, long topicId, long assocId,
                                                                      String topicRoleTypeUri, String assocRoleTypeUri);

    Iterator<AssociationModel> fetchAllAssociations();

    long[] fetchPlayerIds(long assocId);

    // ---

    void storeAssociation(AssociationModel assocModel);

    void storeAssociationUri(long assocId, String uri);

    void storeAssociationTypeUri(long assocId, String assocTypeUri);

    /**
     * Stores and indexes an association value.
     *
     * @param   indexValue  Optional: the value to be indexed. If indexValue is not specified, value is used.
     */
    void storeAssociationValue(long assocId, SimpleValue value, List<IndexMode> indexModes, String indexKey,
                                                                                            SimpleValue indexValue);

    void indexAssociationValue(long assocId, IndexMode indexMode, String indexKey, SimpleValue indexValue);

    void storeRoleTypeUri(long assocId, long playerId, String roleTypeUri);

    // ---

    void deleteAssociation(long assocId);



    // === Generic Object ===

    DeepaMehtaObjectModel fetchObject(long id);



    // === Traversal ===

    List<AssociationModel> fetchTopicAssociations(long topicId);

    List<AssociationModel> fetchAssociationAssociations(long assocId);

    // ---

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    List<RelatedTopicModel> fetchTopicRelatedTopics(long topicId, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersTopicTypeUri);

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    List<RelatedAssociationModel> fetchTopicRelatedAssociations(long topicId, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri);

    // ---

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    List<RelatedTopicModel> fetchAssociationRelatedTopics(long assocId, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersTopicTypeUri);

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    List<RelatedAssociationModel> fetchAssociationRelatedAssociations(long assocId, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri);

    // ---

    /**
     * @param   objectId            id of a topic or an association
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    List<RelatedTopicModel> fetchRelatedTopics(long objectId, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersTopicTypeUri);

    /**
     * @param   objectId            id of a topic or an association
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    List<RelatedAssociationModel> fetchRelatedAssociations(long objectId, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri);



    // === Properties ===

    /**
     * @param   id                  id of a topic or an association
     */
    Object fetchProperty(long id, String propUri);

    /**
     * @param   id                  id of a topic or an association
     */
    boolean hasProperty(long id, String propUri);

    // ---

    List<TopicModel> fetchTopicsByProperty(String propUri, Object propValue);

    List<TopicModel> fetchTopicsByPropertyRange(String propUri, Number from, Number to);

    List<AssociationModel> fetchAssociationsByProperty(String propUri, Object propValue);

    List<AssociationModel> fetchAssociationsByPropertyRange(String propUri, Number from, Number to);

    // ---

    void storeTopicProperty(long topicId, String propUri, Object propValue, boolean addToIndex);

    void storeAssociationProperty(long assocId, String propUri, Object propValue, boolean addToIndex);

    // ---

    void indexTopicProperty(long topicId, String propUri, Object propValue);

    void indexAssociationProperty(long assocId, String propUri, Object propValue);

    // ---

    void deleteTopicProperty(long topicId, String propUri);

    void deleteAssociationProperty(long assocId, String propUri);



    // === DB ===

    DeepaMehtaTransaction beginTx();

    boolean setupRootNode();

    void shutdown();

    // ---

    Object getDatabaseVendorObject();

    Object getDatabaseVendorObject(long objectId);

    // ---

    ModelFactory getModelFactory();
}
