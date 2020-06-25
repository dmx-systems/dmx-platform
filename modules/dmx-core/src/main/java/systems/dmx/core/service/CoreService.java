package systems.dmx.core.service;

import systems.dmx.core.Assoc;
import systems.dmx.core.AssocType;
import systems.dmx.core.DMXObject;
import systems.dmx.core.QueryResult;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.AssocTypeModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicTypeModel;
import systems.dmx.core.service.accesscontrol.PrivilegedAccess;
import systems.dmx.core.storage.spi.DMXTransaction;

import java.util.List;



/**
 * Specification of the DMX core service -- the heart of DMX.
 * <p>
 * The responsibility of the DMX core service is to orchestrate the control flow and allow plugins to hook in.
 * The main duties of the DMX core service are to provide access to the storage layer and to dispatch events to
 * the installed plugins. ### FIXDOC
 * <p>
 * The DMX core service is a realization of the <i>Inversion of Control</i> pattern.
 * <p>
 * The DMX core service provides methods to deal with topics, associations, types, and plugins.
 * <p>
 * Plugin developer notes: Inside the {@link PluginActivator} and {@link Migration} classes an instance of the
 * DMX core service is available through the <code>dmx</code> object.
 */
public interface CoreService {



    // === Topics ===

    /**
     * Accesses a topic by ID.
     *
     * @return  the topic.
     *
     * @throws  RuntimeException    if no such topic exists.
     */
    Topic getTopic(long topicId);

    /**
     * Accesses a topic by URI.
     *
     * @return  the topic, or <code>null</code> if no such topic exists.
     */
    Topic getTopicByUri(String uri);

    List<Topic> getTopicsByType(String topicTypeUri);

    Iterable<Topic> getAllTopics();

    // ---

    /**
     * Retrieves a single topic by type and exact value. Throws if more than one topic is found.
     * Lucene query syntax (phrase, wildcards, escaping, ...) is not supported.
     * Text searches are case-sensitive.
     *
     * @param   typeUri     a topic type URI; only topics of this type are searched; mandatory
     * @param   value       the value to search for
     *
     * @return  the found topic, or <code>null</code> if no topic is found.
     *
     * @throws  RuntimeException    If more than one topic is found.
     * @throws  RuntimeException    If null is given for "typeUri".
     */
    Topic getTopicByValue(String typeUri, SimpleValue value);

    /**
     * Retrieves topics by type and exact value.
     * Lucene query syntax (phrase, wildcards, escaping, ...) is not supported.
     * Text searches are case-sensitive.
     *
     * @param   typeUri     a topic type URI; only topics of this type are searched; mandatory
     * @param   value       the value to search for
     *
     * @return  a list of found topics, may be empty.
     *
     * @throws  RuntimeException    If null is given for "typeUri".
     */
    List<Topic> getTopicsByValue(String typeUri, SimpleValue value);

    /**
     * Retrieves topics by type and value.
     * For text-values Lucene query syntax is supported:
     *      "*" matches arbitrary characters
     *      "?" matches a single character
     *      phrases enclosed in double quotes (")
     *      escaping by preceding with back slash (\)
     * A topic is regarded a hit if the search term matches the topic's entire value (in contrast to a fulltext search).
     * Spaces must be escaped though.
     * Text searches are case-sensitive.
     *
     * @param   typeUri     a topic type URI; only topics of this type are searched; mandatory
     * @param   value       the value to search for
     *
     * @return  a list of found topics, may be empty.
     *
     * @throws  RuntimeException    If null is given for "typeUri".
     */
    List<Topic> queryTopics(String typeUri, SimpleValue value);

    /**
     * Performs a fulltext search in topic values and in entire topic trees.
     * Single words are found in entire text-value.
     * Lucene query syntax is supported:
     *      "*" matches arbitrary characters
     *      "?" matches a single character
     *      phrases enclosed in double quotes (")
     *      escaping by preceding with back slash (\)
     *      combining search terms by "AND" and "OR" (the default). "AND" and "&&" as well as "OR" and "||" are
     *      synonymous respectively (uppercase required).
     * Search is case-insensitive.
     *
     * @param   query               The search query.
     * @param   topicTypeUri        Optional: only topics of this type are searched. If null all topics are searched.
     *                              If given, all returned topics are of this type (regardless of the
     *                              "searchChildTopics" flag).
     * @param   searchChildTopics   Applicable only if "topicTypeUri" is given (ignored otherwise): if true the topic's
     *                              child topics are searched as well.
     *                              Example: to search for Persons where "Berlin" appears in *any* child topic pass
     *                              "dmx.contacts.person" for "topicTypeUri", and set "searchChildTopics" to true.
     *
     * @return  a QueryResult object that wraps both the original query parameters and the resulting topic list (may be
     *          empty).
     */
    QueryResult queryTopicsFulltext(String query, String topicTypeUri, boolean searchChildTopics);

    // ---

    Topic createTopic(TopicModel model);

    void updateTopic(TopicModel updateModel);

    void deleteTopic(long topicId);



