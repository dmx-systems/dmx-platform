package de.deepamehta.core.storage;

import de.deepamehta.core.DeepaMehtaTransaction;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;

import java.util.List;
import java.util.Set;



/**
 * Abstraction of the DeepaMehta storage layer.
 */
public interface DeepaMehtaStorage {



    // === Topics ===

    TopicModel getTopic(long topicId);

    /**
     * Looks up a single topic by exact property value.
     * If no such topic exists <code>null</code> is returned.
     * If more than one topic were found a runtime exception is thrown.
     * <p>
     * IMPORTANT: Looking up a topic this way requires the property to be indexed with indexing mode <code>KEY</code>.
     * This is achieved by declaring the respective data field with <code>indexing_mode: "KEY"</code>
     * (for statically declared data field, typically in <code>types.json</code>) or
     * by calling DataField's {@link DataField#setIndexingMode} method with <code>"KEY"</code> as argument
     * (for dynamically created data fields, typically in migration classes).
     */
    TopicModel getTopic(String key, SimpleValue value);

    // ---

    RelatedTopicModel getTopicRelatedTopic(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                       String othersRoleTypeUri, String othersTopicTypeUri);

    ResultSet<RelatedTopicModel> getTopicRelatedTopics(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                       String othersRoleTypeUri, String othersTopicTypeUri,
                                                       int maxResultSize);

    /**
     * @param   assocTypeUris       may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    ResultSet<RelatedTopicModel> getTopicRelatedTopics(long topicId, List<String> assocTypeUris, String myRoleTypeUri,
                                                       String othersRoleTypeUri, String othersTopicTypeUri,
                                                       int maxResultSize);

    // ---

    /**
     * @param   myRoleTypeUri       may be null
     */
    Set<AssociationModel> getTopicAssociations(long topicId, String myRoleTypeUri);

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersAssocTypeUri  may be null
     */
    Set<RelatedAssociationModel> getTopicRelatedAssociations(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                             String othersRoleTypeUri, String othersAssocTypeUri);

    // ---

    Set<TopicModel> searchTopics(String searchTerm, String fieldUri);

    // ---

    /**
     * Stores and indexes the topic's URI.
     */
    void setTopicUri(long topicId, String uri);

    /**
     * Stores the topic's value.
     * <p>
     * Note: the value is not indexed automatically. Use the {@link indexTopicValue} method.
     *
     * @return  The previous value, or <code>null</code> if no value was stored before.
     */
    SimpleValue setTopicValue(long topicId, SimpleValue value);

    /**
     * @param   oldValue    may be null
     */
    void indexTopicValue(long topicId, IndexMode indexMode, String indexKey, SimpleValue value, SimpleValue oldValue);

    /**
     * Creates a topic.
     * <p>
     * The topic's URI is stored and indexed.
     *
     * @return  FIXME ### the created topic. Note:
     *          - the topic URI   is initialzed and     persisted.
     *          - the topic value is initialzed but not persisted.
     *          - the type URI    is initialzed but not persisted.
     */
    void createTopic(TopicModel topicModel);

    // void setTopicProperties(long id, Properties properties);

    /**
     * Deletes the topic.
     * <p>
     * Prerequisite: the topic has no relations.
     */
    void deleteTopic(long topicId);



    // === Associations ===

    AssociationModel getAssociation(long assocId);

    /**
     * Returns the association between two topics, qualified by association type and both role types.
     * If no such association exists <code>null</code> is returned.
     * If more than one association exist, a runtime exception is thrown.
     *
     * @param   assocTypeUri    Association type filter. Pass <code>null</code> to switch filter off.
     *                          ### FIXME: for methods with a singular return value all filters should be mandatory
     */
    AssociationModel getAssociation(String assocTypeUri, long topic1Id, long topic2Id, String roleTypeUri1,
                                                                                       String roleTypeUri2);

    AssociationModel getAssociationBetweenTopicAndAssociation(String assocTypeUri, long topicId, long assocId,
                                                              String topicRoleTypeUri, String assocRoleTypeUri);

    /**
     * Returns the associations between two topics. If no such association exists an empty set is returned.
     *
     * @param   assocTypeUri    Association type filter. Pass <code>null</code> to switch filter off.
     */
    Set<AssociationModel> getAssociations(long topic1Id, long topic2Id, String assocTypeUri);

    // ---

    RelatedTopicModel getAssociationRelatedTopic(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                             String othersRoleTypeUri, String othersTopicTypeUri);

    ResultSet<RelatedTopicModel> getAssociationRelatedTopics(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                             String othersRoleTypeUri, String othersTopicTypeUri,
                                                             int maxResultSize);

    /**
     * @param   assocTypeUris       may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    ResultSet<RelatedTopicModel> getAssociationRelatedTopics(long assocId, List<String> assocTypeUris, String myRoleTypeUri,
                                                             String othersRoleTypeUri, String othersTopicTypeUri,
                                                             int maxResultSize);

    // ---

    /**
     * @param   myRoleTypeUri       may be null
     */
    Set<AssociationModel> getAssociationAssociations(long assocId, String myRoleTypeUri);

    RelatedAssociationModel getAssociationRelatedAssociation(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                             String othersRoleTypeUri);

    // ---

    void setRoleTypeUri(long assocId, long objectId, String roleTypeUri);

    // ---

    /**
     * Stores and indexes the association's URI.
     */
    void setAssociationUri(long assocId, String uri);

    /**
     * Stores the association's value.
     * <p>
     * Note: the value is not indexed automatically. Use the {@link indexAssociationValue} method.
     *
     * @return  The previous value, or <code>null</code> if no value was stored before.
     */
    SimpleValue setAssociationValue(long assocId, SimpleValue value);

    /**
     * @param   oldValue    may be null
     */
    void indexAssociationValue(long assocId, IndexMode indexMode, String indexKey, SimpleValue value,
                                                                                   SimpleValue oldValue);

    // ---

    void createAssociation(AssociationModel assocModel);

    void deleteAssociation(long assocId);



    // === DB ===

    DeepaMehtaTransaction beginTx();

    /**
     * Initializes the database.
     * Prerequisite: there is an open transaction.
     *
     * @return  <code>true</code> if a clean install is detected, <code>false</code> otherwise.
     */
    boolean init();

    void shutdown();

    int getMigrationNr();

    void setMigrationNr(int migrationNr);
}
