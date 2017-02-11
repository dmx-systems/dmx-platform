package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ModelFactory;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
import de.deepamehta.core.storage.spi.DeepaMehtaStorage;

import static java.util.Arrays.asList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



// ### TODO: should methods return model *impl* objects? -> Yes!
class StorageDecorator {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String PROP_CORE_MODEL_VERSION = "core_migration_nr";  // ### TODO: -> "dm4.core.model_version"

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DeepaMehtaStorage storage;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    StorageDecorator(DeepaMehtaStorage storage) {
        this.storage = storage;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Topics ===

    /**
     * @return  The fetched topic.
     *          Note: its child topics are not fetched.
     */
    final TopicModelImpl fetchTopic(long topicId) {
        return (TopicModelImpl) storage.fetchTopic(topicId);
    }

    /**
     * Looks up a single topic by exact value.
     * <p>
     * IMPORTANT: Looking up a topic this way requires the corresponding type to be indexed with indexing mode
     * <code>dm4.core.key</code>.
     *
     * @return  The fetched topic, or <code>null</code> if no such topic exists.
     *          Note: its child topics are not fetched.
     *
     * @throws  RuntimeException    if more than one topic is found.
     */
    final TopicModelImpl fetchTopic(String key, SimpleValue value) {
        return (TopicModelImpl) storage.fetchTopic(key, value.value());
    }

    final List<TopicModelImpl> fetchTopics(String key, SimpleValue value) {
        return (List<TopicModelImpl>) storage.fetchTopics(key, value.value());
    }

    // ---

    /**
     * @return  The fetched topics.
     *          Note: their child topics are not fetched.
     */
    final List<TopicModelImpl> queryTopics(String key, SimpleValue value) {
        return (List<TopicModelImpl>) storage.queryTopics(key, value.value());
    }

    // ---

    final Iterator<TopicModelImpl> fetchAllTopics() {
        return (Iterator<TopicModelImpl>) storage.fetchAllTopics();
    }

    // ---

    /**
     * Creates a topic.
     * <p>
     * Actually only the topic URI and type URI are stored and indexed.
     * The topic value is not stored.
     *
     * @return  FIXDOC ### the created topic. Note:
     *          - the topic URI   is initialzed and     persisted.
     *          - the topic value is initialzed but not persisted.
     *          - the type URI    is initialzed but not persisted.
     */
    final void storeTopic(TopicModel model) {
        storage.storeTopic(model);
    }

    /**
     * Stores and indexes the topic's URI.
     */
    final void storeTopicUri(long topicId, String uri) {
        storage.storeTopicUri(topicId, uri);
    }

    final void storeTopicTypeUri(long topicId, String topicTypeUri) {
        storage.storeTopicTypeUri(topicId, topicTypeUri);
    }

    // ---

    /**
     * Convenience method (no indexing).
     */
    final void storeTopicValue(long topicId, SimpleValue value) {
        storeTopicValue(topicId, value, asList(IndexMode.OFF), null, null);
    }

    /**
     * Stores and indexes the topic's value. ### TODO: separate storing/indexing?
     */
    final void storeTopicValue(long topicId, SimpleValue value, List<IndexMode> indexModes, String indexKey,
                                                                                            SimpleValue indexValue) {
        storage.storeTopicValue(topicId, value, indexModes, indexKey, indexValue);
    }

    final void indexTopicValue(long topicId, IndexMode indexMode, String indexKey, SimpleValue indexValue) {
        storage.indexTopicValue(topicId, indexMode, indexKey, indexValue);
    }

    // ---

    /**
     * Deletes the topic.
     * <p>
     * Prerequisite: the topic has no relations.
     */
    final void _deleteTopic(long topicId) {
        storage.deleteTopic(topicId);
    }



    // === Associations ===

    final AssociationModelImpl fetchAssociation(long assocId) {
        return (AssociationModelImpl) storage.fetchAssociation(assocId);
    }

    /**
     * Looks up a single association by exact value.
     * If no such association exists <code>null</code> is returned.
     * If more than one association is found a runtime exception is thrown.
     * <p>
     * IMPORTANT: Looking up an association this way requires the corresponding type to be indexed with indexing mode
     * <code>dm4.core.key</code>.
     *
     * @return  The fetched association.
     *          Note: its child topics are not fetched.
     */
    final AssociationModelImpl fetchAssociation(String key, SimpleValue value) {
        return (AssociationModelImpl) storage.fetchAssociation(key, value.value());
    }

    final List<AssociationModelImpl> fetchAssociations(String key, SimpleValue value) {
        return (List<AssociationModelImpl>) storage.fetchAssociations(key, value.value());
    }

    // ---

    /**
     * Convenience method (checks singularity).
     *
     * Returns the association between two topics, qualified by association type and both role types.
     * If no such association exists <code>null</code> is returned.
     * If more than one association exist, a runtime exception is thrown.
     *
     * @param   assocTypeUri    Association type filter. Pass <code>null</code> to switch filter off.
     *                          ### FIXME: for methods with a singular return value all filters should be mandatory
     */
    final AssociationModelImpl fetchAssociation(String assocTypeUri, long topicId1, long topicId2,
                                                                     String roleTypeUri1, String roleTypeUri2) {
        List<AssociationModelImpl> assocs = fetchAssociations(assocTypeUri, topicId1, topicId2, roleTypeUri1,
            roleTypeUri2);
        switch (assocs.size()) {
        case 0:
            return null;
        case 1:
            return assocs.get(0);
        default:
            throw new RuntimeException("Ambiguity: there are " + assocs.size() + " \"" + assocTypeUri +
                "\" associations (topicId1=" + topicId1 + ", topicId2=" + topicId2 + ", " +
                "roleTypeUri1=\"" + roleTypeUri1 + "\", roleTypeUri2=\"" + roleTypeUri2 + "\")");
        }
    }

    /**
     * Returns the associations between two topics. If no such association exists an empty set is returned.
     *
     * @param   assocTypeUri    Association type filter. Pass <code>null</code> to switch filter off.
     */
    final List<AssociationModelImpl> fetchAssociations(String assocTypeUri, long topicId1, long topicId2,
                                                                            String roleTypeUri1, String roleTypeUri2) {
        return (List<AssociationModelImpl>) storage.fetchAssociations(assocTypeUri, topicId1, topicId2, roleTypeUri1,
            roleTypeUri2);
    }

    // ---

    /**
     * Convenience method (checks singularity).
     */
    final AssociationModelImpl fetchAssociationBetweenTopicAndAssociation(String assocTypeUri, long topicId,
                                                       long assocId, String topicRoleTypeUri, String assocRoleTypeUri) {
        List<AssociationModelImpl> assocs = fetchAssociationsBetweenTopicAndAssociation(assocTypeUri, topicId, assocId,
            topicRoleTypeUri, assocRoleTypeUri);
        switch (assocs.size()) {
        case 0:
            return null;
        case 1:
            return assocs.get(0);
        default:
            throw new RuntimeException("Ambiguity: there are " + assocs.size() + " \"" + assocTypeUri +
                "\" associations (topicId=" + topicId + ", assocId=" + assocId + ", " +
                "topicRoleTypeUri=\"" + topicRoleTypeUri + "\", assocRoleTypeUri=\"" + assocRoleTypeUri + "\")");
        }
    }

    final List<AssociationModelImpl> fetchAssociationsBetweenTopicAndAssociation(String assocTypeUri, long topicId,
                                                       long assocId, String topicRoleTypeUri, String assocRoleTypeUri) {
        return (List<AssociationModelImpl>) storage.fetchAssociationsBetweenTopicAndAssociation(assocTypeUri, topicId,
            assocId, topicRoleTypeUri, assocRoleTypeUri);
    }

    // ---

    final Iterator<AssociationModelImpl> fetchAllAssociations() {
        return (Iterator<AssociationModelImpl>) storage.fetchAllAssociations();
    }

    final long[] fetchPlayerIds(long assocId) {
        return storage.fetchPlayerIds(assocId);
    }

    // ---

    /**
     * Stores and indexes the association's URI.
     */
    final void storeAssociationUri(long assocId, String uri) {
        storage.storeAssociationUri(assocId, uri);
    }

    final void storeAssociationTypeUri(long assocId, String assocTypeUri) {
        storage.storeAssociationTypeUri(assocId, assocTypeUri);
    }

    final void storeRoleTypeUri(long assocId, long playerId, String roleTypeUri) {
        storage.storeRoleTypeUri(assocId, playerId, roleTypeUri);
    }

    // ---

    /**
     * Convenience method (no indexing).
     */
    final void storeAssociationValue(long assocId, SimpleValue value) {
        storeAssociationValue(assocId, value, asList(IndexMode.OFF), null, null);
    }

    /**
     * Stores and indexes the association's value. ### TODO: separate storing/indexing?
     */
    final void storeAssociationValue(long assocId, SimpleValue value, List<IndexMode> indexModes, String indexKey,
                                                                                               SimpleValue indexValue) {
        storage.storeAssociationValue(assocId, value, indexModes, indexKey, indexValue);
    }

    final void indexAssociationValue(long assocId, IndexMode indexMode, String indexKey, SimpleValue indexValue) {
        storage.indexAssociationValue(assocId, indexMode, indexKey, indexValue);
    }

    // ---

    final void storeAssociation(AssociationModel model) {
        storage.storeAssociation(model);
    }

    final void _deleteAssociation(long assocId) {
        storage.deleteAssociation(assocId);
    }



    // === Generic Object ===

    final DeepaMehtaObjectModelImpl fetchObject(long id) {
        return (DeepaMehtaObjectModelImpl) storage.fetchObject(id);
    }



    // === Traversal ===

    /**
     * @return  The fetched associations.
     *          Note: their child topics are not fetched.
     */
    final List<AssociationModelImpl> fetchTopicAssociations(long topicId) {
        return (List<AssociationModelImpl>) storage.fetchTopicAssociations(topicId);
    }

    final List<AssociationModelImpl> fetchAssociationAssociations(long assocId) {
        return (List<AssociationModelImpl>) storage.fetchAssociationAssociations(assocId);
    }

    // ---

    /**
     * Convenience method (checks singularity).
     *
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     *
     * @return  The fetched topic.
     *          Note: its child topics are not fetched.
     */
    final RelatedTopicModelImpl fetchTopicRelatedTopic(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                       String othersRoleTypeUri, String othersTopicTypeUri) {
        List<RelatedTopicModelImpl> topics = fetchTopicRelatedTopics(topicId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
        switch (topics.size()) {
        case 0:
            return null;
        case 1:
            return topics.iterator().next();
        default:
            throw new RuntimeException("Ambiguity: there are " + topics.size() + " related topics (topicId=" +
                topicId + ", assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri + "\", " +
                "othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersTopicTypeUri=\"" + othersTopicTypeUri + "\")");
        }
    }

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     *
     * @return  The fetched topics.
     *          Note: their child topics are not fetched.
     */
    final List<RelatedTopicModelImpl> fetchTopicRelatedTopics(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                              String othersRoleTypeUri, String othersTopicTypeUri) {
        return (List<RelatedTopicModelImpl>) storage.fetchTopicRelatedTopics(topicId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
    }

    /**
     * Convenience method (receives *list* of association types).
     *
     * @param   assocTypeUris       may *not* be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     *
     * @return  The fetched topics.
     *          Note: their child topics are not fetched.
     */
    final List<RelatedTopicModelImpl> fetchTopicRelatedTopics(long topicId, List<String> assocTypeUris,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersTopicTypeUri) {
        List<RelatedTopicModelImpl> result = new ArrayList();
        for (String assocTypeUri : assocTypeUris) {
            result.addAll(fetchTopicRelatedTopics(topicId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
                othersTopicTypeUri));
        }
        return result;
    }

    // ---

    /**
     * Convenience method (checks singularity).
     *
     * @return  The fetched association.
     *          Note: its child topics are not fetched.
     */
    final RelatedAssociationModelImpl fetchTopicRelatedAssociation(long topicId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        List<RelatedAssociationModelImpl> assocs = fetchTopicRelatedAssociations(topicId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersAssocTypeUri);
        switch (assocs.size()) {
        case 0:
            return null;
        case 1:
            return assocs.iterator().next();
        default:
            throw new RuntimeException("Ambiguity: there are " + assocs.size() + " related associations (topicId=" +
                topicId + ", assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri + "\", " +
                "othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersAssocTypeUri=\"" + othersAssocTypeUri + "\")");
        }
    }

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersAssocTypeUri  may be null
     *
     * @return  The fetched associations.
     *          Note: their child topics are not fetched.
     */
    final List<RelatedAssociationModelImpl> fetchTopicRelatedAssociations(long topicId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        return (List<RelatedAssociationModelImpl>) storage.fetchTopicRelatedAssociations(topicId, assocTypeUri,
            myRoleTypeUri, othersRoleTypeUri, othersAssocTypeUri);
    }

    // ---

    /**
     * Convenience method (checks singularity).
     *
     * @return  The fetched topic.
     *          Note: its child topics are not fetched.
     */
    final RelatedTopicModelImpl fetchAssociationRelatedTopic(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                             String othersRoleTypeUri, String othersTopicTypeUri) {
        List<RelatedTopicModelImpl> topics = fetchAssociationRelatedTopics(assocId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
        switch (topics.size()) {
        case 0:
            return null;
        case 1:
            return topics.iterator().next();
        default:
            throw new RuntimeException("Ambiguity: there are " + topics.size() + " related topics (assocId=" +
                assocId + ", assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri + "\", " +
                "othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersTopicTypeUri=\"" + othersTopicTypeUri + "\")");
        }
    }

    /**
     * @return  The fetched topics.
     *          Note: their child topics are not fetched.
     */
    final List<RelatedTopicModelImpl> fetchAssociationRelatedTopics(long assocId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersTopicTypeUri) {
        return (List<RelatedTopicModelImpl>) storage.fetchAssociationRelatedTopics(assocId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
    }

    /**
     * Convenience method (receives *list* of association types).
     *
     * @param   assocTypeUris       may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     *
     * @return  The fetched topics.
     *          Note: their child topics are not fetched.
     */
    final List<RelatedTopicModelImpl> fetchAssociationRelatedTopics(long assocId, List<String> assocTypeUris,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersTopicTypeUri) {
        List<RelatedTopicModelImpl> result = new ArrayList();
        for (String assocTypeUri : assocTypeUris) {
            result.addAll(fetchAssociationRelatedTopics(assocId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
                othersTopicTypeUri));
        }
        return result;
    }

    // ---

    /**
     * Convenience method (checks singularity).
     *
     * @return  The fetched association.
     *          Note: its child topics are not fetched.
     */
    final RelatedAssociationModelImpl fetchAssociationRelatedAssociation(long assocId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        List<RelatedAssociationModelImpl> assocs = fetchAssociationRelatedAssociations(assocId, assocTypeUri,
            myRoleTypeUri, othersRoleTypeUri, othersAssocTypeUri);
        switch (assocs.size()) {
        case 0:
            return null;
        case 1:
            return assocs.iterator().next();
        default:
            throw new RuntimeException("Ambiguity: there are " + assocs.size() + " related associations (assocId=" +
                assocId + ", assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri + "\", " +
                "othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersAssocTypeUri=\"" + othersAssocTypeUri +
                "\"),\nresult=" + assocs);
        }
    }

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersAssocTypeUri  may be null
     *
     * @return  The fetched associations.
     *          Note: their child topics are not fetched.
     */
    final List<RelatedAssociationModelImpl> fetchAssociationRelatedAssociations(long assocId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        return (List<RelatedAssociationModelImpl>) storage.fetchAssociationRelatedAssociations(assocId, assocTypeUri,
            myRoleTypeUri, othersRoleTypeUri, othersAssocTypeUri);
    }

    // ---

    /**
     * Convenience method (checks singularity).
     *
     * @param   objectId            id of a topic or an association
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     *
     * @return  The fetched topic.
     *          Note: its child topics are not fetched.
     */
    final RelatedTopicModelImpl fetchRelatedTopic(long objectId, String assocTypeUri, String myRoleTypeUri,
                                                  String othersRoleTypeUri, String othersTopicTypeUri) {
        List<RelatedTopicModelImpl> topics = fetchRelatedTopics(objectId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
        switch (topics.size()) {
        case 0:
            return null;
        case 1:
            return topics.iterator().next();
        default:
            throw new RuntimeException("Ambiguity: there are " + topics.size() + " related topics (objectId=" +
                objectId + ", assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri + "\", " +
                "othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersTopicTypeUri=\"" + othersTopicTypeUri + "\")");
        }
    }

    /**
     * @param   objectId            id of a topic or an association
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     *
     * @return  The fetched topics.
     *          Note: their child topics are not fetched.
     */
    final List<RelatedTopicModelImpl> fetchRelatedTopics(long objectId, String assocTypeUri, String myRoleTypeUri,
                                                         String othersRoleTypeUri, String othersTopicTypeUri) {
        return (List<RelatedTopicModelImpl>) storage.fetchRelatedTopics(objectId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
    }

    // ### TODO: decorator for fetchRelatedAssociations()



    // === Properties ===

    final Object fetchProperty(long id, String propUri) {
        return storage.fetchProperty(id, propUri);
    }

    final boolean hasProperty(long id, String propUri) {
        return storage.hasProperty(id, propUri);
    }

    // ---

    final List<TopicModelImpl> fetchTopicsByProperty(String propUri, Object propValue) {
        return (List<TopicModelImpl>) storage.fetchTopicsByProperty(propUri, propValue);
    }

    final List<TopicModelImpl> fetchTopicsByPropertyRange(String propUri, Number from, Number to) {
        return (List<TopicModelImpl>) storage.fetchTopicsByPropertyRange(propUri, from, to);
    }

    final List<AssociationModelImpl> fetchAssociationsByProperty(String propUri, Object propValue) {
        return (List<AssociationModelImpl>) storage.fetchAssociationsByProperty(propUri, propValue);
    }

    final List<AssociationModelImpl> fetchAssociationsByPropertyRange(String propUri, Number from, Number to) {
        return (List<AssociationModelImpl>) storage.fetchAssociationsByPropertyRange(propUri, from, to);
    }

    // ---

    final void storeTopicProperty(long topicId, String propUri, Object propValue, boolean addToIndex) {
        storage.storeTopicProperty(topicId, propUri, propValue, addToIndex);
    }

    final void storeAssociationProperty(long assocId, String propUri, Object propValue, boolean addToIndex) {
        storage.storeAssociationProperty(assocId, propUri, propValue, addToIndex);
    }

    // ---

    final void indexTopicProperty(long topicId, String propUri, Object propValue) {
        storage.indexTopicProperty(topicId, propUri, propValue);
    }

    final void indexAssociationProperty(long assocId, String propUri, Object propValue) {
        storage.indexAssociationProperty(assocId, propUri, propValue);
    }

    // ---

    final void removeTopicProperty(long topicId, String propUri) {
        storage.deleteTopicProperty(topicId, propUri);
    }

    final void removeAssociationProperty(long assocId, String propUri) {
        storage.deleteAssociationProperty(assocId, propUri);
    }



    // === DB ===

    final DeepaMehtaTransaction beginTx() {
        return storage.beginTx();
    }

    /**
     * Initializes the database.
     * Prerequisite: there is an open transaction.
     *
     * @return  <code>true</code> if a clean install is detected, <code>false</code> otherwise.
     */
    final boolean init() {
        boolean isCleanInstall = storage.init();
        if (isCleanInstall) {
            logger.info("Clean install detected -- Starting with a fresh DB");
            createRootTopic();
            storeCoreModelVersion(0);
        }
        return isCleanInstall;
    }

    final void shutdown() {
        storage.shutdown();
    }

    // ---

    final int fetchCoreModelVersion() {
        // ### FIXME: ID 0
        return (Integer) fetchProperty(0, PROP_CORE_MODEL_VERSION);
    }

    final void storeCoreModelVersion(int version) {
        // ### FIXME: ID 0
        storeTopicProperty(0, PROP_CORE_MODEL_VERSION, version, false);     // addToIndex=false
    }

    // ---

    final Object getDatabaseVendorObject() {
        return storage.getDatabaseVendorObject();
    }

    final Object getDatabaseVendorObject(long objectId) {
        return storage.getDatabaseVendorObject(objectId);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void createRootTopic() {
        storeTopic(storage.getModelFactory().newTopicModel(
            "dm4.core.meta_type",
            "dm4.core.meta_meta_type"
        ));
        // ### FIXME: set topic value
    }
}
