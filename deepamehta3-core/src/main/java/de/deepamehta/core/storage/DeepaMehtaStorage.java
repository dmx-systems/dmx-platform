package de.deepamehta.core.storage;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.AssociationType;
import de.deepamehta.core.model.MetaType;
import de.deepamehta.core.model.RelatedTopic;
import de.deepamehta.core.model.Relation;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicData;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.model.TopicTypeDefinition;
import de.deepamehta.core.model.TopicValue;

import java.util.Map;
import java.util.List;
import java.util.Set;



/**
 * Abstraction of the DeepaMehta storage layer.
 */
public interface DeepaMehtaStorage {

    // === Topics ===

    // Topic getTopic(long id);

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
    Topic getTopic(String key, TopicValue value);

    // Topic getTopic(String typeUri, String key, PropValue value);

    /**
     * Returns a property value of a topic.
     * If the topic has no such property a "no-value" representing {@link PropValue} object is returned.
     */
    // PropValue getTopicProperty(long topicId, String key);

    // List<Topic> getTopics(String typeUri);

    /**
     * Looks up topics by exact property value.
     * If no such topics exists an empty list is returned.
     * <p>
     * IMPORTANT: Looking up a topic this way requires the property to be indexed with indexing mode <code>KEY</code>.
     * This is achieved by declaring the respective data field with <code>indexing_mode: "KEY"</code>
     * (for statically declared data field, typically in <code>types.json</code>) or
     * by calling DataField's {@link DataField#setIndexingMode} method with <code>"KEY"</code> as argument
     * (for dynamically created data fields, typically in migration classes).
     */
    // List<Topic> getTopics(String key, Object value);

    // List<RelatedTopic> getRelatedTopics(long topicId, List<String> includeTopicTypes,
    //                                                          List<String> includeRelTypes,
    //                                                          List<String> excludeRelTypes);

    // List<Topic> searchTopics(String searchTerm, String fieldUri, boolean wholeWord);

    Topic createTopic(TopicData topicData);

    // void setTopicProperties(long id, Properties properties);

    /**
     * Deletes the topic.
     * <p>
     * Prerequisite: the topic has no relations.
     */
    // void deleteTopic(long id);

    // === Associations ===

    // Relation getRelation(long id);

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
     * Returns the relations between two topics. If no such relation exists an empty list is returned.
     *
     * @param   typeId      Relation type filter. Pass <code>null</code> to switch filter off.
     * @param   isDirected  Direction filter. Pass <code>true</code> if direction matters. In this case the relation
     *                      is expected to be directed <i>from</i> source topic <i>to</i> destination topic.
     */
    // List<Relation> getRelations(long srcTopicId, long dstTopicId, String typeId, boolean isDirected);

    Association createAssociation(Association assoc);

    // void setRelationProperties(long id, Properties properties);

    // void deleteRelation(long id);

    // === Types ===

    // Set<String> getTopicTypeUris();

    TopicType getTopicType(String typeUri);

    TopicTypeDefinition getTopicTypeDefinition(String typeUri);

    MetaType createMetaType(MetaType metaType);

    TopicType createTopicType(TopicType topicType);

    AssociationType createAssociationType(AssociationType assocType);

    // void addDataField(String typeUri, DataField dataField);

    // void updateDataField(String typeUri, DataField dataField);

    // void removeDataField(String typeUri, String fieldUri);

    // void setDataFieldOrder(String typeUri, List fieldUris);

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
