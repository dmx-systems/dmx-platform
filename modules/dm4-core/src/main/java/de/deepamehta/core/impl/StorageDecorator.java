package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
import de.deepamehta.core.storage.spi.DeepaMehtaStorage;

import static java.util.Arrays.asList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



public class StorageDecorator {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DeepaMehtaStorage storage;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public StorageDecorator(DeepaMehtaStorage storage) {
        this.storage = storage;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Topics ===

    /**
     * @return  The fetched topic.
     *          Note: its composite value is not initialized.
     */
    TopicModel fetchTopic(long topicId) {
        return storage.fetchTopic(topicId);
    }

    /**
     * Looks up a single topic by exact property value.
     * If no such topic exists <code>null</code> is returned.
     * If more than one topic were found a runtime exception is thrown.
     * <p>
     * IMPORTANT: Looking up a topic this way requires the property to be indexed with indexing mode <code>KEY</code>.
     * This is achieved by declaring the respective data field with <code>indexing_mode: "KEY"</code>
     * (for statically declared data field, typically in <code>types.json</code>) or
     * by calling DataField's {@link DataField#setIndexingMode} method with <code>"KEY"</code> as argument
     * (for dynamically created data fields, typically in migration classes).
     *
     * @return  The fetched topic.
     *          Note: its composite value is not initialized.
     */
    TopicModel fetchTopic(String key, SimpleValue value) {
        return storage.fetchTopic(key, value.value());
    }

    List<TopicModel> fetchTopics(String key, SimpleValue value) {
        return storage.fetchTopics(key, value.value());
    }

    // ---

    /**
     * @return  The fetched topics.
     *          Note: their composite values are not initialized.
     */
    List<TopicModel> queryTopics(String searchTerm, String fieldUri) {
        return storage.queryTopics(fieldUri, searchTerm);
    }

    // ---

    Iterator<TopicModel> fetchAllTopics() {
        return storage.fetchAllTopics();
    }

    // ---

    /**
     * Creates a topic.
     * <p>
     * The topic's URI is stored and indexed.
     *
     * @return  FIXDOC ### the created topic. Note:
     *          - the topic URI   is initialzed and     persisted.
     *          - the topic value is initialzed but not persisted.
     *          - the type URI    is initialzed but not persisted.
     */
    void storeTopic(TopicModel model) {
        storage.storeTopic(model);
    }

    /**
     * Stores and indexes the topic's URI.
     */
    void storeTopicUri(long topicId, String uri) {
        storage.storeTopicUri(topicId, uri);
    }

    void storeTopicTypeUri(long topicId, String topicTypeUri) {
        storage.storeTopicTypeUri(topicId, topicTypeUri);
    }

    // ---

    /**
     * Convenience method (no indexing).
     */
    void storeTopicValue(long topicId, SimpleValue value) {
        storeTopicValue(topicId, value, asList(IndexMode.OFF), null, null);
    }

    /**
     * Stores and indexes the topic's value. ### TODO: separate storing/indexing?
     */
    void storeTopicValue(long topicId, SimpleValue value, List<IndexMode> indexModes, String indexKey,
                                                                                      SimpleValue indexValue) {
        storage.storeTopicValue(topicId, value, indexModes, indexKey, indexValue);
    }

    void indexTopicValue(long topicId, IndexMode indexMode, String indexKey, SimpleValue indexValue) {
        storage.indexTopicValue(topicId, indexMode, indexKey, indexValue);
    }

    // ---

    /**
     * Deletes the topic.
     * <p>
     * Prerequisite: the topic has no relations.
     */
    void deleteTopic(long topicId) {
        storage.deleteTopic(topicId);
    }



    // === Associations ===

