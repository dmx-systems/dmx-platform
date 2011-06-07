package de.deepamehta.core.impl.storage;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaTransaction;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.impl.model.AssociationBase;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationDefinition;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.storage.DeepaMehtaStorage;

import de.deepamehta.hypergraph.ConnectedHyperEdge;
import de.deepamehta.hypergraph.ConnectedHyperNode;
import de.deepamehta.hypergraph.HyperEdge;
import de.deepamehta.hypergraph.HyperGraph;
import de.deepamehta.hypergraph.HyperGraphIndexMode;
import de.deepamehta.hypergraph.HyperNode;
import de.deepamehta.hypergraph.HyperObject;
import de.deepamehta.hypergraph.HyperObjectRole;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;



/**
 * A bridge between the DeepaMehta storage abstraction and a HyperGraph implementation.
 */
public class HGStorageBridge implements DeepaMehtaStorage {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private HyperGraph hg;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public HGStorageBridge(HyperGraph hg) {
        this.hg = hg;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Topics ===

    @Override
    public Topic getTopic(long topicId) {
        return buildTopic(hg.getHyperNode(topicId));
    }

    @Override
    public Topic getTopic(String key, TopicValue value) {
        HyperNode node = hg.getHyperNode(key, value.value());
        return node != null ? buildTopic(node) : null;
    }

    // ---

    @Override
    public RelatedTopic getTopicRelatedTopic(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                                                String othersRoleTypeUri,
                                                                                String othersTopicTypeUri) {
        Set<RelatedTopic> relTopics = getTopicRelatedTopics(topicId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
                                                                                                  othersTopicTypeUri);
        switch (relTopics.size()) {
        case 0:
            return null;
        case 1:
            return relTopics.iterator().next();
        default:
            throw new RuntimeException("Ambiguity: there are " + relTopics.size() + " related topics " + "(topicId=" +
                topicId + ", assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri + "\", " +
                "othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersTopicTypeUri=\"" + othersTopicTypeUri + "\")");
        }
    }

