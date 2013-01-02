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
import java.util.Arrays;
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
     * Convenience method.
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
    public ResultSet<RelatedTopicModel> getTopicRelatedTopics(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                              String othersRoleTypeUri, String othersTopicTypeUri,
                                                              int maxResultSize) {
        return mg.getTopicRelatedTopics(topicId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri,
            maxResultSize);
    }

    /**
     * Convenience method.
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
     * @return  The fetched associations.
     *          Note: their composite values are not initialized.
     */
    public Set<AssociationModel> getTopicAssociations(long topicId) {
        return mg.getTopicAssociations(topicId);
    }

    // ---

    /**
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

    /**
     * Stores the topic's value.
     * <p>
     * Note: the value is not indexed automatically. Use the {@link indexTopicValue} method. ### FIXDOC
     *
     * @return  The previous value, or <code>null</code> if no value was stored before. ### FIXDOC
     */
    public void setTopicValue(long topicId, SimpleValue value, IndexMode indexMode, String indexKey) {
        mg.setTopicValue(topicId, value, indexMode, indexKey);
    }

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



    // === Associations ===

    public AssociationModel getAssociation(long assocId) {
        return buildAssociation(mg.getMehtaEdge(assocId));
    }

    /**
     * Returns the association between two topics, qualified by association type and both role types.
     * If no such association exists <code>null</code> is returned.
     * If more than one association exist, a runtime exception is thrown.
     *
     * @param   assocTypeUri    Association type filter. Pass <code>null</code> to switch filter off.
     *                          ### FIXME: for methods with a singular return value all filters should be mandatory
     */
    public AssociationModel getAssociation(String assocTypeUri, long topic1Id, long topic2Id, String roleTypeUri1,
                                                                                              String roleTypeUri2) {
        Set<MehtaEdge> edges = mg.getMehtaEdges(topic1Id, topic2Id, roleTypeUri1, roleTypeUri2);
        // apply type filter
        if (assocTypeUri != null) {
            filterEdgesByAssociationType(edges, assocTypeUri);
        }
        // error check
        if (edges.size() > 1) {
            throw new RuntimeException("Ambiguity: there are " + edges.size() + " \"" + assocTypeUri +
                "\" associations (topic1Id=" + topic1Id + ", topic2Id=" + topic2Id + ", " +
                "roleTypeUri1=\"" + roleTypeUri1 + "\", roleTypeUri2=\"" + roleTypeUri2 + "\")");
        }
        //
        if (edges.size() == 0) {
            return null;
        } else {
            return buildAssociation(edges.iterator().next());
        }
    }

    public AssociationModel getAssociationBetweenTopicAndAssociation(String assocTypeUri, long topicId, long assocId,
                                                                     String topicRoleTypeUri, String assocRoleTypeUri) {
        Set<MehtaEdge> edges = mg.getMehtaEdgesBetweenNodeAndEdge(topicId, assocId, topicRoleTypeUri, assocRoleTypeUri);
        // apply type filter
        if (assocTypeUri != null) {
            filterEdgesByAssociationType(edges, assocTypeUri);
        }
        // error check
        if (edges.size() > 1) {
            throw new RuntimeException("Ambiguity: there are " + edges.size() + " \"" + assocTypeUri +
                "\" associations (topicId=" + topicId + ", assocId=" + assocId + ", " +
                "topicRoleTypeUri=\"" + topicRoleTypeUri + "\", assocRoleTypeUri=\"" + assocRoleTypeUri + "\")");
        }
        //
        if (edges.size() == 0) {
            return null;
        } else {
            return buildAssociation(edges.iterator().next());
        }
    }

    /**
     * Returns the associations between two topics. If no such association exists an empty set is returned.
     *
     * @param   assocTypeUri    Association type filter. Pass <code>null</code> to switch filter off.
     */
    public Set<AssociationModel> getAssociations(long topic1Id, long topic2Id, String assocTypeUri) {
        Set<MehtaEdge> edges = mg.getMehtaEdges(topic1Id, topic2Id);
        // apply type filter
        if (assocTypeUri != null) {
            filterEdgesByAssociationType(edges, assocTypeUri);
        }
        //
        return buildAssociations(edges);
    }

    // ---

    public RelatedTopicModel getAssociationRelatedTopic(long assocId, String assocTypeUri,
                                                                    String myRoleTypeUri, String othersRoleTypeUri,
                                                                    String othersTopicTypeUri) {
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

    public ResultSet<RelatedTopicModel> getAssociationRelatedTopics(long assocId, String assocTypeUri,
                                                                    String myRoleTypeUri, String othersRoleTypeUri,
                                                                    String othersTopicTypeUri, int maxResultSize) {
        List assocTypeUris = assocTypeUri != null ? Arrays.asList(assocTypeUri) : null;
        return getAssociationRelatedTopics(assocId, assocTypeUris, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri,
            maxResultSize);
    }

    /**
     * @param   assocTypeUris       may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    public ResultSet<RelatedTopicModel> getAssociationRelatedTopics(long assocId, List assocTypeUris,
                                                                    String myRoleTypeUri, String othersRoleTypeUri,
                                                                    String othersTopicTypeUri, int maxResultSize) {
        Set<ConnectedMehtaNode> nodes = mg.getMehtaEdge(assocId).getConnectedMehtaNodes(myRoleTypeUri,
                                                                                        othersRoleTypeUri);
        if (othersTopicTypeUri != null) {
            filterNodesByTopicType(nodes, othersTopicTypeUri);
        }
        if (assocTypeUris != null) {
            filterNodesByAssociationType(nodes, assocTypeUris);
        }
        return buildRelatedTopics(nodes, maxResultSize);
    }

    // ---

    public Set<AssociationModel> getAssociationAssociations(long assocId) {
        return mg.getAssociationAssociations(assocId);
    }

    public RelatedAssociationModel getAssociationRelatedAssociation(long assocId, String assocTypeUri,
                                                                    String myRoleTypeUri, String othersRoleTypeUri) {
        // FIXME: assocTypeUri not respected
        ConnectedMehtaEdge edge = mg.getMehtaEdge(assocId).getConnectedMehtaEdge(myRoleTypeUri, othersRoleTypeUri);
        return edge != null ? buildRelatedAssociation(edge) : null;
    }

    // ---

    public void setRoleTypeUri(long assocId, long objectId, String roleTypeUri) {
        mg.getMehtaEdge(assocId).getMehtaObject(objectId).setRoleType(roleTypeUri);
    }

    // ---

    /**
     * Stores and indexes the association's URI.
     */
    public void setAssociationUri(long assocId, String uri) {
        storeAndIndexUri(mg.getMehtaEdge(assocId), uri);
    }

    /**
     * Stores the association's value.
     * <p>
     * Note: the value is not indexed automatically. Use the {@link indexAssociationValue} method.
     *
     * @return  The previous value, or <code>null</code> if no value was stored before.
     */
    public SimpleValue setAssociationValue(long assocId, SimpleValue value) {
        Object oldValue = mg.getMehtaEdge(assocId).setObject("value", value.value());
        return oldValue != null ? new SimpleValue(oldValue) : null;
    }

    /**
     * @param   oldValue    may be null
     */
    public void indexAssociationValue(long assocId, IndexMode indexMode, String indexKey, SimpleValue value,
                                                                                          SimpleValue oldValue) {
        MehtaGraphIndexMode mgIndexMode = fromIndexMode(indexMode);
        Object oldVal = oldValue != null ? oldValue.value() : null;
        mg.getMehtaEdge(assocId).indexAttribute(mgIndexMode, indexKey, value.value(), oldVal);
    }

    // ---

    public void createAssociation(AssociationModel assocModel) {
        MehtaEdge edge = mg.createMehtaEdge(
            getMehtaObjectRole(assocModel.getRoleModel1()),
            getMehtaObjectRole(assocModel.getRoleModel2()));
        assocModel.setId(edge.getId());
    }

    public void deleteAssociation(long assocId) {
        mg.getMehtaEdge(assocId).delete();
    }



    // === Access Control ===

    /**
     * Fetches the Access Control List for the specified topic or association.
     * If no one is stored an empty Access Control List is returned.
     */
    public AccessControlList getACL(long objectId) {
        try {
            return new AccessControlList(new JSONObject(getProperty(objectId, "acl", "{}")));
        } catch (Exception e) {
            throw new RuntimeException("Fetching access control list for object " + objectId + " failed", e);
        }
    }

    /**
     * Creates the Access Control List for the specified topic or association.
     */
    public void createACL(long objectId, AccessControlList acl) {
        setProperty(objectId, "acl", acl.toJSON().toString());
    }

    // ---

    public String getCreator(long objectId) {
        return getProperty(objectId, "creator", null);
    }

    public void setCreator(long objectId, String username) {
        setProperty(objectId, "creator", username);
    }

    // ---

    public String getOwner(long objectId) {
        return getProperty(objectId, "owner", null);
    }

    public void setOwner(long objectId, String username) {
        setProperty(objectId, "owner", username);
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
        // init core migration number
        boolean isCleanInstall = false;
        if (!mg.hasTopicProperty(0, "core_migration_nr")) {
            logger.info("Starting with a fresh DB -- Setting migration number to 0");
            setMigrationNr(0);
            setupMetaTypeNode();
            isCleanInstall = true;
        }
        return isCleanInstall;
    }

    public void shutdown() {
        mg.shutdown();
    }

    public int getMigrationNr() {
        return (Integer) mg.getTopicProperty(0, "core_migration_nr");
    }

    public void setMigrationNr(int migrationNr) {
        mg.setTopicProperty(0, "core_migration_nr", migrationNr);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === DB ===

    private void setupMetaTypeNode() {
        MehtaNode refNode = mg.getMehtaNode(0);
        String uri = "dm4.core.meta_type";
        refNode.setString("uri", uri);
        refNode.setString("value", "Meta Type");
        //
        refNode.indexAttribute(MehtaGraphIndexMode.KEY, "uri", uri, null);     // oldValue=null
    }
}
