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
import de.deepamehta.core.storage.DeepaMehtaStorage;

import de.deepamehta.mehtagraph.ConnectedMehtaEdge;
import de.deepamehta.mehtagraph.ConnectedMehtaNode;
import de.deepamehta.mehtagraph.MehtaEdge;
import de.deepamehta.mehtagraph.MehtaGraph;
import de.deepamehta.mehtagraph.MehtaGraphIndexMode;
import de.deepamehta.mehtagraph.MehtaNode;
import de.deepamehta.mehtagraph.MehtaObject;
import de.deepamehta.mehtagraph.MehtaObjectRole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;



/**
 * A DeepaMehta storage implementation by the means of a MehtaGraph implementation.
 * <p>
 * The DeepaMehta service knows nothing about a MehtaGraph and a MehtaGraph knows nothing about DeepaMehta.
 * This class bridges between them.
 */
public class MGStorageBridge implements DeepaMehtaStorage {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private MehtaGraph mg;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public MGStorageBridge(MehtaGraph mg) {
        this.mg = mg;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Topics ===

    @Override
    public TopicModel getTopic(long topicId) {
        return buildTopic(mg.getMehtaNode(topicId));
    }

    @Override
    public TopicModel getTopic(String key, SimpleValue value) {
        MehtaNode node = mg.getMehtaNode(key, value.value());
        return node != null ? buildTopic(node) : null;
    }

    // ---

    @Override
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

    @Override
    public ResultSet<RelatedTopicModel> getTopicRelatedTopics(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                              String othersRoleTypeUri, String othersTopicTypeUri,
                                                              int maxResultSize) {
        List<String> assocTypeUris = assocTypeUri != null ? Arrays.asList(assocTypeUri) : null;
        return getTopicRelatedTopics(topicId, assocTypeUris, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri,
            maxResultSize);
    }

