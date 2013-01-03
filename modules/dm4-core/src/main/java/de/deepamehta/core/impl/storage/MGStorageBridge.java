package de.deepamehta.core.impl.storage;

import de.deepamehta.core.DeepaMehtaTransaction;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.accesscontrol.AccessControlList;
import de.deepamehta.core.storage.ConnectedMehtaEdge;
import de.deepamehta.core.storage.ConnectedMehtaNode;
import de.deepamehta.core.storage.MehtaGraphIndexMode;
import de.deepamehta.core.storage.MehtaObjectRole;
import de.deepamehta.core.storage.spi.MehtaEdge;
import de.deepamehta.core.storage.spi.MehtaGraph;
import de.deepamehta.core.storage.spi.MehtaNode;
import de.deepamehta.core.storage.spi.MehtaObject;
import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;



/**
 * A DeepaMehta storage implementation by the means of a MehtaGraph implementation. ### FIXDOC
 * <p>
 * The DeepaMehta service knows nothing about a MehtaGraph and a MehtaGraph knows nothing about DeepaMehta.
 * This class bridges between them. ### FIXDOC
 */
public class MGStorageBridge {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private MehtaGraph mg;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public MGStorageBridge(MehtaGraph mg) {
        this.mg = mg;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Topics ===

    /**
     * @return  The fetched topic.
     *          Note: its composite value is not initialized.
     */
    public TopicModel getTopic(long topicId) {
        return mg.getMehtaNode(topicId);
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
    public TopicModel getTopic(String key, SimpleValue value) {
        return mg.getMehtaNode(key, value.value());
    }

    // ---

    /**
     * @return  The fetched topics.
     *          Note: their composite values are not initialized.
     */
    public Set<TopicModel> searchTopics(String searchTerm, String fieldUri) {
        return DeepaMehtaUtils.toTopicSet(mg.queryMehtaNodes(fieldUri, searchTerm));
    }

    // ---

    /**
     * Stores and indexes the topic's URI.
     */
    public void setTopicUri(long topicId, String uri) {
        mg.setTopicUri(topicId, uri);
    }

    // ---

    /**
     * Convenience method.
     */
    public void setTopicValue(long assocId, SimpleValue value) {
        setTopicValue(assocId, value, new HashSet(asList(IndexMode.OFF)), null);
    }

    /**
     * Stores the topic's value.
     * <p>
     * Note: the value is not indexed automatically. Use the {@link indexTopicValue} method. ### FIXDOC
     *
     * @return  The previous value, or <code>null</code> if no value was stored before. ### FIXDOC
     */
    public void setTopicValue(long topicId, SimpleValue value, Set<IndexMode> indexModes, String indexKey) {
        mg.setTopicValue(topicId, value, indexModes, indexKey);
    }

    // ---

    /**
     * Creates a topic.
     * <p>
     * The topic's URI is stored and indexed.
     *
     * @return  FIXME ### the created topic. Note:
     *          - the topic URI   is initialzed and     persisted.
     *          - the topic value is initialzed but not persisted.
     *          - the type URI    is initialzed but not persisted.
     */
    public void createTopic(TopicModel topicModel) {
        mg.createMehtaNode(topicModel);
    }

    /**
     * Deletes the topic.
     * <p>
     * Prerequisite: the topic has no relations.
     */
    public void deleteTopic(long topicId) {
        mg.deleteTopic(topicId);
    }



    // --- Traversal ---

    /**
     * @return  The fetched associations.
     *          Note: their composite values are not initialized.
     */
    public Set<AssociationModel> getTopicAssociations(long topicId) {
        return mg.getTopicAssociations(topicId);
    }

    // ---

    /**
     * Convenience method (checks singularity).
     *
     * @return  The fetched topics.
     *          Note: their composite values are not initialized.
     */
    public RelatedTopicModel getTopicRelatedTopic(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                  String othersRoleTypeUri, String othersTopicTypeUri) {
        ResultSet<RelatedTopicModel> topics = getTopicRelatedTopics(topicId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri, 0);
        switch (topics.getSize()) {
        case 0:
            return null;
        case 1:
            return topics.getIterator().next();
        default:
            throw new RuntimeException("Ambiguity: there are " + topics.getSize() + " related topics (topicId=" +
                topicId + ", assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri + "\", " +
                "othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersTopicTypeUri=\"" + othersTopicTypeUri + "\")");
        }
    }

    /**
     * @return  The fetched topics.
     *          Note: their composite values are not initialized.
     */
    public ResultSet<RelatedTopicModel> getTopicRelatedTopics(long topicId, String assocTypeUri,
                                                              String myRoleTypeUri, String othersRoleTypeUri,
                                                              String othersTopicTypeUri, int maxResultSize) {
        Set<RelatedTopicModel> relTopics = mg.getTopicRelatedTopics(topicId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
        // ### TODO: respect maxResultSize
        return new ResultSet(relTopics.size(), relTopics);
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
    public ResultSet<RelatedTopicModel> getTopicRelatedTopics(long topicId, List<String> assocTypeUris,
                                                              String myRoleTypeUri, String othersRoleTypeUri,
                                                              String othersTopicTypeUri, int maxResultSize) {
        ResultSet<RelatedTopicModel> result = new ResultSet();
        for (String assocTypeUri : assocTypeUris) {
            ResultSet<RelatedTopicModel> res = getTopicRelatedTopics(topicId, assocTypeUri, myRoleTypeUri,
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
    public RelatedAssociationModel getTopicRelatedAssociation(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                              String othersRoleTypeUri, String othersAssocTypeUri) {
        Set<RelatedAssociationModel> assocs = getTopicRelatedAssociations(topicId, assocTypeUri, myRoleTypeUri,
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
     *          Note: their composite values are not initialized.
     */
    public Set<RelatedAssociationModel> getTopicRelatedAssociations(long topicId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        return mg.getTopicRelatedAssociations(topicId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri);
    }



    // === Associations ===

    public AssociationModel getAssociation(long assocId) {
        return mg.getMehtaEdge(assocId);
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
    public AssociationModel getAssociation(String assocTypeUri, long topicId1, long topicId2, String roleTypeUri1,
                                                                                              String roleTypeUri2) {
        Set<AssociationModel> assocs = getAssociations(assocTypeUri, topicId1, topicId2, roleTypeUri1, roleTypeUri2);
        switch (assocs.size()) {
        case 0:
            return null;
        case 1:
            return assocs.iterator().next();
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
    public Set<AssociationModel> getAssociations(String assocTypeUri, long topicId1, long topicId2, String roleTypeUri1,
                                                                                                  String roleTypeUri2) {
        return mg.getMehtaEdges(assocTypeUri, topicId1, topicId2, roleTypeUri1, roleTypeUri2);
    }

    // ---

    /**
     * Convenience method (checks singularity).
     */
    public AssociationModel getAssociationBetweenTopicAndAssociation(String assocTypeUri, long topicId, long assocId,
                                                                     String topicRoleTypeUri, String assocRoleTypeUri) {
        Set<AssociationModel> assocs = getAssociationsBetweenTopicAndAssociation(assocTypeUri, topicId, assocId,
            topicRoleTypeUri, assocRoleTypeUri);
        switch (assocs.size()) {
        case 0:
            return null;
        case 1:
            return assocs.iterator().next();
        default:
            throw new RuntimeException("Ambiguity: there are " + assocs.size() + " \"" + assocTypeUri +
                "\" associations (topicId=" + topicId + ", assocId=" + assocId + ", " +
                "topicRoleTypeUri=\"" + topicRoleTypeUri + "\", assocRoleTypeUri=\"" + assocRoleTypeUri + "\")");
        }
    }

    public Set<AssociationModel> getAssociationsBetweenTopicAndAssociation(String assocTypeUri, long topicId,
                                                       long assocId, String topicRoleTypeUri, String assocRoleTypeUri) {
        return mg.getMehtaEdgesBetweenNodeAndEdge(assocTypeUri, topicId, assocId, topicRoleTypeUri, assocRoleTypeUri);
    }

    // ---

    public void storeRoleTypeUri(long assocId, long playerId, String roleTypeUri) {
        mg.storeRoleTypeUri(assocId, playerId, roleTypeUri);
    }

    // ---

    /**
     * Stores and indexes the association's URI.
     */
    public void setAssociationUri(long assocId, String uri) {
        mg.storeAssociationUri(assocId, uri);
    }

    // ---

    /**
     * Convenience method.
     */
    public void setAssociationValue(long assocId, SimpleValue value) {
        setAssociationValue(assocId, value, new HashSet(asList(IndexMode.OFF)), null);
    }

    /**
     * Stores the association's value.
     * <p>
     * Note: the value is not indexed automatically. Use the {@link indexAssociationValue} method. ### FIXDOC
     *
     * @return  The previous value, or <code>null</code> if no value was stored before. ### FIXDOC
     */
    public void setAssociationValue(long assocId, SimpleValue value, Set<IndexMode> indexModes, String indexKey) {
        mg.storeAssociationValue(assocId, value, indexModes, indexKey);
    }

    // ---

    public void createAssociation(AssociationModel assocModel) {
        mg.createMehtaEdge(assocModel);
    }

    public void deleteAssociation(long assocId) {
        mg.deleteAssociation(assocId);
    }



    // --- Traversal ---

    public Set<AssociationModel> getAssociationAssociations(long assocId) {
        return mg.getAssociationAssociations(assocId);
    }

    // ---

    /**
     * Convenience method (checks singularity).
     *
     * @return  The fetched topics.
     *          Note: their composite values are not initialized.
     */
    public RelatedTopicModel getAssociationRelatedTopic(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                        String othersRoleTypeUri, String othersTopicTypeUri) {
        ResultSet<RelatedTopicModel> topics = getAssociationRelatedTopics(assocId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri, 0);
        switch (topics.getSize()) {
        case 0:
            return null;
        case 1:
            return topics.getIterator().next();
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
    public ResultSet<RelatedTopicModel> getAssociationRelatedTopics(long assocId, String assocTypeUri,
                                                                    String myRoleTypeUri, String othersRoleTypeUri,
                                                                    String othersTopicTypeUri, int maxResultSize) {
        Set<RelatedTopicModel> relTopics = mg.getAssociationRelatedTopics(assocId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
        // ### TODO: respect maxResultSize
        return new ResultSet(relTopics.size(), relTopics);
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
    public ResultSet<RelatedTopicModel> getAssociationRelatedTopics(long assocId, List<String> assocTypeUris,
                                                                    String myRoleTypeUri, String othersRoleTypeUri,
                                                                    String othersTopicTypeUri, int maxResultSize) {
        ResultSet<RelatedTopicModel> result = new ResultSet();
        for (String assocTypeUri : assocTypeUris) {
            ResultSet<RelatedTopicModel> res = getAssociationRelatedTopics(assocId, assocTypeUri, myRoleTypeUri,
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
    public RelatedAssociationModel getAssociationRelatedAssociation(long assocId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        Set<RelatedAssociationModel> assocs = getAssociationRelatedAssociations(assocId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersAssocTypeUri);
        switch (assocs.size()) {
        case 0:
            return null;
        case 1:
            return assocs.iterator().next();
        default:
            throw new RuntimeException("Ambiguity: there are " + assocs.size() + " related associations (assocId=" +
                assocId + ", assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri + "\", " +
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
    public Set<RelatedAssociationModel> getAssociationRelatedAssociations(long assocId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        return mg.getAssociationRelatedAssociations(assocId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri);
    }



    // === Access Control ===

    /**
     * Fetches the Access Control List for the specified topic or association.
     * If no one is stored an empty Access Control List is returned.
     */
    public AccessControlList getACL(long objectId) {
        try {
            boolean hasACL = mg.hasProperty(objectId, "acl");
            JSONObject acl = hasACL ? new JSONObject((String) mg.getProperty(objectId, "acl")) : new JSONObject();
            return new AccessControlList(acl);
        } catch (Exception e) {
            throw new RuntimeException("Fetching access control list for object " + objectId + " failed", e);
        }
    }

    /**
     * Creates the Access Control List for the specified topic or association.
     */
    public void createACL(long objectId, AccessControlList acl) {
        mg.setProperty(objectId, "acl", acl.toJSON().toString());
    }

    // ---

    public String getCreator(long objectId) {
        return mg.hasProperty(objectId, "creator") ? (String) mg.getProperty(objectId, "creator") : null;
    }

    public void setCreator(long objectId, String username) {
        mg.setProperty(objectId, "creator", username);
    }

    // ---

    public String getOwner(long objectId) {
        return mg.hasProperty(objectId, "owner") ? (String) mg.getProperty(objectId, "owner") : null;
    }

    public void setOwner(long objectId, String username) {
        mg.setProperty(objectId, "owner", username);
    }



    // === DB ===

    public DeepaMehtaTransaction beginTx() {
        return new MGTransactionAdapter(mg);
    }

    /**
     * Initializes the database.
     * Prerequisite: there is an open transaction.
     *
     * @return  <code>true</code> if a clean install is detected, <code>false</code> otherwise.
     */
    public boolean init() {
        boolean isCleanInstall = mg.setupRootNode();
        if (isCleanInstall) {
            logger.info("Starting with a fresh DB -- Setting migration number to 0");
            setMigrationNr(0);
        }
        return isCleanInstall;
    }

    public void shutdown() {
        mg.shutdown();
    }

    public int getMigrationNr() {
        return (Integer) mg.getProperty(0, "core_migration_nr");
    }

    public void setMigrationNr(int migrationNr) {
        mg.setProperty(0, "core_migration_nr", migrationNr);
    }
}
