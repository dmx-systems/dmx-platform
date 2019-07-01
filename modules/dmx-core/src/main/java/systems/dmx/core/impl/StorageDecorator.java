package systems.dmx.core.impl;

import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.storage.spi.DMXTransaction;
import systems.dmx.core.storage.spi.DMXStorage;

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



    // === Associations ===

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
    final AssocModelImpl fetchAssoc(String assocTypeUri, long topicId1, long topicId2, String roleTypeUri1,
                                                                                       String roleTypeUri2) {
        List<AssocModelImpl> assocs = storage.fetchAssocs(assocTypeUri, topicId1, topicId2, roleTypeUri1, roleTypeUri2);
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
     * Convenience method (checks singularity).
     */
    final AssocModelImpl fetchAssocBetweenTopicAndAssoc(String assocTypeUri, long topicId, long assocId,
                                                        String topicRoleTypeUri, String assocRoleTypeUri) {
        List<AssocModelImpl> assocs = storage.fetchAssocsBetweenTopicAndAssoc(assocTypeUri, topicId, assocId,
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
    final RelatedAssocModelImpl fetchTopicRelatedAssoc(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                       String othersRoleTypeUri, String othersAssocTypeUri) {
        List<RelatedAssocModelImpl> assocs = fetchTopicRelatedAssocs(topicId, assocTypeUri, myRoleTypeUri,
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
    final List<RelatedAssocModelImpl> fetchTopicRelatedAssocs(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                              String othersRoleTypeUri, String othersAssocTypeUri) {
        return (List<RelatedAssocModelImpl>) storage.fetchTopicRelatedAssocs(topicId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersAssocTypeUri);
    }

    // ---

    /**
     * @return  The fetched associations.
     *          Note: their child topics are not fetched.
     */
    final List<AssocModelImpl> fetchTopicAssocs(long topicId) {
        return (List<AssocModelImpl>) storage.fetchTopicAssocs(topicId);
    }

    // --- Assoc Source ---

    /**
     * Convenience method (checks singularity).
     *
     * @return  The fetched topic.
     *          Note: its child topics are not fetched.
     */
    final RelatedTopicModelImpl fetchAssocRelatedTopic(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                       String othersRoleTypeUri, String othersTopicTypeUri) {
        List<RelatedTopicModelImpl> topics = fetchAssocRelatedTopics(assocId, assocTypeUri, myRoleTypeUri,
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
    final List<RelatedTopicModelImpl> fetchAssocRelatedTopics(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                              String othersRoleTypeUri, String othersTopicTypeUri) {
        return (List<RelatedTopicModelImpl>) storage.fetchAssocRelatedTopics(assocId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
    }

    // ---

    /**
     * Convenience method (checks singularity).
     *
     * @return  The fetched association.
     *          Note: its child topics are not fetched.
     */
    final RelatedAssocModelImpl fetchAssocRelatedAssoc(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                       String othersRoleTypeUri, String othersAssocTypeUri) {
        List<RelatedAssocModelImpl> assocs = fetchAssocRelatedAssocs(assocId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersAssocTypeUri);
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
    final List<RelatedAssocModelImpl> fetchAssocRelatedAssocs(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                              String othersRoleTypeUri, String othersAssocTypeUri) {
        return (List<RelatedAssocModelImpl>) storage.fetchAssocRelatedAssocs(assocId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersAssocTypeUri);
    }

    // ---

    final List<AssocModelImpl> fetchAssocAssocs(long assocId) {
        return (List<AssocModelImpl>) storage.fetchAssocAssocs(assocId);
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

    // ### TODO: decorator for fetchRelatedAssocs()



    // === DB ===

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

    // ---

    final int fetchMigrationNr() {
        return (Integer) storage.fetchProperty(0, "core_migration_nr");
    }

    final void storeMigrationNr(int migrationNr) {
        storage.storeTopicProperty(0, "core_migration_nr", migrationNr, false);     // addToIndex=false
    }
}