    @Override
    public ResultSet<RelatedTopicModel> getTopicRelatedTopics(long topicId, List<String> assocTypeUris, String myRoleTypeUri,
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

    @Override
    public Set<AssociationModel> getTopicAssociations(long topicId, String myRoleTypeUri) {
        return buildAssociations(mg.getMehtaNode(topicId).getMehtaEdges(myRoleTypeUri));
    }

    @Override
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

    @Override
    public Set<TopicModel> searchTopics(String searchTerm, String fieldUri) {
        return buildTopics(mg.queryMehtaNodes(fieldUri, searchTerm));
    }

    // ---

    @Override
    public void setTopicUri(long topicId, String uri) {
        storeAndIndexUri(mg.getMehtaNode(topicId), uri);
    }

    @Override
    public SimpleValue setTopicValue(long topicId, SimpleValue value) {
        Object oldValue = mg.getMehtaNode(topicId).setObject("value", value.value());
        return oldValue != null ? new SimpleValue(oldValue) : null;
    }

    @Override
    public void indexTopicValue(long topicId, IndexMode indexMode, String indexKey, SimpleValue value,
                                                                                    SimpleValue oldValue) {
        MehtaGraphIndexMode mgIndexMode = fromIndexMode(indexMode);
        Object oldVal = oldValue != null ? oldValue.value() : null;
        mg.getMehtaNode(topicId).indexAttribute(mgIndexMode, indexKey, value.value(), oldVal);
    }

    @Override
    public void createTopic(TopicModel topicModel) {
        String uri = topicModel.getUri();
        checkUniqueness(uri);
        // 1) update DB
        MehtaNode node = mg.createMehtaNode();
        storeAndIndexUri(node, uri);
        // Note: an initial topic value is needed.
        // Consider this case: in the POST_CREATE_ASSOCIATION listener a plugin fetches both of the associated
        // topics (the Access Control plugin does). If that association is created in the course of storing the
        // topic's composite value, that very topic doesn't have a simple value yet (it is known only once storing
        // the composite value is complete). Fetching the topic would fail.
        node.setString("value", "");
        // 2) update model
        topicModel.setId(node.getId());
    }

    @Override
    public void deleteTopic(long topicId) {
        mg.getMehtaNode(topicId).delete();
    }



    // === Associations ===

    @Override
    public AssociationModel getAssociation(long assocId) {
        return buildAssociation(mg.getMehtaEdge(assocId));
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public ResultSet<RelatedTopicModel> getAssociationRelatedTopics(long assocId, String assocTypeUri,
                                                                    String myRoleTypeUri, String othersRoleTypeUri,
                                                                    String othersTopicTypeUri, int maxResultSize) {
        List<String> assocTypeUris = assocTypeUri != null ? Arrays.asList(assocTypeUri) : null;
        return getAssociationRelatedTopics(assocId, assocTypeUris, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri,
            maxResultSize);
    }

    @Override
    public ResultSet<RelatedTopicModel> getAssociationRelatedTopics(long assocId, List<String> assocTypeUris,
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

    @Override
    public Set<AssociationModel> getAssociationAssociations(long assocId, String myRoleTypeUri) {
        return buildAssociations(mg.getMehtaEdge(assocId).getMehtaEdges(myRoleTypeUri));
    }

    @Override
    public RelatedAssociationModel getAssociationRelatedAssociation(long assocId, String assocTypeUri,
                                                                    String myRoleTypeUri, String othersRoleTypeUri) {
        // FIXME: assocTypeUri not respected
        ConnectedMehtaEdge edge = mg.getMehtaEdge(assocId).getConnectedMehtaEdge(myRoleTypeUri, othersRoleTypeUri);
        return edge != null ? buildRelatedAssociation(edge) : null;
    }

    // ---

    @Override
    public void setRoleTypeUri(long assocId, long objectId, String roleTypeUri) {
        mg.getMehtaEdge(assocId).getMehtaObject(objectId).setRoleType(roleTypeUri);
    }

    // ---

    @Override
    public void setAssociationUri(long assocId, String uri) {
        storeAndIndexUri(mg.getMehtaEdge(assocId), uri);
    }

    @Override
    public SimpleValue setAssociationValue(long assocId, SimpleValue value) {
        Object oldValue = mg.getMehtaEdge(assocId).setObject("value", value.value());
        return oldValue != null ? new SimpleValue(oldValue) : null;
    }

    @Override
    public void indexAssociationValue(long assocId, IndexMode indexMode, String indexKey, SimpleValue value,
                                                                                          SimpleValue oldValue) {
        MehtaGraphIndexMode mgIndexMode = fromIndexMode(indexMode);
        Object oldVal = oldValue != null ? oldValue.value() : null;
        mg.getMehtaEdge(assocId).indexAttribute(mgIndexMode, indexKey, value.value(), oldVal);
    }

    // ---

    @Override
    public void createAssociation(AssociationModel assocModel) {
        // error check
        if (assocModel.getTypeUri() == null) {
            throw new IllegalArgumentException("Tried to create an association with null association type " +
                "(typeUri=null)");
        }
        //
        MehtaEdge edge = mg.createMehtaEdge(
            getMehtaObjectRole(assocModel.getRoleModel1()),
            getMehtaObjectRole(assocModel.getRoleModel2()));
        assocModel.setId(edge.getId());
    }

    @Override
    public void deleteAssociation(long assocId) {
        mg.getMehtaEdge(assocId).delete();
    }



    // === DB ===

    @Override
    public DeepaMehtaTransaction beginTx() {
        return new MGTransactionAdapter(mg);
    }

    @Override
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

    @Override
    public void shutdown() {
        mg.shutdown();
    }

    @Override
    public int getMigrationNr() {
        return mg.getMehtaNode(0).getInteger("core_migration_nr");
    }

    @Override
    public void setMigrationNr(int migrationNr) {
        mg.getMehtaNode(0).setInteger("core_migration_nr", migrationNr);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Build Topic models from MehtaNodes ===

    private TopicModel buildTopic(MehtaNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Tried to build a TopicModel from a null MehtaNode");
        }
        //
        long id = node.getId();
        String uri = node.getString("uri");
        String typeUri = getTopicTypeUri(node);
        SimpleValue value = new SimpleValue(node.getObject("value"));
        return new TopicModel(id, uri, typeUri, value, null);   // composite=null
    }

    private Set<TopicModel> buildTopics(List<MehtaNode> nodes) {
        Set<TopicModel> topics = new LinkedHashSet<TopicModel>();
        for (MehtaNode node : nodes) {
            topics.add(buildTopic(node));
        }
        return topics;
    }

    private RelatedTopicModel buildRelatedTopic(ConnectedMehtaNode node) {
        RelatedTopicModel relTopic = new RelatedTopicModel(
            buildTopic(node.getMehtaNode()),
            buildAssociation(node.getConnectingMehtaEdge()));
        return relTopic;
    }

    private ResultSet<RelatedTopicModel> buildRelatedTopics(Set<ConnectedMehtaNode> nodes, int maxResultSize) {
        Set<RelatedTopicModel> relTopics = new LinkedHashSet<RelatedTopicModel>();
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
        if (edge == null) {
            throw new IllegalArgumentException("Tried to build an AssociationModel from a null MehtaEdge");
        }
        //
        long id = edge.getId();
        String typeUri = getAssociationTypeUri(edge);
        List<RoleModel> roleModels = getRoleModels(edge);
        return new AssociationModel(id, typeUri, roleModels.get(0), roleModels.get(1));
    }

    private Set<AssociationModel> buildAssociations(Iterable<MehtaEdge> edges) {
        Set<AssociationModel> assocs = new LinkedHashSet<AssociationModel>();
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
        Set<RelatedAssociationModel> relAssocs = new LinkedHashSet<RelatedAssociationModel>();
        for (ConnectedMehtaEdge edge : edges) {
            relAssocs.add(buildRelatedAssociation(edge));
        }
        return relAssocs;
    }



    // === Type Filter ===

    @SuppressWarnings("unused")
    private void filterNodesByAssociationType(Set<ConnectedMehtaNode> nodes, String assocTypeUri) {
        filterNodesByAssociationType(nodes, Arrays.asList(assocTypeUri));
    }

    private void filterNodesByAssociationType(Set<ConnectedMehtaNode> nodes, List<String> assocTypeUris) {
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
        List<RoleModel> roleModels = new ArrayList<RoleModel>();
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

    private void setupMetaTypeNode() {
        MehtaNode refNode = mg.getMehtaNode(0);
        String uri = "dm4.core.meta_type";
        refNode.setString("uri", uri);
        refNode.setString("value", "Meta Type");
        //
        refNode.indexAttribute(MehtaGraphIndexMode.KEY, "uri", uri, null);     // oldValue=null
    }
}
