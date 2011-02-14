package de.deepamehta.core.service;

import de.deepamehta.core.model.ClientContext;
import de.deepamehta.core.model.DataField;
import de.deepamehta.core.model.PropValue;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.model.RelatedTopic;
import de.deepamehta.core.model.Relation;
import de.deepamehta.core.storage.Transaction;

import org.codehaus.jettison.json.JSONObject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;



/**
 * Specification of the DeepaMehta core service -- the heart of DeepaMehta.
 * <p>
 * The responsibility of the DeepaMehta core service is to orchestrate the control flow and allow plugins to hook in.
 * The main duties of the DeepaMehta core service are to provide access to the storage layer and to trigger hooks of
 * the registered plugins.
 * <p>
 * The DeepaMehta core service is a realization of the <i>Inversion of Control</i> pattern.
 * <p>
 * The DeepaMehta core service provides methods to deal with topics, relations, types, commands, and plugins.
 * <p>
 * Plugin developer notes: Inside the {@link Plugin} and {@link Migration} classes an instance of the DeepaMehta
 * core service is available through the <code>dms</code> object.
 */
public interface CoreService {

    // === Topics ===

    public Topic getTopic(long id, ClientContext clientContext);

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

    public Object getTopicProperty(long topicId, String key);

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

    /**
     * Retrieves topics and relationships that are directly connected to the given topic, optionally filtered
     * by topic types and relation types.
     *
     * IMPORTANT: the topics and relations returned by this method provide no properties.
     * To initialize the properties needed by your plugin define its providePropertiesHook().
     *
     * @param   includeTopicTypes   The include topic type filter (optional).
     *                              A list of topic type URIs (strings), e.g. "de/deepamehta/core/topictype/Note".
     *                              Null or an empty list switches the filter off.
     * @param   includeRelTypes     The include relation type filter (optional).
     *                              A list of strings of the form "<relTypeName>[;<direction>]",
     *                              e.g. "TOPICMAP_TOPIC;INCOMING".
     *                              Null or an empty list switches the filter off.
     * @param   excludeRelTypes     The exclude relation type filter (optional).
     *                              A list of strings of the form "<relTypeName>[;<direction>]",
     *                              e.g. "SEARCH_RESULT;OUTGOING".
     *                              Null or an empty list switches the filter off.
     *
     * @return  The related topics, each one as a pair: the topic (a Topic object), and the connecting relation
     *          (a Relation object).
     */
    public List<RelatedTopic> getRelatedTopics(long topicId, List<String> includeTopicTypes,
                                                             List<String> includeRelTypes,
                                                             List<String> excludeRelTypes);

    /**
     * Performs a fulltext search.
     *
     * @param   fieldUri    The URI of the data field to search. If null is provided all fields are searched.
     * @param   wholeWord   If true the searchTerm is regarded as whole word.
     *                      If false the searchTerm is regarded as begin-of-word substring.
     */
    public List<Topic> searchTopics(String searchTerm, String fieldUri, boolean wholeWord, ClientContext clientContext);

    public Topic createTopic(String typeUri, Map properties, ClientContext clientContext);

    public void setTopicProperties(long id, Map properties);

    public void deleteTopic(long id);

    // === Relations ===

    public Relation getRelation(long id);

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

    public Relation createRelation(String typeId, long srcTopicId, long dstTopicId, Map properties);

    public void setRelationProperties(long id, Map properties);

    public void deleteRelation(long id);

    // === Types ===

    public Set<String> getTopicTypeUris();

    public TopicType getTopicType(String typeUri, ClientContext clientContext);

    public TopicType createTopicType(Map properties, List dataFields, ClientContext clientContext);

    public void addDataField(String typeUri, DataField dataField);

    public void updateDataField(String typeUri, DataField dataField);

    public void removeDataField(String typeUri, String fieldUri);

    public void setDataFieldOrder(String typeUri, List fieldUris);

    // === Commands ===

    public JSONObject executeCommand(String command, Map params, ClientContext clientContext);

    // === Plugins ===

    public void registerPlugin(Plugin plugin);

    public void unregisterPlugin(String pluginId);

    public Set<String> getPluginIds();

    public Plugin getPlugin(String pluginId);

    public void runPluginMigration(Plugin plugin, int migrationNr, boolean isCleanInstall);

    // === Misc ===

    public void startup();

    public void shutdown();

    public Transaction beginTx();
}