    @Override
    public Set<RelatedTopic> getTopicRelatedTopics(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                                                      String othersRoleTypeUri,
                                                                                      String othersTopicTypeUri) {
        Set<ConnectedHyperNode> nodes = hg.getHyperNode(topicId).getConnectedHyperNodes(myRoleTypeUri,
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
    public Set<Association> getAssociations(long topicId) {
        return getAssociations(topicId, null);
    }

    @Override
    public Set<Association> getAssociations(long topicId, String myRoleTypeUri) {
        return buildAssociations(hg.getHyperNode(topicId).getHyperEdges(myRoleTypeUri));
    }

    @Override
    public Association getTopicRelatedAssociation(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                                                     String othersRoleTypeUri) {
        // FIXME: assocTypeUri not respected
        ConnectedHyperEdge edge = hg.getHyperNode(topicId).getConnectedHyperEdge(myRoleTypeUri, othersRoleTypeUri);
        return edge != null ? buildAssociation(edge.getHyperEdge()) : null;
    }

    // ---

    @Override
    public Set<Topic> searchTopics(String searchTerm, String fieldUri, boolean wholeWord) {
        if (!wholeWord) {
            searchTerm += "*";
        }
        return buildTopics(hg.queryHyperNodes(fieldUri, searchTerm));
    }

    // ---

    @Override
    public void setTopicUri(long topicId, String uri) {
        setNodeUri(hg.getHyperNode(topicId), uri);
    }

    @Override
    public TopicValue setTopicValue(long topicId, TopicValue value) {
        Object oldValue = hg.getHyperNode(topicId).setObject("value", value.value());
        return oldValue != null ? new TopicValue(oldValue) : null;
    }

    @Override
    public void indexTopicValue(long topicId, IndexMode indexMode, String indexKey, TopicValue value,
                                                                                    TopicValue oldValue) {
        HyperGraphIndexMode hgIndexMode = fromIndexMode(indexMode);
        Object oldVal = oldValue != null ? oldValue.value() : null;
        hg.getHyperNode(topicId).indexAttribute(hgIndexMode, indexKey, value.value(), oldVal);
    }

    @Override
    public Topic createTopic(TopicModel topicModel) {
        String uri = topicModel.getUri();
        // 1) check uniqueness
        checkUniqueness(uri);
        // 2) create node
        HyperNode node = hg.createHyperNode();
        topicModel.setId(node.getId());
        // 3) set URI
        setNodeUri(node, uri);
        //
        return buildTopic(topicModel.getId(), uri, topicModel.getValue(), topicModel.getTypeUri());
    }

    @Override
    public void deleteTopic(long topicId) {
        hg.getHyperNode(topicId).delete();
    }



    // === Associations ===

    @Override
    public Association getAssociation(long assocId) {
        return buildAssociation(hg.getHyperEdge(assocId));
    }

    @Override
    public Set<Association> getAssociations(long topic1Id, long topic2Id, String assocTypeUri) {
        Set<HyperEdge> edges = hg.getHyperEdges(topic1Id, topic2Id);
        if (assocTypeUri != null) {
            filterEdgesByAssociationType(edges, assocTypeUri);
        }
        return buildAssociations(edges);
    }

    // ---

    @Override
    public RelatedTopic getAssociationRelatedTopic(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                                                      String othersRoleTypeUri,
                                                                                      String othersTopicTypeUri) {
        Set<RelatedTopic> relTopics = getAssociationRelatedTopics(assocId, assocTypeUri, myRoleTypeUri,
                                                                                         othersRoleTypeUri,
                                                                                         othersTopicTypeUri);
        switch (relTopics.size()) {
        case 0:
            return null;
        case 1:
            return relTopics.iterator().next();
        default:
            throw new RuntimeException("Ambiguity: there are " + relTopics.size() + " related topics " + "(assocId=" +
                assocId + ", assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri + "\", " +
                "othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersTopicTypeUri=\"" + othersTopicTypeUri + "\")");
        }
    }

    @Override
    public Set<RelatedTopic> getAssociationRelatedTopics(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                                                            String othersRoleTypeUri,
                                                                                            String othersTopicTypeUri) {
        Set<ConnectedHyperNode> nodes = hg.getHyperEdge(assocId).getConnectedHyperNodes(myRoleTypeUri,
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
    public Association getAssociationRelatedAssociation(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                                                           String othersRoleTypeUri) {
        // FIXME: assocTypeUri not respected
        ConnectedHyperEdge edge = hg.getHyperEdge(assocId).getConnectedHyperEdge(myRoleTypeUri, othersRoleTypeUri);
        return edge != null ? buildAssociation(edge.getHyperEdge()) : null;
    }

    // ---

    @Override
    public void setRoleTypeUri(long assocId, long objectId, String roleTypeUri) {
        hg.getHyperEdge(assocId).getHyperObject(objectId).setRoleType(roleTypeUri);
    }

    // ---

    @Override
    public Association createAssociation(AssociationModel assocModel) {
        String typeUri = assocModel.getTypeUri();
        //
        if (typeUri == null) {
            throw new IllegalArgumentException("Tried to create an association with null association type " +
                "(typeUri=null)");
        }
        //
        RoleModel roleModel1 = assocModel.getRoleModel1();
        RoleModel roleModel2 = assocModel.getRoleModel2();
        HyperEdge edge = hg.createHyperEdge(getHyperObjectRole(roleModel1), getHyperObjectRole(roleModel2));
        //
        return buildAssociation(edge.getId(), typeUri, roleModel1, roleModel2);
    }

    @Override
    public void deleteAssociation(long assocId) {
        hg.getHyperEdge(assocId).delete();
    }



    // === DB ===

    @Override
    public DeepaMehtaTransaction beginTx() {
        return new HGTransactionAdapter(hg);
    }

    @Override
    public boolean init() {
        // init core migration number
        boolean isCleanInstall = false;
        if (!hg.getHyperNode(0).hasAttribute("core_migration_nr")) {
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
        return hg.getHyperNode(0).getInteger("core_migration_nr");
    }

    @Override
    public void setMigrationNr(int migrationNr) {
        hg.getHyperNode(0).setInteger("core_migration_nr", migrationNr);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Build Topics from HyperNodes ===

    private Topic buildTopic(HyperNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Tried to build a Topic from a null HyperNode");
        }
        //
        return buildTopic(node.getId(), node.getString("uri"), new TopicValue(node.getObject("value")),
            getTopicTypeUri(node));
    }

    private Topic buildTopic(long id, String uri, TopicValue value, String typeUri) {
        return new HGTopic(id, uri, value, typeUri, null);   // composite=null
    }

    // ---

    private Set<Topic> buildTopics(List<HyperNode> nodes) {
        Set<Topic> topics = new LinkedHashSet();
        for (HyperNode node : nodes) {
            topics.add(buildTopic(node));
        }
        return topics;
    }

    // ---

    private RelatedTopic buildRelatedTopic(ConnectedHyperNode node) {
        RelatedTopic relTopic = new HGRelatedTopic(buildTopic(node.getHyperNode()));
        relTopic.setAssociation(buildAssociation(node.getConnectingHyperEdge()));
        return relTopic;
    }

    private Set<RelatedTopic> buildRelatedTopics(Set<ConnectedHyperNode> nodes) {
        Set<RelatedTopic> topics = new LinkedHashSet();
        for (ConnectedHyperNode node : nodes) {
            topics.add(buildRelatedTopic(node));
        }
        return topics;
    }



    // === Build Associations from HyperEdges ===

    private Association buildAssociation(HyperEdge edge) {
        if (edge == null) {
            throw new IllegalArgumentException("Tried to build an Association from a null HyperEdge");
        }
        //
        List<RoleModel> roleModels = getRoleModels(edge);
        return buildAssociation(edge.getId(), getAssociationTypeUri(edge), roleModels.get(0), roleModels.get(1));
    }

    private Association buildAssociation(long id, String typeUri, RoleModel roleModel1, RoleModel roleModel2) {
        return new HGAssociation(id, typeUri, roleModel1, roleModel2);
    }

    // ---

    private Set<Association> buildAssociations(Iterable<HyperEdge> edges) {
        Set<Association> assocs = new HashSet();
        for (HyperEdge edge : edges) {
            assocs.add(buildAssociation(edge));
        }
        return assocs;
    }



    // === Type Filter ===

    private void filterNodesByAssociationType(Set<ConnectedHyperNode> nodes, String assocTypeUri) {
        ConnectedHyperNode node = null;
        HyperEdge edge = null;
        try {
            Iterator<ConnectedHyperNode> i = nodes.iterator();
            while (i.hasNext()) {
                node = i.next();
                edge = node.getConnectingHyperEdge();
                if (!getAssociationTypeUri(edge).equals(assocTypeUri)) {
                    i.remove();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Applying association type filter \"" + assocTypeUri +
                "\" failed (" + edge + ",\n" + node.getHyperNode() + ")", e);
        }
    }

    private void filterNodesByTopicType(Set<ConnectedHyperNode> nodes, String topicTypeUri) {
        HyperNode node = null;
        try {
            Iterator<ConnectedHyperNode> i = nodes.iterator();
            while (i.hasNext()) {
                node = i.next().getHyperNode();
                if (!getTopicTypeUri(node).equals(topicTypeUri)) {
                    i.remove();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Applying topic type filter \"" + topicTypeUri + "\" failed (" + node + ")", e);
        }
    }

    private void filterEdgesByAssociationType(Set<HyperEdge> edges, String assocTypeUri) {
        HyperEdge edge = null;
        try {
            Iterator<HyperEdge> i = edges.iterator();
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

    private HyperNode lookupTopic(String uri) {
        HyperNode topic = lookupHyperNode(uri);
        if (topic == null) {
            throw new RuntimeException("Topic \"" + uri + "\" not found");
        }
        return topic;
    }

    /* FIXME: not used
    private HyperNode lookupTopicType(String topicTypeUri) {
        HyperNode topicType = lookupHyperNode(topicTypeUri);
        if (topicType == null) {
            throw new RuntimeException("Topic type \"" + topicTypeUri + "\" not found");
        }
        return topicType;
    } */

    /* FIXME: not used
    private HyperNode lookupAssociationType(String assocTypeUri) {
        HyperNode assocType = lookupHyperNode(assocTypeUri);
        if (assocType == null) {
            throw new RuntimeException("Association type \"" + assocTypeUri + "\" not found");
        }
        return assocType;
    } */

    private HyperNode lookupRoleType(String roleTypeUri) {
        HyperNode roleType = lookupHyperNode(roleTypeUri);
        if (roleType == null) {
            throw new RuntimeException("Role type \"" + roleTypeUri + "\" not found");
        }
        return roleType;
    }

    // ---

    /**
     * Throws an exception if there is a topic with the given URI in the database.
     * If an empty string is given no check is performed.
     *
     * @param   uri     The URI to check. Must not be null.
     */
    private void checkUniqueness(String uri) {
        if (!uri.equals("") && lookupHyperNode(uri) != null) {
            throw new RuntimeException("Topic URI \"" + uri + "\" is not unique");
        }
    }

    private HyperNode lookupHyperNode(String uri) {
        return hg.getHyperNode("uri", uri);
    }

    // ---

    private void setNodeUri(HyperNode node, String uri) {
        String oldUri = node.getString("uri", null);
        node.setString("uri", uri);
        node.indexAttribute(HyperGraphIndexMode.KEY, "uri", uri, oldUri);
    }

    // ---

    /**
     * Determines the topic type of a hyper node.
     *
     * @return  The topic type's URI.
     */
    private String getTopicTypeUri(HyperNode node) {
        if (node.getString("uri").equals("dm3.core.meta_type")) {
            return "dm3.core.meta_meta_type";
        } else {
            return fetchTypeNode(node).getString("uri");
        }
    }

    /**
     * Determines the association type of a hyper edge.
     *
     * @return  The association type's URI.
     */
    private String getAssociationTypeUri(HyperEdge edge) {
        HyperNode typeNode = fetchTypeNode(edge);
        // typeNode is null for "dm3.core.instantiation" edges of edges
        return typeNode != null ? typeNode.getString("uri") : "";
    }

    // ---

    /**
     * Determines the topic type of a hyper node.
     *
     * @return  The hyper node that represents the topic type.
     */
    private HyperNode fetchTypeNode(HyperNode node) {
        ConnectedHyperNode typeNode = node.getConnectedHyperNode("dm3.core.instance", "dm3.core.type");
        if (typeNode == null) {
            throw new RuntimeException("No type node is connected to " + node);
        }
        return typeNode.getHyperNode();
    }

    /**
     * Determines the association type of a hyper edge.
     *
     * @return  The hyper node that represents the association type.
     */
    private HyperNode fetchTypeNode(HyperEdge edge) {
        ConnectedHyperNode typeNode = edge.getConnectedHyperNode("dm3.core.instance", "dm3.core.type");
        if (typeNode == null) {
            return null;
            // "dm3.core.instantiation" edges of edges are deliberately not connected to a type node
            // ### throw new RuntimeException("No type node is connected to " + edge);
        }
        return typeNode.getHyperNode();
    }

    // ---

    private HyperObjectRole getHyperObjectRole(RoleModel roleModel) {
        String roleTypeUri = roleModel.getRoleTypeUri();
        lookupRoleType(roleTypeUri);    // consistency check
        return new HyperObjectRole(getRoleObject(roleModel), roleTypeUri);
    }

    private List<RoleModel> getRoleModels(HyperEdge edge) {
        List<RoleModel> roleModels = new ArrayList();
        for (HyperObjectRole objectRole : edge.getHyperObjects()) {
            HyperObject hyperObject = objectRole.getHyperObject();
            String roleTypeUri = objectRole.getRoleType();
            long id = hyperObject.getId();
            //
            RoleModel roleModel;
            if (hyperObject instanceof HyperNode) {
                roleModel = new TopicRoleModel(id, roleTypeUri);
            } else if (hyperObject instanceof HyperEdge) {
                roleModel = new AssociationRoleModel(id, roleTypeUri);
            } else {
                throw new RuntimeException("Unexpected HyperObject (" + hyperObject.getClass() + ")");
            }
            roleModels.add(roleModel);
        }
        return roleModels;
    }

    // ---

    private HyperObject getRoleObject(RoleModel roleModel) {
        if (roleModel instanceof TopicRoleModel) {
            return getRoleNode((TopicRoleModel) roleModel);
        } else if (roleModel instanceof AssociationRoleModel) {
            return getRoleEdge((AssociationRoleModel) roleModel);
        } else {
            throw new RuntimeException("Unexpected RoleModel object (" + roleModel.getClass() + ")");
        }
    }

    private HyperNode getRoleNode(TopicRoleModel topicRoleModel) {
        if (topicRoleModel.topicIdentifiedByUri()) {
            return lookupTopic(topicRoleModel.getTopicUri());
        } else {
            return hg.getHyperNode(topicRoleModel.getTopicId());
        }
    }

    private HyperEdge getRoleEdge(AssociationRoleModel assocRoleModel) {
        return hg.getHyperEdge(assocRoleModel.getAssociationId());
    }

    // ---

    private HyperGraphIndexMode fromIndexMode(IndexMode indexMode) {
        return HyperGraphIndexMode.valueOf(indexMode.name());
    }

    // ---

    private void setupMetaTypeNode() {
        HyperNode refNode = hg.getHyperNode(0);
        String uri = "dm3.core.meta_type";
        refNode.setString("uri", uri);
        refNode.setString("value", "Meta Type");
        //
        refNode.indexAttribute(HyperGraphIndexMode.KEY, "uri", uri, null);     // oldValue=null
    }
}
