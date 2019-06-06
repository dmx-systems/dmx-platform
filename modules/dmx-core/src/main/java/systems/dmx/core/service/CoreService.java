package systems.dmx.core.service;

import systems.dmx.core.Assoc;
import systems.dmx.core.AssocType;
import systems.dmx.core.DMXObject;
import systems.dmx.core.RelatedAssociation;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.AssocTypeModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicTypeModel;
import systems.dmx.core.service.accesscontrol.AccessControl;
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

    /**
     * Looks up a single topic by exact value.
     * <p>
     * Note: wildcards like "*" in String values are <i>not</i> interpreted. They are treated literally.
     * Compare to {@link #getTopicsByValue(String,SimpleValue)}
     * <p>
     * IMPORTANT: Looking up a topic this way requires the corresponding type to be indexed with indexing mode
     * <code>dmx.core.key</code>.
     *
     * @return  the topic, or <code>null</code> if no such topic exists.
     *
     * @throws  RuntimeException    If more than one topic is found.
     */
    Topic getTopicByValue(String key, SimpleValue value);

    /**
     * Looks up topics by key and value.
     * <p>
     * Wildcards like "*" in String values are interpreted.
     * <p>
     * IMPORTANT: Looking up topics this way requires the corresponding type to be indexed with indexing mode
     * <code>dmx.core.key</code>.
     */
    List<Topic> getTopicsByValue(String key, SimpleValue value);

    List<Topic> getTopicsByType(String topicTypeUri);

    /**
     * Performs a fulltext search.
     * <p>
     * IMPORTANT: Searching topics this way requires the corresponding type to be indexed with indexing mode
     * <code>dmx.core.fulltext</code> or <code>dmx.core.fulltext_key</code>. ### FIXDOC
     *
     * @param   fieldUri    The URI of the data field to search. If null is provided all fields are searched. ### FIXDOC
     *                      ### TODO: rename parameter to "key"/"typeUri"?
     */
    List<Topic> searchTopics(String searchTerm, String fieldUri);

    Iterable<Topic> getAllTopics();

    // ---

    Topic createTopic(TopicModel model);

    void updateTopic(TopicModel updateModel);

    void deleteTopic(long topicId);



    // === Associations ===

    Assoc getAssociation(long assocId);

    /**
     * Looks up a single association by exact value.
     * <p>
     * Note: wildcards like "*" in String values are <i>not</i> interpreted. They are treated literally.
     * Compare to {@link #getAssociationsByValue(String,SimpleValue)}
     * <p>
     * IMPORTANT: Looking up an association this way requires the corresponding type to be indexed with indexing mode
     * <code>dmx.core.key</code>.
     *
     * @return  the association, or <code>null</code> if no such association exists.
     *
     * @throws  RuntimeException    If more than one association is found.
     */
    Assoc getAssociationByValue(String key, SimpleValue value);

    /**
     * Looks up associations by key and value.
     * <p>
     * Wildcards like "*" in String values <i>are</i> interpreted.
     * <p>
     * IMPORTANT: Looking up associations this way requires the corresponding type to be indexed with indexing mode
     * <code>dmx.core.key</code>.
     */
    List<Assoc> getAssociationsByValue(String key, SimpleValue value);

    /**
     * Returns the association between two topics, qualified by association type and both role types.
     * If no such association exists <code>null</code> is returned.
     * If more than one association exist, a runtime exception is thrown.
     *
     * @param   assocTypeUri    Assoc type filter. Pass <code>null</code> to switch filter off.
     */
    Assoc getAssociation(String assocTypeUri, long topic1Id, long topic2Id, String roleTypeUri1, String roleTypeUri2);

    Assoc getAssociationBetweenTopicAndAssociation(String assocTypeUri, long topicId, long assocId,
                                                   String topicRoleTypeUri, String assocRoleTypeUri);

    // ---

    List<Assoc> getAssociationsByType(String assocTypeUri);

    /**
     * Returns all associations between two topics. If no such association exists an empty list is returned.
     */
    List<Assoc> getAssociations(long topic1Id, long topic2Id);

    /**
     * Returns the associations between two topics. If no such association exists an empty list is returned.
     *
     * @param   assocTypeUri    Assoc type filter. Pass <code>null</code> to switch filter off.
     */
    List<Assoc> getAssociations(long topic1Id, long topic2Id, String assocTypeUri);

    // ---

    Iterable<Assoc> getAllAssociations();

    List<PlayerModel> getRoleModels(long assocId);

    // ---

    Assoc createAssociation(AssocModel model);

    void updateAssociation(AssocModel updateModel);

    void deleteAssociation(long assocId);



    // === Topic Types ===

    TopicType getTopicType(String topicTypeUri);

    /**
     * Acccesses a topic type while enforcing the <i>implicit READ permission</i>.
     * A user has implicit READ permission for the topic type if she has READ permission for the given topic.
     */
    TopicType getTopicTypeImplicitly(long topicId);

    // ---

    List<TopicType> getAllTopicTypes();

    // ---

    TopicType createTopicType(TopicTypeModel model);

    void updateTopicType(TopicTypeModel updateModel);

    void deleteTopicType(String topicTypeUri);



    // === Assoc Types ===

    AssocType getAssociationType(String assocTypeUri);

    /**
     * Acccesses an association type while enforcing the <i>implicit READ permission</i>.
     * A user has implicit READ permission for the association type if she has READ permission for the given
     * association.
     */
    AssocType getAssociationTypeImplicitly(long assocId);

    // ---

    List<AssocType> getAllAssociationTypes();

    // ---

    AssocType createAssociationType(AssocTypeModel model);

    void updateAssociationType(AssocTypeModel updateModel);

    void deleteAssociationType(String assocTypeUri);



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

    List<Assoc> getAssociationsByProperty(String propUri, Object propValue);

    List<Assoc> getAssociationsByPropertyRange(String propUri, Number from, Number to);

    // ---

    void addTopicPropertyIndex(String propUri);

    void addAssociationPropertyIndex(String propUri);



    // === Misc ===

    DMXTransaction beginTx();

    // ---

    ModelFactory getModelFactory();

    AccessControl getAccessControl();   // ### TODO: drop this

    WebSocketsService getWebSocketsService();

    Object getDatabaseVendorObject();
}
