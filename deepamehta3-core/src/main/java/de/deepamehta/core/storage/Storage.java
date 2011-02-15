package de.deepamehta.core.storage;

import de.deepamehta.core.model.DataField;
import de.deepamehta.core.model.Properties;
import de.deepamehta.core.model.PropValue;
import de.deepamehta.core.model.RelatedTopic;
import de.deepamehta.core.model.Relation;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicType;

import java.util.Map;
import java.util.List;
import java.util.Set;



/**
 * Abstraction of the DeepaMehta storage layer.
 */
public interface Storage {

    // --- Topics ---

    public Topic getTopic(long id);

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
    public Topic getTopic(String key, PropValue value);

    public Topic getTopic(String typeUri, String key, PropValue value);

    /**
     * Returns a property value of a topic, or <code>null</code> if the topic doesn't have such a property.
     */
    public PropValue getTopicProperty(long topicId, String key);

    public List<Topic> getTopics(String typeUri);

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
    public List<Topic> getTopics(String key, Object value);

    public List<RelatedTopic> getRelatedTopics(long topicId, List<String> includeTopicTypes,
                                                             List<String> includeRelTypes,
                                                             List<String> excludeRelTypes);

    public List<Topic> searchTopics(String searchTerm, String fieldUri, boolean wholeWord);

    public Topic createTopic(String typeUri, Properties properties);

    public void setTopicProperties(long id, Properties properties);

    /**
     * Deletes the topic.
     * <p>
     * Prerequisite: the topic has no relations.
     */
    public void deleteTopic(long id);

    // --- Relations ---

    public Relation getRelation(long id);

    public Set<Relation> getRelations(long topicId);

    /**
     * Returns the relation between two topics. If no such relation exists null is returned.
     * If more than one relation exists, an exception is thrown.
     *
     * @param   typeId      Relation type filter. Pass <code>null</code> to switch filter off.
     * @param   isDirected  Direction filter. Pass <code>true</code> if direction matters. In this case the relation
     *                      is expected to be directed <i>from</i> source topic <i>to</i> destination topic.
     */
    public Relation getRelation(long srcTopicId, long dstTopicId, String typeId, boolean isDirected);

    /**
     * Returns the relations between two topics. If no such relation exists an empty list is returned.
     *
     * @param   typeId      Relation type filter. Pass <code>null</code> to switch filter off.
     * @param   isDirected  Direction filter. Pass <code>true</code> if direction matters. In this case the relation
     *                      is expected to be directed <i>from</i> source topic <i>to</i> destination topic.
     */
    public List<Relation> getRelations(long srcTopicId, long dstTopicId, String typeId, boolean isDirected);

    public Relation createRelation(String typeId, long srcTopicId, long dstTopicId, Properties properties);

    public void setRelationProperties(long id, Properties properties);

    public void deleteRelation(long id);

    // --- Types ---

    public Set<String> getTopicTypeUris();

    public TopicType getTopicType(String typeUri);

    public TopicType createTopicType(Properties properties, List<DataField> dataFields);

    public void addDataField(String typeUri, DataField dataField);

    public void updateDataField(String typeUri, DataField dataField);

    public void removeDataField(String typeUri, String fieldUri);

    public void setDataFieldOrder(String typeUri, List fieldUris);

    // --- DB ---

    public Transaction beginTx();

    /**
     * @return  <code>true</code> if this is a clean install, <code>false</code> otherwise.
     */
    public boolean init();

    public void shutdown();

    public int getMigrationNr();

    public void setMigrationNr(int migrationNr);
}
