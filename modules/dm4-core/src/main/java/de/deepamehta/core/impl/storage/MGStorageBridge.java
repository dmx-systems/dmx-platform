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
        List assocTypeUris = assocTypeUri != null ? Arrays.asList(assocTypeUri) : null;
        return getTopicRelatedTopics(topicId, assocTypeUris, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri,
            maxResultSize);
    }

    /**
     * @param   assocTypeUris       may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     *
     * @return  The fetched topics.
     *          Note: their composite values are not initialized.
     */
    public ResultSet<RelatedTopicModel> getTopicRelatedTopics(long topicId, List assocTypeUris, String myRoleTypeUri,
                                                              String othersRoleTypeUri, String othersTopicTypeUri,
                                                              int maxResultSize) {
        Set<ConnectedMehtaNode> nodes = mg.getMehtaNode(topicId).getConnectedMehtaNodes(myRoleTypeUri,
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

    /**
     * @param   myRoleTypeUri       may be null
     *
     * @return  The fetched associations.
     *          Note: their composite values are not initialized.
     */
    public Set<AssociationModel> getTopicAssociations(long topicId, String myRoleTypeUri) {
        return buildAssociations(mg.getMehtaNode(topicId).getMehtaEdges(myRoleTypeUri));
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
                                                                    String myRoleTypeUri, String othersRoleTypeUri,
                                                                    String othersAssocTypeUri) {
        Set<ConnectedMehtaEdge> edges = mg.getMehtaNode(topicId).getConnectedMehtaEdges(myRoleTypeUri,
                                                                                        othersRoleTypeUri);
        if (othersAssocTypeUri != null) {
            // TODO
            throw new RuntimeException("not yet implemented");
        }
        if (assocTypeUri != null) {
            filterConnectedEdgesByAssociationType(edges, assocTypeUri);
        }
        return buildRelatedAssociations(edges);
    }

    // ---

    /**
     * @return  The fetched topics.
     *          Note: their composite values are not initialized.
     */
    public Set<TopicModel> searchTopics(String searchTerm, String fieldUri) {
        return buildTopics(mg.queryMehtaNodes(fieldUri, searchTerm));
    }

    // ---

    /**
     * Stores and indexes the topic's URI.
     */
    public void setTopicUri(long topicId, String uri) {
        storeAndIndexUri(mg.getMehtaNode(topicId), uri);
    }

    /**
     * Stores the topic's value.
     * <p>
     * Note: the value is not indexed automatically. Use the {@link indexTopicValue} method.
     *
     * @return  The previous value, or <code>null</code> if no value was stored before.
     */
    public SimpleValue setTopicValue(long topicId, SimpleValue value) {
        Object oldValue = mg.getMehtaNode(topicId).setObject("value", value.value());
        return oldValue != null ? new SimpleValue(oldValue) : null;
    }

    /**
     * @param   oldValue    may be null
     */
    public void indexTopicValue(long topicId, IndexMode indexMode, String indexKey, SimpleValue value,
                                                                                    SimpleValue oldValue) {
        MehtaGraphIndexMode mgIndexMode = fromIndexMode(indexMode);
        Object oldVal = oldValue != null ? oldValue.value() : null;
        mg.getMehtaNode(topicId).indexAttribute(mgIndexMode, indexKey, value.value(), oldVal);
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
        mg.getMehtaNode(topicId).delete();
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

    /**
     * @param   myRoleTypeUri       may be null
     */
    public Set<AssociationModel> getAssociationAssociations(long assocId, String myRoleTypeUri) {
        return buildAssociations(mg.getMehtaEdge(assocId).getMehtaEdges(myRoleTypeUri));
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
        if (!mg.getMehtaNode(0).hasAttribute("core_migration_nr")) {
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
        return mg.getMehtaNode(0).getInteger("core_migration_nr");
    }

    public void setMigrationNr(int migrationNr) {
        mg.getMehtaNode(0).setInteger("core_migration_nr", migrationNr);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Build Topic models from MehtaNodes ===

    private TopicModel buildTopic(MehtaNode node) {
        long id = node.getId();
        String uri = node.getString("uri");
        String typeUri = getTopicTypeUri(node);
        SimpleValue value = new SimpleValue(node.getObject("value"));
        return new TopicModel(id, uri, typeUri, value, null);   // composite=null
    }

    private Set<TopicModel> buildTopics(List<MehtaNode> nodes) {
        Set<TopicModel> topics = new LinkedHashSet();
        for (MehtaNode node : nodes) {
            topics.add(buildTopic(node));
        }
        return topics;
    }

    private RelatedTopicModel buildRelatedTopic(ConnectedMehtaNode node) {
        try {
            RelatedTopicModel relTopic = new RelatedTopicModel(
                buildTopic(node.getMehtaNode()),
                buildAssociation(node.getConnectingMehtaEdge()));
            return relTopic;
        } catch (Exception e) {
            throw new RuntimeException("Building a RelatedTopicModel from a ConnectedMehtaNode failed (" + node + ")",
                e);
        }
    }

    private ResultSet<RelatedTopicModel> buildRelatedTopics(Set<ConnectedMehtaNode> nodes, int maxResultSize) {
        Set<RelatedTopicModel> relTopics = new LinkedHashSet();
        for (ConnectedMehtaNode node : nodes) {
            relTopics.add(buildRelatedTopic(node));
            // limit result set
            if (relTopics.size() == maxResultSize) {
                break;
            }
        }
        return new ResultSet<RelatedTopicModel>(nodes.size(), relTopics);
    }



    // === Build Association models from MehtaEdges ===

    private AssociationModel buildAssociation(MehtaEdge edge) {
        try {
            long id = edge.getId();
            // ### TODO: retrieve association URI
            String typeUri = getAssociationTypeUri(edge);
            SimpleValue value = new SimpleValue(edge.getObject("value"));
            List<RoleModel> roleModels = getRoleModels(edge);
            return new AssociationModel(id, typeUri, roleModels.get(0), roleModels.get(1), value, null);
        } catch (Exception e) {                                                                // composite=null
            throw new RuntimeException("Building an AssociationModel from a MehtaEdge failed (edge=" + edge + ")", e);
        }
    }

    private Set<AssociationModel> buildAssociations(Iterable<MehtaEdge> edges) {
        Set<AssociationModel> assocs = new LinkedHashSet();
        for (MehtaEdge edge : edges) {
            assocs.add(buildAssociation(edge));
        }
        return assocs;
    }

    private RelatedAssociationModel buildRelatedAssociation(ConnectedMehtaEdge edge) {
        RelatedAssociationModel relAssoc = new RelatedAssociationModel(
            buildAssociation(edge.getMehtaEdge()),
            buildAssociation(edge.getConnectingMehtaEdge()));
        return relAssoc;
    }

    private Set<RelatedAssociationModel> buildRelatedAssociations(Set<ConnectedMehtaEdge> edges) {
        Set<RelatedAssociationModel> relAssocs = new LinkedHashSet();
        for (ConnectedMehtaEdge edge : edges) {
            relAssocs.add(buildRelatedAssociation(edge));
        }
        return relAssocs;
    }



    // === Type Filter ===

    private void filterNodesByAssociationType(Set<ConnectedMehtaNode> nodes, String assocTypeUri) {
        filterNodesByAssociationType(nodes, Arrays.asList(assocTypeUri));
    }

    private void filterNodesByAssociationType(Set<ConnectedMehtaNode> nodes, List assocTypeUris) {
        ConnectedMehtaNode node = null;
        MehtaEdge edge = null;
        try {
            Iterator<ConnectedMehtaNode> i = nodes.iterator();
            while (i.hasNext()) {
                node = i.next();
                edge = node.getConnectingMehtaEdge();
                if (!assocTypeUris.contains(getAssociationTypeUri(edge))) {
                    i.remove();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Applying association type filter " + assocTypeUris +
                " failed (" + edge + ",\n" + node.getMehtaNode() + ")", e);
        }
    }

    private void filterNodesByTopicType(Set<ConnectedMehtaNode> nodes, String topicTypeUri) {
        MehtaNode node = null;
        try {
            Iterator<ConnectedMehtaNode> i = nodes.iterator();
            while (i.hasNext()) {
                node = i.next().getMehtaNode();
                if (!getTopicTypeUri(node).equals(topicTypeUri)) {
                    i.remove();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Applying topic type filter \"" + topicTypeUri + "\" failed (" + node + ")", e);
        }
    }

    // ---

    private void filterConnectedEdgesByAssociationType(Set<ConnectedMehtaEdge> edges, String assocTypeUri) {
        MehtaEdge edge = null;
        try {
            Iterator<ConnectedMehtaEdge> i = edges.iterator();
            while (i.hasNext()) {
                edge = i.next().getConnectingMehtaEdge();
                if (!getAssociationTypeUri(edge).equals(assocTypeUri)) {
                    i.remove();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Applying association type filter \"" + assocTypeUri +
                "\" failed (" + edge + ")", e);
        }
    }

    private void filterEdgesByAssociationType(Set<MehtaEdge> edges, String assocTypeUri) {
        MehtaEdge edge = null;
        try {
            Iterator<MehtaEdge> i = edges.iterator();
            while (i.hasNext()) {
                edge = i.next();
                if (!getAssociationTypeUri(edge).equals(assocTypeUri)) {
                    i.remove();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Applying association type filter \"" + assocTypeUri +
                "\" failed (" + edge + ")", e);
        }
    }



    // === Helper ===

    private MehtaNode lookupTopic(String uri) {
        MehtaNode topic = lookupMehtaNode(uri);
        if (topic == null) {
            throw new RuntimeException("Topic \"" + uri + "\" not found");
        }
        return topic;
    }

    private void checkRoleType(String roleTypeUri) {
        // Note: the meta meta type is not stored in the DB (in memory only). It can't be looked up from DB.
        // However, the meta meta type is used as role type to connect the meta meta type's instance (node 0)
        // with its view configuration.
        if (roleTypeUri.equals("dm4.core.meta_meta_type")) {
            return;
        }
        MehtaNode roleType = lookupMehtaNode(roleTypeUri);
        if (roleType == null) {
            throw new RuntimeException("Role type \"" + roleTypeUri + "\" not found");
        }
    }

    // ---

    /**
     * Checks if a topic with the given URI exists in the database, and if so, throws an exception.
     * If an empty string is given no check is performed.
     *
     * @param   uri     The URI to check. Must not be null.
     */
    private void checkUniqueness(String uri) {
        if (!uri.equals("") && lookupMehtaNode(uri) != null) {
            throw new RuntimeException("Topic URI \"" + uri + "\" is not unique");
        }
    }

    private MehtaNode lookupMehtaNode(String uri) {
        return mg.getMehtaNode("uri", uri);
    }

    // ---

    private void storeAndIndexUri(MehtaNode node, String uri) {
        String oldUri = node.getString("uri", null);
        node.setString("uri", uri);
        node.indexAttribute(MehtaGraphIndexMode.KEY, "uri", uri, oldUri);
    }

    private void storeAndIndexUri(MehtaEdge edge, String uri) {
        String oldUri = edge.getString("uri", null);
        edge.setString("uri", uri);
        edge.indexAttribute(MehtaGraphIndexMode.KEY, "uri", uri, oldUri);
    }

    // ---

    /**
     * Determines the topic type of a mehta node.
     *
     * @return  The topic type's URI.
     */
    private String getTopicTypeUri(MehtaNode node) {
        if (node.getString("uri").equals("dm4.core.meta_type")) {
            return "dm4.core.meta_meta_type";
        } else {
            return fetchTypeNode(node).getString("uri");
        }
    }

    /**
     * Determines the association type of a mehta edge.
     *
     * @return  The association type's URI.
     */
    private String getAssociationTypeUri(MehtaEdge edge) {
        MehtaNode typeNode = fetchTypeNode(edge);
        // typeNode is null for "dm4.core.instantiation" edges of edges
        return typeNode != null ? typeNode.getString("uri") : "";
    }

    // ---

    /**
     * Determines the topic type of a mehta node.
     *
     * @return  The mehta node that represents the topic type.
     */
    private MehtaNode fetchTypeNode(MehtaNode node) {
        ConnectedMehtaNode typeNode = node.getConnectedMehtaNode("dm4.core.instance", "dm4.core.type");
        if (typeNode == null) {
            throw new RuntimeException("No type node is connected to " + node);
        }
        return typeNode.getMehtaNode();
    }

    /**
     * Determines the association type of a mehta edge.
     *
     * @return  The mehta node that represents the association type.
     */
    private MehtaNode fetchTypeNode(MehtaEdge edge) {
        ConnectedMehtaNode typeNode = edge.getConnectedMehtaNode("dm4.core.instance", "dm4.core.type");
        if (typeNode == null) {
            return null;
            // "dm4.core.instantiation" edges of edges are deliberately not connected to a type node
            // ### throw new RuntimeException("No type node is connected to " + edge);
        }
        return typeNode.getMehtaNode();
    }

    // ---

    private MehtaObjectRole getMehtaObjectRole(RoleModel roleModel) {
        String roleTypeUri = roleModel.getRoleTypeUri();
        checkRoleType(roleTypeUri);     // sanity check
        return new MehtaObjectRole(getRoleObject(roleModel), roleTypeUri);
    }

    private List<RoleModel> getRoleModels(MehtaEdge edge) {
        List<RoleModel> roleModels = new ArrayList();
        for (MehtaObjectRole objectRole : edge.getMehtaObjects()) {
            MehtaObject mehtaObject = objectRole.getMehtaObject();
            String roleTypeUri = objectRole.getRoleType();
            long id = mehtaObject.getId();
            //
            RoleModel roleModel;
            if (mehtaObject instanceof MehtaNode) {
                roleModel = new TopicRoleModel(id, roleTypeUri);
            } else if (mehtaObject instanceof MehtaEdge) {
                roleModel = new AssociationRoleModel(id, roleTypeUri);
            } else {
                throw new RuntimeException("Unexpected MehtaObject (" + mehtaObject.getClass() + ")");
            }
            roleModels.add(roleModel);
        }
        return roleModels;
    }

    // ---

    private MehtaObject getRoleObject(RoleModel roleModel) {
        if (roleModel instanceof TopicRoleModel) {
            return getRoleNode((TopicRoleModel) roleModel);
        } else if (roleModel instanceof AssociationRoleModel) {
            return getRoleEdge((AssociationRoleModel) roleModel);
        } else {
            throw new RuntimeException("Unexpected RoleModel object (" + roleModel.getClass() + ")");
        }
    }

    private MehtaNode getRoleNode(TopicRoleModel topicRoleModel) {
        if (topicRoleModel.topicIdentifiedByUri()) {
            return lookupTopic(topicRoleModel.getTopicUri());
        } else {
            return mg.getMehtaNode(topicRoleModel.getTopicId());
        }
    }

    private MehtaEdge getRoleEdge(AssociationRoleModel assocRoleModel) {
        return mg.getMehtaEdge(assocRoleModel.getAssociationId());
    }

    // ---

    private MehtaGraphIndexMode fromIndexMode(IndexMode indexMode) {
        return MehtaGraphIndexMode.valueOf(indexMode.name());
    }

    // ---

    private void setProperty(long objectId, String key, String value) {
        mg.getMehtaObject(objectId).setString(key, value);
    }

    private String getProperty(long objectId, String key, String defaultValue) {
        return mg.getMehtaObject(objectId).getString(key, defaultValue);
    }

    // ---

    private void setupMetaTypeNode() {
        MehtaNode refNode = mg.getMehtaNode(0);
        String uri = "dm4.core.meta_type";
        refNode.setString("uri", uri);
        refNode.setString("value", "Meta Type");
        //
        refNode.indexAttribute(MehtaGraphIndexMode.KEY, "uri", uri, null);     // oldValue=null
    }
}
