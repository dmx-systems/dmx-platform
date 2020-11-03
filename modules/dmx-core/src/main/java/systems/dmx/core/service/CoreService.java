package systems.dmx.core.service;

import systems.dmx.core.Assoc;
import systems.dmx.core.AssocType;
import systems.dmx.core.DMXObject;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.AssocTypeModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicTypeModel;
import systems.dmx.core.service.TopicResult;
import systems.dmx.core.service.accesscontrol.PrivilegedAccess;
import systems.dmx.core.service.websocket.WebSocketService;
import systems.dmx.core.storage.spi.DMXTransaction;

import java.util.List;



/**
 * The DMX Core Service provides generic database operations (create, retrieve, update, delete) to deal with the DMX
 * Core objects: Topics, Associations, Topic Types, Association Types.
 * <p>
 * Inside the {@link PluginActivator} and {@link Migration} (sub)classes an instance of the DMX Core Service is
 * available as the <code>dmx</code> object.
 */
public interface CoreService {



    // === Topics ===

    /**
     * Retrieves a topic by ID.
     *
     * @return  the topic.
     *
     * @throws  RuntimeException    if no topic is found.
     */
    Topic getTopic(long topicId);

    /**
     * Retrieves a topic by URI.
     *
     * @return  the topic, or <code>null</code> if no topic is found.
     */
    Topic getTopicByUri(String uri);

    List<Topic> getTopicsByType(String topicTypeUri);

    Iterable<Topic> getAllTopics();

    // ---

    /**
     * Retrieves a single topic by type and exact value. Throws if more than one topic is found.
     * Lucene query syntax (wildcards, phrase search, ...) is not supported.
     * A text search is case-sensitive.
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
     * Lucene query syntax (wildcards, phrase search, ...) is not supported.
     * A text search is case-sensitive.
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
     * Lucene query syntax is supported.
     * A topic is regarded a hit if the search term matches the topic's entire value (in contrast to a fulltext search).
     * Spaces must be escaped though.
     * Search is case-sensitive.
     *
     * @param   typeUri     a topic type URI; only topics of this type are searched; mandatory
     * @param   query       The query. Must be non-empty. Lucene query syntax is supported:
     *      <ul>
     *          <li>"*" matches arbitrary characters</li>
     *          <li>"?" matches a single character</li>
     *          <li>phrases enclosed in double quotes (")</li>
     *          <li>escape special character by preceding with back slash (\)</li>
     *          <li>combining search terms by "AND"/"OR" (uppercase required). Default is "OR". "&&" and "||" are
     *              respective synonyms.</li>
     *      </ul>
     *
     * @return  a list of found topics, may be empty.
     *
     * @throws  RuntimeException    If null is given for "typeUri".
     */
    List<Topic> queryTopics(String typeUri, String query);

    /**
     * Performs a fulltext search in topic values and in entire topic trees.
     * Single words are found in entire text-value.
     * Lucene query syntax is supported.
     * Search is case-insensitive.
     *
     * @param   query               The search query. Must be non-empty. Lucene query syntax is supported:
     *      <ul>
     *          <li>"*" matches arbitrary characters</li>
     *          <li>"?" matches a single character</li>
     *          <li>phrases enclosed in double quotes (")</li>
     *          <li>escape special character by preceding with back slash (\)</li>
     *          <li>combining search terms by "AND"/"OR" (uppercase required). Default is "OR". "&&" and "||" are
     *              respective synonyms.</li>
     *      </ul>
     * @param   typeUri             Optional: a topic type URI; only topics of this type are searched. If null all
     *                              topics are searched.<br>
     *                              If given, all returned topics are of this type (regardless of the
     *                              "searchChildTopics" setting).
     * @param   searchChildTopics   Applicable only if "topicTypeUri" is given (ignored otherwise): if true the topic's
     *                              child topics are searched as well.<br>
     *                              Example: to search for Persons where "Berlin" appears in *any* child topic pass
     *                              "dmx.contacts.person" for "topicTypeUri", and set "searchChildTopics" to true.
     *
     * @return  a TopicResult object that wraps both the original query parameters and the resulting topic list (may be
     *          empty).
     */
    TopicResult queryTopicsFulltext(String query, String typeUri, boolean searchChildTopics);

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
     * Retrieves a single association by type and exact value. Throws if more than one association is found.
     * Lucene query syntax (wildcards, phrase search, ...) is not supported.
     * A text search is case-sensitive.
     *
     * @param   typeUri     an association type URI; only associations of this type are searched; mandatory
     * @param   value       the value to search for
     *
     * @return  the found association, or <code>null</code> if no association is found.
     *
     * @throws  RuntimeException    If more than one association is found.
     * @throws  RuntimeException    If null is given for "typeUri".
     */
    Assoc getAssocByValue(String typeUri, SimpleValue value);

    /**
     * Retrieves associations by type and value.
     * Lucene query syntax is supported.
     * An association is regarded a hit if the search term matches the association's entire value (in contrast to a
     * fulltext search). Spaces must be escaped though.
     * Search is case-sensitive.
     *
     * @param   typeUri     an association type URI; only associations of this type are searched; mandatory
     * @param   query       The query. Must be non-empty. Lucene query syntax is supported:
     *      <ul>
     *          <li>"*" matches arbitrary characters</li>
     *          <li>"?" matches a single character</li>
     *          <li>phrases enclosed in double quotes (")</li>
     *          <li>escape special character by preceding with back slash (\)</li>
     *          <li>combining search terms by "AND"/"OR" (uppercase required). Default is "OR". "&&" and "||" are
     *              respective synonyms.</li>
     *      </ul>
     *
     * @return  a list of found associations, may be empty.
     *
     * @throws  RuntimeException    If null is given for "typeUri".
     */
    List<Assoc> queryAssocs(String typeUri, String query);

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

    RelatedTopicResult query(String topicQuery, String topicTypeUri, boolean searchTopicChildren,
                             String assocQuery, String assocTypeUri, boolean searchAssocChildren);



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

    WebSocketService getWebSocketService();

    Object getDatabaseVendorObject();
}
