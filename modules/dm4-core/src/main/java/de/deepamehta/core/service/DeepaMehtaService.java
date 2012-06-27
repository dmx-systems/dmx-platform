package de.deepamehta.core.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DeepaMehtaTransaction;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.SimpleValue;
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

    Topic getTopic(long id, boolean fetchComposite, ClientState clientState);

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
    Topic getTopic(String key, SimpleValue value, boolean fetchComposite, ClientState clientState);

    ResultSet<Topic> getTopics(String typeUri, boolean fetchComposite, int maxResultSize, ClientState clientState);

    /**
     * Performs a fulltext search.
     *
     * @param   fieldUri    The URI of the data field to search. If null is provided all fields are searched.
     * @param   wholeWord   If true the searchTerm is regarded as whole word.
     *                      If false the searchTerm is regarded as begin-of-word substring.
     */
    Set<Topic> searchTopics(String searchTerm, String fieldUri, boolean wholeWord, ClientState clientState);

    Topic createTopic(TopicModel model, ClientState clientState);

    Directives updateTopic(TopicModel model, ClientState clientState);

    Directives deleteTopic(long topicId, ClientState clientState);



    // === Associations ===

    Association getAssociation(long assocId, boolean fetchComposite, ClientState clientState);

    /**
     * Returns the association between two topics, qualified by association type and both role types.
     * If no such association exists <code>null</code> is returned.
     * If more than one association exist, a runtime exception is thrown.
     *
     * @param   assocTypeUri    Association type filter. Pass <code>null</code> to switch filter off.
     */
    Association getAssociation(String assocTypeUri, long topic1Id, long topic2Id,
                                                    String roleTypeUri1, String roleTypeUri2,
                                                    boolean fetchComposite, ClientState clientState);

    // ---

    /**
     * Returns all associations between two topics. If no such association exists an empty set is returned.
     */
    Set<Association> getAssociations(long topic1Id, long topic2Id);

    /**
     * Returns the associations between two topics. If no such association exists an empty set is returned.
     *
     * @param   assocTypeUri    Association type filter. Pass <code>null</code> to switch filter off.
     */
    Set<Association> getAssociations(long topic1Id, long topic2Id, String assocTypeUri);

    // ---

    Association createAssociation(AssociationModel model, ClientState clientState);

    Directives updateAssociation(AssociationModel model, ClientState clientState);

    Directives deleteAssociation(long assocId, ClientState clientState);



    // === Topic Types ===

    Set<String> getTopicTypeUris();

    TopicType getTopicType(String topicTypeUri, ClientState clientState);

    TopicType createTopicType(TopicTypeModel model, ClientState clientState);

    Directives updateTopicType(TopicTypeModel model, ClientState clientState);



    // === Association Types ===

    Set<String> getAssociationTypeUris();

    AssociationType getAssociationType(String assocTypeUri, ClientState clientState);

    AssociationType createAssociationType(AssociationTypeModel model, ClientState clientState);



    // === Commands ===

    CommandResult executeCommand(String command, CommandParams params, ClientState clientState);



    // === Plugins ===

    void registerPlugin(Plugin plugin);

    void unregisterPlugin(String pluginUri);

    Plugin getPlugin(String pluginUri);

    Set<PluginInfo> getPluginInfo();

    void runPluginMigration(Plugin plugin, int migrationNr, boolean isCleanInstall);

    Map<String, Object> triggerHook(Hook hook, Object... params);



    // === Misc ===

    DeepaMehtaTransaction beginTx();

    ObjectFactory getObjectFactory();

    /**
     * Checks if all DeepaMehta plugin bundles are registered at core.
     * Triggers the ALL_PLUGINS_READY hook if so.
     * <p>
     * Called from the Plugin class.
     * Not meant to be called by a plugin developer.
     */
    void checkAllPluginsReady();

    /**
     * Setups the database to be compatible with this core service.
     * <p>
     * Called from the core activator.
     * Not meant to be called by a plugin developer.
     */
    void setupDB();

    /**
     * Shuts down the database.
     * Called when the core service stops.
     * <p>
     * Called from the core activator.
     * Not meant to be called by a plugin developer.
     */
    void shutdown();
}
