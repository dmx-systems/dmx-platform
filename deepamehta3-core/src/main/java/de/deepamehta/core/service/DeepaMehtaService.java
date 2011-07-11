package de.deepamehta.core.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DeepaMehtaTransaction;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.ClientContext;
import de.deepamehta.core.model.CommandParams;
import de.deepamehta.core.model.CommandResult;
import de.deepamehta.core.model.PluginInfo;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicTypeModel;

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
public interface DeepaMehtaService {



    // === Topics ===

    public Topic getTopic(long id, boolean fetchComposite, ClientContext clientContext);

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
    public Topic getTopic(String key, TopicValue value, boolean fetchComposite);

    // public Topic getTopic(String typeUri, String key, TopicValue value);

    public Set<RelatedTopic> getTopics(String typeUri);

    /**
     * Performs a fulltext search.
     *
     * @param   fieldUri    The URI of the data field to search. If null is provided all fields are searched.
     * @param   wholeWord   If true the searchTerm is regarded as whole word.
     *                      If false the searchTerm is regarded as begin-of-word substring.
     */
    public Set<Topic> searchTopics(String searchTerm, String fieldUri, boolean wholeWord,
                                                                       ClientContext clientContext);

    public Topic createTopic(TopicModel topicModel, ClientContext clientContext);

    public Topic updateTopic(TopicModel topicModel, ClientContext clientContext);

    public Directives deleteTopic(long topicId, ClientContext clientContext);



    // === Associations ===

    public Association getAssociation(long assocId);

    /**
     * Returns the relation between two topics. If no such relation exists null is returned.
     * If more than one relation exists, an exception is thrown.
     *
     * @param   typeId      Relation type filter. Pass <code>null</code> to switch filter off.
     * @param   isDirected  Direction filter. Pass <code>true</code> if direction matters. In this case the relation
     *                      is expected to be directed <i>from</i> source topic <i>to</i> destination topic.
     */
    // public Relation getRelation(long srcTopicId, long dstTopicId, String typeId, boolean isDirected);

    /**
     * Returns all associations between two topics. If no such association exists an empty set is returned.
     */
    public Set<Association> getAssociations(long topic1Id, long topic2Id);

    /**
     * Returns the associations between two topics. If no such association exists an empty set is returned.
     *
     * @param   assocTypeUri    Association type filter. Pass <code>null</code> to switch filter off.
     */
    public Set<Association> getAssociations(long topic1Id, long topic2Id, String assocTypeUri);

    // ---

    public Association createAssociation(AssociationModel assocModel, ClientContext clientContext);

    public Directives updateAssociation(AssociationModel assocModel, ClientContext clientContext);

    public Directives deleteAssociation(long assocId, ClientContext clientContext);



    // === Topic Types ===

    public Set<String> getTopicTypeUris();

    public TopicType getTopicType(String topicTypeUri, ClientContext clientContext);

    public TopicType createTopicType(TopicTypeModel topicTypeModel, ClientContext clientContext);

    public TopicType updateTopicType(TopicTypeModel topicTypeModel, ClientContext clientContext);



    // === Association Types ===

    public Set<String> getAssociationTypeUris();

    public AssociationType getAssociationType(String assocTypeUri, ClientContext clientContext);

    public AssociationType createAssociationType(AssociationTypeModel assocTypeModel, ClientContext clientContext);



    // === Commands ===

    public CommandResult executeCommand(String command, CommandParams params, ClientContext clientContext);



    // === Plugins ===

    public void registerPlugin(Plugin plugin);

    public void unregisterPlugin(String pluginId);

    public Plugin getPlugin(String pluginId);

    public Set<PluginInfo> getPluginInfo();

    public void runPluginMigration(Plugin plugin, int migrationNr, boolean isCleanInstall);



    // === Misc ===

    public DeepaMehtaTransaction beginTx();

    /**
     * Checks if all DeepaMehta plugin bundles are registered at core.
     * Triggers the ALL_PLUGINS_READY hook if so.
     * <p>
     * Called from the Plugin class.
     * Not meant to be called by a plugin developer.
     */
    public void checkPluginsReady();

    /**
     * Setups the database to be compatible with this core service.
     * <p>
     * Called from the core activator.
     * Not meant to be called by a plugin developer.
     */
    public void setupDB();

    /**
     * Shuts down the database.
     * Called when the core service stops.
     * <p>
     * Called from the core activator.
     * Not meant to be called by a plugin developer.
     */
    public void shutdown();
}