    // === Associations ===

    Assoc getAssoc(long assocId);

    List<PlayerModel> getPlayerModels(long assocId);

    List<Assoc> getAssocsByType(String assocTypeUri);

    /**
     * Returns all associations between two topics. If no such association exists an empty list is returned.
     */
    List<Assoc> getAssocs(long topic1Id, long topic2Id);

    /**
     * Returns the associations between two topics. If no such association exists an empty list is returned.
     *
     * @param   assocTypeUri    Assoc type filter. Pass <code>null</code> to switch filter off.
     */
    List<Assoc> getAssocs(long topic1Id, long topic2Id, String assocTypeUri);

    /**
     * Returns the association between two topics, qualified by association type and both role types.
     * If no such association exists <code>null</code> is returned.
     * If more than one association exist, a runtime exception is thrown.
     *
     * TODO: rename to "getAssocBetweenTopicAndTopic"
     *
     * @param   assocTypeUri    Assoc type filter. Pass <code>null</code> to switch filter off.
     */
    Assoc getAssocBetweenTopicAndTopic(String assocTypeUri, long topic1Id, long topic2Id, String roleTypeUri1,
                                       String roleTypeUri2);

    Assoc getAssocBetweenTopicAndAssoc(String assocTypeUri, long topicId, long assocId, String topicRoleTypeUri,
                                       String assocRoleTypeUri);

    Iterable<Assoc> getAllAssocs();

    // ---

    /**
     * Looks up a single association by exact value.
     * <p>
     * Note: wildcards like "*" in String values are <i>not</i> interpreted. They are treated literally.
     * Compare to {@link #queryAssocs(String,SimpleValue)}
     *
     * @return  the association, or <code>null</code> if no such association exists.
     *
     * @throws  RuntimeException    If more than one association is found.
     */
    Assoc getAssocByValue(String typeUri, SimpleValue value);

    /**
     * Looks up associations by typeUri and value.
     * <p>
     * Wildcards like "*" in String values <i>are</i> interpreted.
     */
    List<Assoc> queryAssocs(String typeUri, SimpleValue value);

    // ---

    Assoc createAssoc(AssocModel model);

    void updateAssoc(AssocModel updateModel);

    void deleteAssoc(long assocId);



    // === Topic Types ===

    TopicType getTopicType(String topicTypeUri);

    /**
     * Acccesses a topic type while enforcing the <i>implicit READ permission</i>.
     * A user has implicit READ permission for the topic type if she has READ permission for the given topic.
     */
    TopicType getTopicTypeImplicitly(long topicId);

    List<TopicType> getAllTopicTypes();

    // ---

    TopicType createTopicType(TopicTypeModel model);

    void updateTopicType(TopicTypeModel updateModel);

    void deleteTopicType(String topicTypeUri);



    // === Assoc Types ===

    AssocType getAssocType(String assocTypeUri);

    /**
     * Acccesses an association type while enforcing the <i>implicit READ permission</i>.
     * A user has implicit READ permission for the association type if she has READ permission for the given
     * association.
     */
    AssocType getAssocTypeImplicitly(long assocId);

    List<AssocType> getAllAssocTypes();

    // ---

    AssocType createAssocType(AssocTypeModel model);

    void updateAssocType(AssocTypeModel updateModel);

    void deleteAssocType(String assocTypeUri);



    // === Role Types ===

    Topic createRoleType(TopicModel model);



    // === Generic Object ===

    DMXObject getObject(long id);



    // === Plugins ===

    Plugin getPlugin(String pluginUri);

    List<PluginInfo> getPluginInfo();



    // === Events ===

    void fireEvent(DMXEvent event, Object... params);

    void dispatchEvent(String pluginUri, DMXEvent event, Object... params);



    // === Properties ===

    /**
     * Returns a topic's or association's property value associated with the given property URI.
     * If there's no property value associated with the property URI an exception is thrown.
     *
     * @param   id  a topic ID, or an association ID
     */
    Object getProperty(long id, String propUri);

    /**
     * Checks whether for a given topic or association a property value is associated with a given property URI.
     *
     * @param   id  a topic ID, or an association ID
     */
    boolean hasProperty(long id, String propUri);

    // Note: there is no setter here. If we want one we actually need 2 setters: one for topics, one for assocs.
    // This is because the storage layer maintains separate indexes for topics and assocs.

    // ---

    List<Topic> getTopicsByProperty(String propUri, Object propValue);

    List<Topic> getTopicsByPropertyRange(String propUri, Number from, Number to);

    List<Assoc> getAssocsByProperty(String propUri, Object propValue);

    List<Assoc> getAssocsByPropertyRange(String propUri, Number from, Number to);

    // ---

    void addTopicPropertyIndex(String propUri);

    void addAssocPropertyIndex(String propUri);



    // === Misc ===

    DMXTransaction beginTx();

    // ---

    ModelFactory getModelFactory();

    PrivilegedAccess getPrivilegedAccess();

    WebSocketsService getWebSocketsService();

    Object getDatabaseVendorObject();
}