    AssociationModel fetchAssociation(long assocId) {
        return storage.fetchAssociation(assocId);
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
    AssociationModel fetchAssociation(String assocTypeUri, long topicId1, long topicId2, String roleTypeUri1,
                                                                                         String roleTypeUri2) {
        List<AssociationModel> assocs = fetchAssociations(assocTypeUri, topicId1, topicId2, roleTypeUri1, roleTypeUri2);
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
    List<AssociationModel> fetchAssociations(String assocTypeUri, long topicId1, long topicId2,
                                                                        String roleTypeUri1, String roleTypeUri2) {
        return storage.fetchAssociations(assocTypeUri, topicId1, topicId2, roleTypeUri1, roleTypeUri2);
    }

    // ---

    /**
     * Convenience method (checks singularity).
     */
    AssociationModel fetchAssociationBetweenTopicAndAssociation(String assocTypeUri, long topicId, long assocId,
                                                                     String topicRoleTypeUri, String assocRoleTypeUri) {
        List<AssociationModel> assocs = fetchAssociationsBetweenTopicAndAssociation(assocTypeUri, topicId, assocId,
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

    List<AssociationModel> fetchAssociationsBetweenTopicAndAssociation(String assocTypeUri, long topicId,
                                                       long assocId, String topicRoleTypeUri, String assocRoleTypeUri) {
        return storage.fetchAssociationsBetweenTopicAndAssociation(assocTypeUri, topicId, assocId, topicRoleTypeUri,
            assocRoleTypeUri);
    }

    // ---

    Iterator<AssociationModel> fetchAllAssociations() {
        return storage.fetchAllAssociations();
    }

    long[] fetchPlayerIds(long assocId) {
        return storage.fetchPlayerIds(assocId);
    }

    // ---

    /**
     * Stores and indexes the association's URI.
     */
    void storeAssociationUri(long assocId, String uri) {
        storage.storeAssociationUri(assocId, uri);
    }

    void storeAssociationTypeUri(long assocId, String assocTypeUri) {
        storage.storeAssociationTypeUri(assocId, assocTypeUri);
    }

    void storeRoleTypeUri(long assocId, long playerId, String roleTypeUri) {
        storage.storeRoleTypeUri(assocId, playerId, roleTypeUri);
    }

    // ---

    /**
     * Convenience method (no indexing).
     */
    void storeAssociationValue(long assocId, SimpleValue value) {
        storeAssociationValue(assocId, value, asList(IndexMode.OFF), null, null);
    }

    /**
     * Stores and indexes the association's value. ### TODO: separate storing/indexing?
     */
    void storeAssociationValue(long assocId, SimpleValue value, List<IndexMode> indexModes, String indexKey,
                                                                                            SimpleValue indexValue) {
        storage.storeAssociationValue(assocId, value, indexModes, indexKey, indexValue);
    }

    void indexAssociationValue(long assocId, IndexMode indexMode, String indexKey, SimpleValue indexValue) {
        storage.indexAssociationValue(assocId, indexMode, indexKey, indexValue);
    }

    // ---

    void storeAssociation(AssociationModel model) {
        storage.storeAssociation(model);
    }

    void deleteAssociation(long assocId) {
        storage.deleteAssociation(assocId);
    }



    // === Traversal ===

    /**
     * @return  The fetched associations.
     *          Note: their composite values are not initialized.
     */
    List<AssociationModel> fetchTopicAssociations(long topicId) {
        return storage.fetchTopicAssociations(topicId);
    }

    List<AssociationModel> fetchAssociationAssociations(long assocId) {
        return storage.fetchAssociationAssociations(assocId);
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
     * @return  The fetched topics.
     *          Note: their composite values are not initialized.
     */
    RelatedTopicModel fetchTopicRelatedTopic(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                           String othersRoleTypeUri, String othersTopicTypeUri) {
        ResultList<RelatedTopicModel> topics = fetchTopicRelatedTopics(topicId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri, 0);
        switch (topics.getSize()) {
        case 0:
            return null;
        case 1:
            return topics.iterator().next();
        default:
            throw new RuntimeException("Ambiguity: there are " + topics.getSize() + " related topics (topicId=" +
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
     *          Note: their composite values are not initialized.
     */
    ResultList<RelatedTopicModel> fetchTopicRelatedTopics(long topicId, String assocTypeUri,
                                                                String myRoleTypeUri, String othersRoleTypeUri,
                                                                String othersTopicTypeUri, int maxResultSize) {
        List<RelatedTopicModel> relTopics = storage.fetchTopicRelatedTopics(topicId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
        // ### TODO: respect maxResultSize
        return new ResultList(relTopics.size(), relTopics);
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
     *          Note: their composite values are not initialized.
     */
    ResultList<RelatedTopicModel> fetchTopicRelatedTopics(long topicId, List<String> assocTypeUris,
                                                                String myRoleTypeUri, String othersRoleTypeUri,
                                                                String othersTopicTypeUri, int maxResultSize) {
        ResultList<RelatedTopicModel> result = new ResultList();
        for (String assocTypeUri : assocTypeUris) {
            ResultList<RelatedTopicModel> res = fetchTopicRelatedTopics(topicId, assocTypeUri, myRoleTypeUri,
                othersRoleTypeUri, othersTopicTypeUri, maxResultSize);
            result.addAll(res);
        }
        return result;
    }

    // ---

    /**
     * Convenience method (checks singularity).
     *
     * @return  The fetched association.
     *          Note: its composite value is not initialized.
     */
    RelatedAssociationModel fetchTopicRelatedAssociation(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                                String othersRoleTypeUri, String othersAssocTypeUri) {
        List<RelatedAssociationModel> assocs = fetchTopicRelatedAssociations(topicId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersAssocTypeUri);
        switch (assocs.size()) {
        case 0:
            return null;
        case 1:
            return assocs.get(0);
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
     *          Note: their composite values are not initialized.
     */
    List<RelatedAssociationModel> fetchTopicRelatedAssociations(long topicId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        return storage.fetchTopicRelatedAssociations(topicId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri);
    }

    // ---

    /**
     * Convenience method (checks singularity).
     *
     * @return  The fetched topics.
     *          Note: their composite values are not initialized.
     */
    RelatedTopicModel fetchAssociationRelatedTopic(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                          String othersRoleTypeUri, String othersTopicTypeUri) {
        ResultList<RelatedTopicModel> topics = fetchAssociationRelatedTopics(assocId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri, 0);
        switch (topics.getSize()) {
        case 0:
            return null;
        case 1:
            return topics.iterator().next();
        default:
            throw new RuntimeException("Ambiguity: there are " + topics.getSize() + " related topics (assocId=" +
                assocId + ", assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri + "\", " +
                "othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersTopicTypeUri=\"" + othersTopicTypeUri + "\")");
        }
    }

    /**
     * @return  The fetched topics.
     *          Note: their composite values are not initialized.
     */
    ResultList<RelatedTopicModel> fetchAssociationRelatedTopics(long assocId, String assocTypeUri,
                                                                      String myRoleTypeUri, String othersRoleTypeUri,
                                                                      String othersTopicTypeUri, int maxResultSize) {
        List<RelatedTopicModel> relTopics = storage.fetchAssociationRelatedTopics(assocId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
        // ### TODO: respect maxResultSize
        return new ResultList(relTopics.size(), relTopics);
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
     *          Note: their composite values are not initialized.
     */
    ResultList<RelatedTopicModel> fetchAssociationRelatedTopics(long assocId, List<String> assocTypeUris,
                                                                      String myRoleTypeUri, String othersRoleTypeUri,
                                                                      String othersTopicTypeUri, int maxResultSize) {
        ResultList<RelatedTopicModel> result = new ResultList();
        for (String assocTypeUri : assocTypeUris) {
            ResultList<RelatedTopicModel> res = fetchAssociationRelatedTopics(assocId, assocTypeUri, myRoleTypeUri,
                othersRoleTypeUri, othersTopicTypeUri, maxResultSize);
            result.addAll(res);
        }
        return result;
    }

    // ---

    /**
     * Convenience method (checks singularity).
     *
     * @return  The fetched association.
     *          Note: its composite value is not initialized.
     */
    RelatedAssociationModel fetchAssociationRelatedAssociation(long assocId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        List<RelatedAssociationModel> assocs = fetchAssociationRelatedAssociations(assocId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersAssocTypeUri);
        switch (assocs.size()) {
        case 0:
            return null;
        case 1:
            return assocs.get(0);
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
     *          Note: their composite values are not initialized.
     */
    List<RelatedAssociationModel> fetchAssociationRelatedAssociations(long assocId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        return storage.fetchAssociationRelatedAssociations(assocId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri);
    }

    // ---

    /**
     * Convenience method (checks singularity).
     *
     * @param   id                  id of a topic or an association
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     *
     * @return  The fetched topic.
     *          Note: its composite value is not initialized.
     */
    RelatedTopicModel fetchRelatedTopic(long id, String assocTypeUri, String myRoleTypeUri,
                                               String othersRoleTypeUri, String othersTopicTypeUri) {
        ResultList<RelatedTopicModel> topics = fetchRelatedTopics(id, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
        switch (topics.getSize()) {
        case 0:
            return null;
        case 1:
            return topics.iterator().next();
        default:
            throw new RuntimeException("Ambiguity: there are " + topics.getSize() + " related topics (id=" + id +
                ", assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri + "\", " +
                "othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersTopicTypeUri=\"" + othersTopicTypeUri + "\")");
        }
    }

    /**
     * @param   id                  id of a topic or an association
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     *
     * @return  The fetched topics.
     *          Note: their composite values are not initialized.
     */
    ResultList<RelatedTopicModel> fetchRelatedTopics(long id, String assocTypeUri, String myRoleTypeUri,
                                                           String othersRoleTypeUri, String othersTopicTypeUri) {
        List<RelatedTopicModel> relTopics = storage.fetchRelatedTopics(id, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
        return new ResultList(relTopics.size(), relTopics);
    }

    // ### TODO: decorator for fetchRelatedAssociations()



    // === Properties ===

    Object fetchProperty(long id, String propUri) {
        return storage.fetchProperty(id, propUri);
    }

    boolean hasProperty(long id, String propUri) {
        return storage.hasProperty(id, propUri);
    }

    // ---

    List<TopicModel> fetchTopicsByProperty(String propUri, Object propValue) {
        return storage.fetchTopicsByProperty(propUri, propValue);
    }

    List<TopicModel> fetchTopicsByPropertyRange(String propUri, Number from, Number to) {
        return storage.fetchTopicsByPropertyRange(propUri, from, to);
    }

    List<AssociationModel> fetchAssociationsByProperty(String propUri, Object propValue) {
        return storage.fetchAssociationsByProperty(propUri, propValue);
    }

    List<AssociationModel> fetchAssociationsByPropertyRange(String propUri, Number from, Number to) {
        return storage.fetchAssociationsByPropertyRange(propUri, from, to);
    }

    // ---

    void storeTopicProperty(long topicId, String propUri, Object propValue, boolean addToIndex) {
        storage.storeTopicProperty(topicId, propUri, propValue, addToIndex);
    }

    void storeAssociationProperty(long assocId, String propUri, Object propValue, boolean addToIndex) {
        storage.storeAssociationProperty(assocId, propUri, propValue, addToIndex);
    }

    // ---

    void removeTopicProperty(long topicId, String propUri) {
        storage.deleteTopicProperty(topicId, propUri);
    }

    void removeAssociationProperty(long assocId, String propUri) {
        storage.deleteAssociationProperty(assocId, propUri);
    }



    // === DB ===

    DeepaMehtaTransaction beginTx() {
        return storage.beginTx();
    }

    /**
     * Initializes the database.
     * Prerequisite: there is an open transaction.
     *
     * @return  <code>true</code> if a clean install is detected, <code>false</code> otherwise.
     */
    boolean init() {
        boolean isCleanInstall = storage.setupRootNode();
        if (isCleanInstall) {
            logger.info("Starting with a fresh DB -- Setting migration number to 0");
            storeMigrationNr(0);
        }
        return isCleanInstall;
    }

    void shutdown() {
        storage.shutdown();
    }

    // ---

    int fetchMigrationNr() {
        return (Integer) storage.fetchProperty(0, "core_migration_nr");
    }

    void storeMigrationNr(int migrationNr) {
        storage.storeTopicProperty(0, "core_migration_nr", migrationNr, false);     // addToIndex=false
    }

    // ---

    Object getDatabaseVendorObject() {
        return storage.getDatabaseVendorObject();
    }
}
