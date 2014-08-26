package de.deepamehta.core.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;

import java.util.List;



/**
 * Specification of the DeepaMehta core service -- the heart of DeepaMehta.
 * <p>
 * The responsibility of the DeepaMehta core service is to orchestrate the control flow and allow plugins to hook in.
 * The main duties of the DeepaMehta core service are to provide access to the storage layer and to deliver events to
 * the installed plugins. ### FIXDOC
 * <p>
 * The DeepaMehta core service is a realization of the <i>Inversion of Control</i> pattern.
 * <p>
 * The DeepaMehta core service provides methods to deal with topics, associations, types, and plugins.
 * <p>
 * Plugin developer notes: Inside the {@link PluginActivator} and {@link Migration} classes an instance of the
 * DeepaMehta core service is available through the <code>dms</code> object.
 */
public interface DeepaMehtaService {



    // === Topics ===

    Topic getTopic(long topicId, boolean fetchComposite);

    /**
     * Looks up a single topic by exact value.
     * If no such topic exists <code>null</code> is returned.
     * If more than one topic is found a runtime exception is thrown.
     * <p>
     * Note: wildcards like "*" in String values are treated literally. They are <i>not</i> interpreted.
     * Compare to {@link #getTopics(String,SimpleValue,boolean)}
     * <p>
     * IMPORTANT: Looking up a topic this way requires the corresponding type to be indexed with indexing mode
     * <code>dm4.core.key</code>.
     */
    Topic getTopic(String key, SimpleValue value, boolean fetchComposite);

    /**
     * Looks up topics by key and value.
     * <p>
     * Wildcards like "*" in String values <i>are</i> interpreted.
     * <p>
     * IMPORTANT: Looking up topics this way requires the corresponding type to be indexed with indexing mode
     * <code>dm4.core.key</code>.
     */
    List<Topic> getTopics(String key, SimpleValue value, boolean fetchComposite);

    ResultList<RelatedTopic> getTopics(String topicTypeUri, boolean fetchComposite, int maxResultSize);

    /**
     * Performs a fulltext search.
     * <p>
     * IMPORTANT: Searching topics this way requires the corresponding type to be indexed with indexing mode
     * <code>dm4.core.fulltext</code> or <code>dm4.core.fulltext_key</code>. ### FIXDOC
     *
     * @param   fieldUri    The URI of the data field to search. If null is provided all fields are searched. ### FIXDOC
     *                      ### TODO: rename parameter to "key"?
     */
    List<Topic> searchTopics(String searchTerm, String fieldUri);

    Iterable<Topic> getAllTopics();

    // ---

    Topic createTopic(TopicModel model);

    Directives updateTopic(TopicModel model);

    Directives deleteTopic(long topicId);



    // === Associations ===

    Association getAssociation(long assocId, boolean fetchComposite);

    /**
     * Returns the association between two topics, qualified by association type and both role types.
     * If no such association exists <code>null</code> is returned.
     * If more than one association exist, a runtime exception is thrown.
     *
     * @param   assocTypeUri    Association type filter. Pass <code>null</code> to switch filter off.
     */
    Association getAssociation(String assocTypeUri, long topic1Id, long topic2Id,
                                                    String roleTypeUri1, String roleTypeUri2,
                                                    boolean fetchComposite);

    Association getAssociationBetweenTopicAndAssociation(String assocTypeUri, long topicId, long assocId,
                                                    String topicRoleTypeUri, String assocRoleTypeUri,
                                                    boolean fetchComposite);

    // ---

    List<RelatedAssociation> getAssociations(String assocTypeUri);

    /**
     * Returns all associations between two topics. If no such association exists an empty set is returned.
     */
    List<Association> getAssociations(long topic1Id, long topic2Id);

    /**
     * Returns the associations between two topics. If no such association exists an empty set is returned.
     *
     * @param   assocTypeUri    Association type filter. Pass <code>null</code> to switch filter off.
     */
    List<Association> getAssociations(long topic1Id, long topic2Id, String assocTypeUri);

    // ---

    Iterable<Association> getAllAssociations();

    // ---

    Association createAssociation(AssociationModel model);

    Directives updateAssociation(AssociationModel model);

    Directives deleteAssociation(long assocId);



    // === Topic Types ===

    List<String> getTopicTypeUris();

    TopicType getTopicType(String topicTypeUri);

    List<TopicType> getAllTopicTypes();

    // ---

    TopicType createTopicType(TopicTypeModel model);

    Directives updateTopicType(TopicTypeModel model);

    Directives deleteTopicType(String topicTypeUri);



    // === Association Types ===

    List<String> getAssociationTypeUris();

    AssociationType getAssociationType(String assocTypeUri);

    List<AssociationType> getAllAssociationTypes();

    // ---

    AssociationType createAssociationType(AssociationTypeModel model);

    Directives updateAssociationType(AssociationTypeModel model);

    Directives deleteAssociationType(String assocTypeUri);



    // === Plugins ===

    Plugin getPlugin(String pluginUri);

    List<PluginInfo> getPluginInfo();



    // === Events ===

    void fireEvent(DeepaMehtaEvent event, Object... params);

    void deliverEvent(String pluginUri, DeepaMehtaEvent event, Object... params);



    // === Properties ===

    List<Topic> getTopicsByProperty(String propUri, Object propValue);

    List<Topic> getTopicsByPropertyRange(String propUri, Number from, Number to);

    List<Association> getAssociationsByProperty(String propUri, Object propValue);

    List<Association> getAssociationsByPropertyRange(String propUri, Number from, Number to);



    // === Misc ===

    DeepaMehtaTransaction beginTx();

    TypeStorage getTypeStorage();

    Object getDatabaseVendorObject();
}
