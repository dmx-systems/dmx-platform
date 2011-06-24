package de.deepamehta.core.impl.storage;

import de.deepamehta.core.DeepaMehtaTransaction;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TopicValue;
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
import java.util.HashSet;
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

    private MehtaGraph hg;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public MGStorageBridge(MehtaGraph hg) {
        this.hg = hg;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Topics ===

    @Override
    public TopicModel getTopic(long topicId) {
        return buildTopic(hg.getMehtaNode(topicId));
    }

    @Override
    public TopicModel getTopic(String key, TopicValue value) {
        MehtaNode node = hg.getMehtaNode(key, value.value());
        return node != null ? buildTopic(node) : null;
    }

    // ---

    @Override
    public Set<RelatedTopicModel> getTopicRelatedTopics(long topicId, List assocTypeUris, String myRoleTypeUri,
                                                                                          String othersRoleTypeUri,
                                                                                          String othersTopicTypeUri) {
        Set<ConnectedMehtaNode> nodes = hg.getMehtaNode(topicId).getConnectedMehtaNodes(myRoleTypeUri,
                                                                                        othersRoleTypeUri);
        if (othersTopicTypeUri != null) {
            filterNodesByTopicType(nodes, othersTopicTypeUri);
        }
        if (assocTypeUris != null) {
            filterNodesByAssociationType(nodes, assocTypeUris);
        }
        return buildRelatedTopics(nodes);
    }

    // ---

    @Override
    public Set<AssociationModel> getAssociations(long topicId) {
        return getAssociations(topicId, null);
    }

    @Override
    public Set<AssociationModel> getAssociations(long topicId, String myRoleTypeUri) {
        return buildAssociations(hg.getMehtaNode(topicId).getMehtaEdges(myRoleTypeUri));
    }

    @Override
    public RelatedAssociationModel getTopicRelatedAssociation(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                              String othersRoleTypeUri) {
        // FIXME: assocTypeUri not respected
        ConnectedMehtaEdge edge = hg.getMehtaNode(topicId).getConnectedMehtaEdge(myRoleTypeUri, othersRoleTypeUri);
        return edge != null ? buildRelatedAssociation(edge) : null;
    }

    // ---

    @Override
    public Set<TopicModel> searchTopics(String searchTerm, String fieldUri, boolean wholeWord) {
        if (!wholeWord) {
            searchTerm += "*";
        }
        return buildTopics(hg.queryMehtaNodes(fieldUri, searchTerm));
    }

    // ---

    @Override
    public void setTopicUri(long topicId, String uri) {
        setNodeUri(hg.getMehtaNode(topicId), uri);
    }

    @Override
    public TopicValue setTopicValue(long topicId, TopicValue value) {
        Object oldValue = hg.getMehtaNode(topicId).setObject("value", value.value());
        return oldValue != null ? new TopicValue(oldValue) : null;
    }

    @Override
    public void indexTopicValue(long topicId, IndexMode indexMode, String indexKey, TopicValue value,
                                                                                    TopicValue oldValue) {
        MehtaGraphIndexMode hgIndexMode = fromIndexMode(indexMode);
        Object oldVal = oldValue != null ? oldValue.value() : null;
        hg.getMehtaNode(topicId).indexAttribute(hgIndexMode, indexKey, value.value(), oldVal);
    }

    @Override
    public void createTopic(TopicModel topicModel) {
        String uri = topicModel.getUri();
        // 1) check uniqueness
        checkUniqueness(uri);
        // 2) create node
        MehtaNode node = hg.createMehtaNode();
        topicModel.setId(node.getId());
        // 3) set URI
        setNodeUri(node, uri);
    }

    @Override
    public void deleteTopic(long topicId) {
        hg.getMehtaNode(topicId).delete();
    }



    // === Associations ===

    @Override
    public AssociationModel getAssociation(long assocId) {
        return buildAssociation(hg.getMehtaEdge(assocId));
    }

    @Override
    public Set<AssociationModel> getAssociations(long topic1Id, long topic2Id, String assocTypeUri) {
        Set<MehtaEdge> edges = hg.getMehtaEdges(topic1Id, topic2Id);
        if (assocTypeUri != null) {
            filterEdgesByAssociationType(edges, assocTypeUri);
        }
        return buildAssociations(edges);
    }

    // ---

    @Override
    public Set<RelatedTopicModel> getAssociationRelatedTopics(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                              String othersRoleTypeUri, String othersTopicTypeUri) {
        Set<ConnectedMehtaNode> nodes = hg.getMehtaEdge(assocId).getConnectedMehtaNodes(myRoleTypeUri,
                                                                                        othersRoleTypeUri);
        if (othersTopicTypeUri != null) {
            filterNodesByTopicType(nodes, othersTopicTypeUri);
        }
        if (assocTypeUri != null) {
            filterNodesByAssociationType(nodes, assocTypeUri);
        }
        return buildRelatedTopics(nodes);
    }

    // ---

    @Override
    public RelatedAssociationModel getAssociationRelatedAssociation(long assocId, String assocTypeUri,
                                                                    String myRoleTypeUri, String othersRoleTypeUri) {
        // FIXME: assocTypeUri not respected
        ConnectedMehtaEdge edge = hg.getMehtaEdge(assocId).getConnectedMehtaEdge(myRoleTypeUri, othersRoleTypeUri);
        return edge != null ? buildRelatedAssociation(edge) : null;
    }

    // ---

    @Override
    public void setRoleTypeUri(long assocId, long objectId, String roleTypeUri) {
        hg.getMehtaEdge(assocId).getMehtaObject(objectId).setRoleType(roleTypeUri);
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
        MehtaEdge edge = hg.createMehtaEdge(
            getMehtaObjectRole(assocModel.getRoleModel1()),
            getMehtaObjectRole(assocModel.getRoleModel2()));
        assocModel.setId(edge.getId());
    }

    @Override
    public void deleteAssociation(long assocId) {
        hg.getMehtaEdge(assocId).delete();
    }



    // === DB ===

    @Override
    public DeepaMehtaTransaction beginTx() {
        return new MGTransactionAdapter(hg);
    }

    @Override
    public boolean init() {
        // init core migration number
        boolean isCleanInstall = false;
        if (!hg.getMehtaNode(0).hasAttribute("core_migration_nr")) {
            logger.info("Starting with a fresh DB -- Setting migration number to 0");
            setMigrationNr(0);
            setupMetaTypeNode();
            isCleanInstall = true;
        }
        return isCleanInstall;
    }

    @Override
    public void shutdown() {
        hg.shutdown();
    }

    @Override
    public int getMigrationNr() {
        return hg.getMehtaNode(0).getInteger("core_migration_nr");
    }

    @Override
    public void setMigrationNr(int migrationNr) {
        hg.getMehtaNode(0).setInteger("core_migration_nr", migrationNr);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Build Topics from MehtaNodes ===

    private TopicModel buildTopic(MehtaNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Tried to build a TopicModel from a null MehtaNode");
        }
        //
        return buildTopic(node.getId(), node.getString("uri"), new TopicValue(node.getObject("value")),
            getTopicTypeUri(node));
    }

    private TopicModel buildTopic(long id, String uri, TopicValue value, String typeUri) {
        return new TopicModel(id, uri, value, typeUri, null);   // composite=null
    }

    // ---

    private Set<TopicModel> buildTopics(List<MehtaNode> nodes) {
        Set<TopicModel> topics = new LinkedHashSet();
        for (MehtaNode node : nodes) {
            topics.add(buildTopic(node));
        }
        return topics;
    }

    // ---

    private RelatedTopicModel buildRelatedTopic(ConnectedMehtaNode node) {
        RelatedTopicModel relTopic = new RelatedTopicModel(
            buildTopic(node.getMehtaNode()),
            buildAssociation(node.getConnectingMehtaEdge()));
        return relTopic;
    }

    private Set<RelatedTopicModel> buildRelatedTopics(Set<ConnectedMehtaNode> nodes) {
        Set<RelatedTopicModel> relTopics = new LinkedHashSet();
        for (ConnectedMehtaNode node : nodes) {
            relTopics.add(buildRelatedTopic(node));
        }
        return relTopics;
    }



    // === Build Associations from MehtaEdges ===

    private AssociationModel buildAssociation(MehtaEdge edge) {
        if (edge == null) {
            throw new IllegalArgumentException("Tried to build an AssociationModel from a null MehtaEdge");
        }
        //
        List<RoleModel> roleModels = getRoleModels(edge);
        return buildAssociation(edge.getId(), getAssociationTypeUri(edge), roleModels.get(0), roleModels.get(1));
    }

    private AssociationModel buildAssociation(long id, String typeUri, RoleModel roleModel1, RoleModel roleModel2) {
        return new AssociationModel(id, typeUri, roleModel1, roleModel2);
    }

    // ---

    private Set<AssociationModel> buildAssociations(Iterable<MehtaEdge> edges) {
        Set<AssociationModel> assocs = new LinkedHashSet();
        for (MehtaEdge edge : edges) {
            assocs.add(buildAssociation(edge));
        }
        return assocs;
    }

    // ---

    private RelatedAssociationModel buildRelatedAssociation(ConnectedMehtaEdge edge) {
        RelatedAssociationModel relAssoc = new RelatedAssociationModel(
            buildAssociation(edge.getMehtaEdge()),
            buildAssociation(edge.getConnectingMehtaEdge()));
        return relAssoc;
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

    // ---

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
        if (roleTypeUri.equals("dm3.core.meta_meta_type")) {
            return;
        }
        MehtaNode roleType = lookupMehtaNode(roleTypeUri);
        if (roleType == null) {
            throw new RuntimeException("Role type \"" + roleTypeUri + "\" not found");
        }
    }

    // ---

    /**
     * Throws an exception if there is a topic with the given URI in the database.
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
        return hg.getMehtaNode("uri", uri);
    }

    // ---

    private void setNodeUri(MehtaNode node, String uri) {
        String oldUri = node.getString("uri", null);
        node.setString("uri", uri);
        node.indexAttribute(MehtaGraphIndexMode.KEY, "uri", uri, oldUri);
    }

    // ---

    /**
     * Determines the topic type of a mehta node.
     *
     * @return  The topic type's URI.
     */
    private String getTopicTypeUri(MehtaNode node) {
        if (node.getString("uri").equals("dm3.core.meta_type")) {
            return "dm3.core.meta_meta_type";
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
        // typeNode is null for "dm3.core.instantiation" edges of edges
        return typeNode != null ? typeNode.getString("uri") : "";
    }

    // ---

    /**
     * Determines the topic type of a mehta node.
     *
     * @return  The mehta node that represents the topic type.
     */
    private MehtaNode fetchTypeNode(MehtaNode node) {
        ConnectedMehtaNode typeNode = node.getConnectedMehtaNode("dm3.core.instance", "dm3.core.type");
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
        ConnectedMehtaNode typeNode = edge.getConnectedMehtaNode("dm3.core.instance", "dm3.core.type");
        if (typeNode == null) {
            return null;
            // "dm3.core.instantiation" edges of edges are deliberately not connected to a type node
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
            return hg.getMehtaNode(topicRoleModel.getTopicId());
        }
    }

    private MehtaEdge getRoleEdge(AssociationRoleModel assocRoleModel) {
        return hg.getMehtaEdge(assocRoleModel.getAssociationId());
    }

    // ---

    private MehtaGraphIndexMode fromIndexMode(IndexMode indexMode) {
        return MehtaGraphIndexMode.valueOf(indexMode.name());
    }

    // ---

    private void setupMetaTypeNode() {
        MehtaNode refNode = hg.getMehtaNode(0);
        String uri = "dm3.core.meta_type";
        refNode.setString("uri", uri);
        refNode.setString("value", "Meta Type");
        //
        refNode.indexAttribute(MehtaGraphIndexMode.KEY, "uri", uri, null);     // oldValue=null
    }
}
