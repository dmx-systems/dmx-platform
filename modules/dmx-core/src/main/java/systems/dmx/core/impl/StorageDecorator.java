package systems.dmx.core.impl;

import systems.dmx.core.storage.spi.DMXStorage;

import java.util.List;
import java.util.logging.Logger;



/**
 * A utility layer above storage implementation.
 *  - Adds fetch-single calls on top of fetch-multiple calls and performs sanity checks.
 */
public class StorageDecorator {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private final DMXStorage db;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    StorageDecorator(DMXStorage db) {
        this.db = db;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // Note: is public as accessed by storage tests.
    // Note: not part of storage SPI as it is sole on-top convenience.

    /**
     * Retrieves a single topic by exact value.
     *
     * @return  The fetched topic, or <code>null</code> if no such topic exists.
     *
     * @throws  RuntimeException    if more than one topic exists.
     */
    public TopicModelImpl fetchTopic(String key, Object value) {
        try {
            List<TopicModelImpl> topics = db.fetchTopics(key, value);
            int size = topics.size();
            switch (size) {
            case 0:
                return null;
            case 1:
                return topics.get(0);
            default:
                throw new RuntimeException("Ambiguity: " + size + " topics do match, key=\"" + key + "\", value=" +
                    value);
            }
        } catch (Exception e) {
            throw new RuntimeException("Fetching topic by value failed", e);
        }
    }

    /**
     * Retrieves a single assoc by exact value.
     *
     * @return  The fetched assoc, or <code>null</code> if no such assoc exists.
     *
     * @throws  RuntimeException    if more than one assoc exists.
     */
    public AssocModelImpl fetchAssoc(String key, Object value) {
        try {
            List<AssocModelImpl> assocs = db.fetchAssocs(key, value);
            int size = assocs.size();
            switch (size) {
            case 0:
                return null;
            case 1:
                return assocs.get(0);
            default:
                throw new RuntimeException("Ambiguity: " + size + " assocs do match, key=\"" + key + "\", value=" +
                    value);
            }
        } catch (Exception e) {
            throw new RuntimeException("Fetching assoc by value failed", e);
        }
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
        List<AssocModelImpl> assocs = db.fetchAssocs(assocTypeUri, topicId1, topicId2, roleTypeUri1, roleTypeUri2);
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
        List<AssocModelImpl> assocs = db.fetchAssocsBetweenTopicAndAssoc(assocTypeUri, topicId, assocId,
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
        List<RelatedTopicModelImpl> topics = db.fetchTopicRelatedTopics(topicId, assocTypeUri, myRoleTypeUri,
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
     * Convenience method (checks singularity).
     *
     * @return  The fetched association.
     *          Note: its child topics are not fetched.
     */
    final RelatedAssocModelImpl fetchTopicRelatedAssoc(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                       String othersRoleTypeUri, String othersAssocTypeUri) {
        List<RelatedAssocModelImpl> assocs = db.fetchTopicRelatedAssocs(topicId, assocTypeUri, myRoleTypeUri,
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

    // --- Assoc Source ---

    /**
     * Convenience method (checks singularity).
     *
     * @return  The fetched topic.
     *          Note: its child topics are not fetched.
     */
    final RelatedTopicModelImpl fetchAssocRelatedTopic(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                       String othersRoleTypeUri, String othersTopicTypeUri) {
        List<RelatedTopicModelImpl> topics = db.fetchAssocRelatedTopics(assocId, assocTypeUri, myRoleTypeUri,
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
     * Convenience method (checks singularity).
     *
     * @return  The fetched association.
     *          Note: its child topics are not fetched.
     */
    final RelatedAssocModelImpl fetchAssocRelatedAssoc(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                       String othersRoleTypeUri, String othersAssocTypeUri) {
        List<RelatedAssocModelImpl> assocs = db.fetchAssocRelatedAssocs(assocId, assocTypeUri, myRoleTypeUri,
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
        List<RelatedTopicModelImpl> topics = db.fetchRelatedTopics(objectId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
        switch (topics.size()) {
        case 0:
            return null;
        case 1:
            return topics.iterator().next();
        default:
            // TODO: use this format for all ambiguity exceptions
            throw new RuntimeException("Ambiguity: object " + objectId + " has " + topics.size() + " related \"" +
                othersTopicTypeUri + "\" topics, assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" +
                myRoleTypeUri + "\", " + "othersRoleTypeUri=\"" + othersRoleTypeUri + "\", topics=" + topics);
        }
    }

    // ### TODO: decorator fetchRelatedAssoc()



    // === DB ===

    /**
     * Initializes the database.
     * Prerequisite: there is an open transaction.
     *
     * @return  <code>true</code> if a clean install is detected, <code>false</code> otherwise.
     */
    final boolean init() {
        boolean isCleanInstall = db.setupRootNode();
        if (isCleanInstall) {
            logger.info("Clean install detected -- Starting with a fresh DB");
            storeMigrationNr(0);
        }
        return isCleanInstall;
    }

    // ---

    final int fetchMigrationNr() {
        return (Integer) db.fetchProperty(0, "core_migration_nr");
    }

    final void storeMigrationNr(int migrationNr) {
        db.storeTopicProperty(0, "core_migration_nr", migrationNr, false);     // addToIndex=false
    }
}
