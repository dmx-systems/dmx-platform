package de.deepamehta.core.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;



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

    Topic getTopic(long id, boolean fetchComposite);

    /**
     * Looks up a single topic by exact value.
     * If no such topic exists <code>null</code> is returned.
     * If more than one topic is found a runtime exception is thrown.
     * <p>
     * IMPORTANT: Looking up a topic this way requires the corresponding type to be indexed with indexing mode
     * <code>dm4.core.key</code>.
     */
    Topic getTopic(String key, SimpleValue value, boolean fetchComposite);

    /**
     * Looks up topics by key and value. String values can contain wildcards like "*".
     * <p>
     * IMPORTANT: Looking up topics this way requires the corresponding type to be indexed with indexing mode
     * <code>dm4.core.key</code>.
     */
    Set<Topic> getTopics(String key, SimpleValue value, boolean fetchComposite);

    ResultSet<RelatedTopic> getTopics(String topicTypeUri, boolean fetchComposite, int maxResultSize);

    /**
     * Performs a fulltext search.
     * <p>
     * IMPORTANT: Searching topics this way requires the corresponding type to be indexed with indexing mode
     * <code>dm4.core.fulltext</code> or <code>dm4.core.fulltext_key</code>.
     *
     * @param   fieldUri    The URI of the data field to search. If null is provided all fields are searched. ### FIXDOC
     */
    Set<Topic> searchTopics(String searchTerm, String fieldUri);

    Iterable<Topic> getAllTopics();

    // ---

    Topic createTopic(TopicModel model, ClientState clientState);

    Directives updateTopic(TopicModel model, ClientState clientState);

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

    Set<RelatedAssociation> getAssociations(String assocTypeUri);

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

    Iterable<Association> getAllAssociations();

    // ---

    Association createAssociation(AssociationModel model, ClientState clientState);

    Directives updateAssociation(AssociationModel model, ClientState clientState);

    Directives deleteAssociation(long assocId);



    // === Topic Types ===

    Set<String> getTopicTypeUris();

    TopicType getTopicType(String topicTypeUri);

    Set<TopicType> getAllTopicTypes();

    TopicType createTopicType(TopicTypeModel model, ClientState clientState);

    Directives updateTopicType(TopicTypeModel model, ClientState clientState);



    // === Association Types ===

    Set<String> getAssociationTypeUris();

    AssociationType getAssociationType(String assocTypeUri);

    Set<AssociationType> getAllAssociationTypes();

    AssociationType createAssociationType(AssociationTypeModel model, ClientState clientState);

    Directives updateAssociationType(AssociationTypeModel model, ClientState clientState);



    // === Plugins ===

    Plugin getPlugin(String pluginUri);

    Set<PluginInfo> getPluginInfo();



    // === Properties ===

    Collection<Topic> getTopicsByProperty(String propUri, Object propValue);

    Collection<Topic> getTopicsByPropertyRange(String propUri, Number from, Number to);

    Collection<Association> getAssociationsByProperty(String propUri, Object propValue);

    Collection<Association> getAssociationsByPropertyRange(String propUri, Number from, Number to);



    // === Misc ===

    DeepaMehtaTransaction beginTx();

    TypeStorage getTypeStorage();
}
