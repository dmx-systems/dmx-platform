package systems.dmx.core.impl;

import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.model.RoleModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.storage.spi.DMXTransaction;
import systems.dmx.core.storage.spi.DMXStorage;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



/**
 * A thin convenience layer above vendor specific storage.
 * 2 responsibilites:
 *  - Adapts public storage API to Core internal API (type casting).
 *  - Adds fetch-single calls on top of fetch-multiple calls and performs sanity checks.
 */
class StorageDecorator {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private final DMXStorage storage;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    StorageDecorator(DMXStorage storage) {
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

    final TopicModelImpl fetchTopicByUri(String uri) {
        return fetchTopic("uri", new SimpleValue(uri));
    }

    /**
     * Looks up a single topic by exact value.
     * <p>
     * IMPORTANT: Looking up a topic this way requires the corresponding type to be indexed with indexing mode
     * <code>dmx.core.key</code>.
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

    /**
     * Stores and indexes a topic value.
     */
    final void storeTopicValue(long topicId, SimpleValue value, String indexKey, boolean isHtmlValue) {
        storage.storeTopicValue(topicId, value, indexKey, isHtmlValue);
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
     * <code>dmx.core.key</code>.
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
     * @param   assocTypeUri    Assoc type filter. Pass <code>null</code> to switch filter off.
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
     * @param   assocTypeUri    Assoc type filter. Pass <code>null</code> to switch filter off.
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

    final List<RoleModel> fetchRoleModels(long assocId) {
        return storage.fetchRoleModels(assocId);
    }

    // ---

    final void storeAssociation(AssociationModel model) {
        storage.storeAssociation(model);
    }

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

    /**
     * Stores and indexes an association value.
     */
    final void storeAssociationValue(long assocId, SimpleValue value, String indexKey, boolean isHtmlValue) {
        storage.storeAssociationValue(assocId, value, indexKey, isHtmlValue);
    }

    // ---

    final void _deleteAssociation(long assocId) {
        storage.deleteAssociation(assocId);
    }



    // === Generic Object ===

    final DMXObjectModelImpl fetchObject(long id) {
        return (DMXObjectModelImpl) storage.fetchObject(id);
    }



    // === Traversal ===

    // --- Topic Source ---

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
     * @return  The fetched associations.
     *          Note: their child topics are not fetched.
     */
    final List<AssociationModelImpl> fetchTopicAssociations(long topicId) {
        return (List<AssociationModelImpl>) storage.fetchTopicAssociations(topicId);
    }

    // --- Assoc Source ---

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

    final List<AssociationModelImpl> fetchAssociationAssociations(long assocId) {
        return (List<AssociationModelImpl>) storage.fetchAssociationAssociations(assocId);
    }

    // --- Object Source ---

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

    final DMXTransaction beginTx() {
        return storage.beginTx();
    }

    /**
     * Initializes the database.
     * Prerequisite: there is an open transaction.
     *
     * @return  <code>true</code> if a clean install is detected, <code>false</code> otherwise.
     */
    final boolean init() {
        boolean isCleanInstall = storage.setupRootNode();
        if (isCleanInstall) {
            logger.info("Clean install detected -- Starting with a fresh DB");
            storeMigrationNr(0);
        }
        return isCleanInstall;
    }

    final void shutdown() {
        storage.shutdown();
    }

    // ---

    final int fetchMigrationNr() {
        return (Integer) fetchProperty(0, "core_migration_nr");
    }

    final void storeMigrationNr(int migrationNr) {
        storage.storeTopicProperty(0, "core_migration_nr", migrationNr, false);     // addToIndex=false
    }

    // ---

    final Object getDatabaseVendorObject() {
        return storage.getDatabaseVendorObject();
    }

    final Object getDatabaseVendorObject(long objectId) {
        return storage.getDatabaseVendorObject(objectId);
    }
}
