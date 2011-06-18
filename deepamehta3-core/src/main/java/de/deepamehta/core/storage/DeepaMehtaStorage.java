package de.deepamehta.core.storage;

import de.deepamehta.core.DeepaMehtaTransaction;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicValue;

import java.util.Map;
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
     * If more than one topic is found a runtime exception is thrown.
     * <p>
     * IMPORTANT: Looking up a topic this way requires the property to be indexed with indexing mode <code>KEY</code>.
     * This is achieved by declaring the respective data field with <code>indexing_mode: "KEY"</code>
     * (for statically declared data field, typically in <code>types.json</code>) or
     * by calling DataField's {@link DataField#setIndexingMode} method with <code>"KEY"</code> as argument
     * (for dynamically created data fields, typically in migration classes).
     */
    TopicModel getTopic(String key, TopicValue value);

    // ---

    /**
     * @param   assocTypeUris       may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    Set<RelatedTopicModel> getTopicRelatedTopics(long topicId, List assocTypeUris, String myRoleTypeUri,
                                                                                   String othersRoleTypeUri,
                                                                                   String othersTopicTypeUri);

    // ---

    Set<AssociationModel> getAssociations(long topicId);

    Set<AssociationModel> getAssociations(long topicId, String myRoleTypeUri);

    AssociationModel getTopicRelatedAssociation(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                                                   String othersRoleTypeUri);

    // ---

    Set<TopicModel> searchTopics(String searchTerm, String fieldUri, boolean wholeWord);

    // ---

    /**
     * Stores and indexes the topic URI.
     */
    void setTopicUri(long topicId, String uri);

    /**
     * Sets the topic value.
     * <p>
     * Note: the value is not indexed automatically. Use the {@link indexTopicValue} method.
     *
     * @return  The previous topic value, or <code>null</code> if no value was set before.
     */
    TopicValue setTopicValue(long topicId, TopicValue value);

    /**
     * @param   oldValue    may be null
     */
    void indexTopicValue(long topicId, IndexMode indexMode, String indexKey, TopicValue value, TopicValue oldValue);

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

    // Set<Relation> getRelations(long topicId);

    /**
     * Returns the relation between two topics. If no such relation exists null is returned.
     * If more than one relation exists, an exception is thrown.
     *
     * @param   typeId      Relation type filter. Pass <code>null</code> to switch filter off.
     * @param   isDirected  Direction filter. Pass <code>true</code> if direction matters. In this case the relation
     *                      is expected to be directed <i>from</i> source topic <i>to</i> destination topic.
     */
    // Relation getRelation(long srcTopicId, long dstTopicId, String typeId, boolean isDirected);

    /**
     * Returns the associations between two topics. If no such association exists an empty set is returned.
     *
     * @param   assocTypeUri    Association type filter. Pass <code>null</code> to switch filter off.
     */
    Set<AssociationModel> getAssociations(long topic1Id, long topic2Id, String assocTypeUri);

    // ---

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    Set<RelatedTopicModel> getAssociationRelatedTopics(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                                                          String othersRoleTypeUri,
                                                                                          String othersTopicTypeUri);

    // ---

    AssociationModel getAssociationRelatedAssociation(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                                                         String othersRoleTypeUri);

    // ---

    void setRoleTypeUri(long assocId, long objectId, String roleTypeUri);

    // ---

    void createAssociation(AssociationModel assocModel);

    void deleteAssociation(long assocId);



    // === DB ===

    DeepaMehtaTransaction beginTx();

    /**
     * Performs storage layer initialization. Runs in a transaction.
     *
     * @return  <code>true</code> if this is a clean install, <code>false</code> otherwise.
     */
    boolean init();

    void shutdown();

    int getMigrationNr();

    void setMigrationNr(int migrationNr);
}
