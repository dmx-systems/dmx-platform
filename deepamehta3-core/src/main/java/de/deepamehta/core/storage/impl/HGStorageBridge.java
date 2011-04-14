package de.deepamehta.core.storage.impl;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.AssociationData;
import de.deepamehta.core.model.AssociationDefinition;
import de.deepamehta.core.model.AssociationRole;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicData;
import de.deepamehta.core.model.TopicRole;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.storage.DeepaMehtaStorage;
import de.deepamehta.core.storage.DeepaMehtaTransaction;

import de.deepamehta.hypergraph.ConnectedHyperEdge;
import de.deepamehta.hypergraph.ConnectedHyperNode;
import de.deepamehta.hypergraph.HyperEdge;
import de.deepamehta.hypergraph.HyperEdgeRole;
import de.deepamehta.hypergraph.HyperGraph;
import de.deepamehta.hypergraph.HyperNode;
import de.deepamehta.hypergraph.HyperNodeRole;
import de.deepamehta.hypergraph.IndexMode;

import java.util.HashSet;
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
    public Set<Topic> getRelatedTopics(long topicId, String assocTypeUri) {
        return getRelatedTopics(topicId, assocTypeUri, null, null);
    }

    // ---

    @Override
    public Topic getRelatedTopic(long topicId, String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri) {
        Set<Topic> topics = getRelatedTopics(topicId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri);
        switch (topics.size()) {
        case 0:
            return null;
        case 1:
            return topics.iterator().next();
        default:
            throw new RuntimeException("Ambiguity: there are " + topics.size() + " related topics " +
                "(topicId=" + topicId + ", assocTypeUri=\"" + assocTypeUri +
                "\", myRoleTypeUri=\"" + myRoleTypeUri + "\", othersRoleTypeUri=\"" + othersRoleTypeUri + "\")");
        }
    }

    @Override
    public Set<Topic> getRelatedTopics(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                                          String othersRoleTypeUri) {
        Set<ConnectedHyperNode> nodes = hg.getHyperNode(topicId).getConnectedHyperNodes(myRoleTypeUri,
                                                                                        othersRoleTypeUri);
        applyAssocTypeFilter(nodes, assocTypeUri);
        //
        Set<Topic> topics = new HashSet();
        for (ConnectedHyperNode node : nodes) {
            topics.add(buildTopic(node.getHyperNode()));
        }
        return topics;
    }

    // ---

    @Override
    public Set<Association> getAssociations(long topicId, String myRoleTypeUri) {
        Set<Association> assocs = new HashSet();
        for (HyperEdge edge : hg.getHyperNode(topicId).getHyperEdges(myRoleTypeUri)) {
            assocs.add(buildAssociation(edge));
        }
        return assocs;
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
    public void setTopicValue(long topicId, TopicValue value) {
        hg.getHyperNode(topicId).setAttribute("value", value.value());
    }

    @Override
    public Topic createTopic(TopicData topicData) {
        String uri = topicData.getUri();
        TopicValue value = topicData.getValue();
        String typeUri = topicData.getTypeUri();
        // 1) check uniqueness
        checkUniqueness(uri);
        // 2) create node
        HyperNode node = hg.createHyperNode();
        node.setAttribute("uri", uri, IndexMode.KEY);
        node.setAttribute("value", value.value());
        // 3) connect to type node
        connectNodeToTypeNode(node, typeUri);
        //
        return buildTopic(node.getId(), uri, value, typeUri);
    }

    // === Associations ===

    @Override
    public Association getAssociationRelatedAssociation(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                                                           String othersRoleTypeUri) {
        // FIXME: assocTypeUri not respected
        ConnectedHyperEdge edge = hg.getHyperEdge(assocId).getConnectedHyperEdge(myRoleTypeUri, othersRoleTypeUri);
        return edge != null ? buildAssociation(edge.getHyperEdge()) : null;
    }

    // ---

    @Override
    public Association createAssociation(AssociationData assocData) {
        String typeUri = assocData.getTypeUri();
        Set<TopicRole> topicRoles = assocData.getTopicRoles();
        Set<AssociationRole> assocRoles = assocData.getAssociationRoles();
        //
        if (typeUri == null) {
            throw new IllegalArgumentException("Tried to create an association with null association type " +
                "(typeUri=null)");
        }
        // 1) create edge
        HyperEdge edge = hg.createHyperEdge();
        for (TopicRole topicRole : topicRoles) {
            addTopicToEdge(edge, topicRole);
        }
        for (AssociationRole assocRole : assocRoles) {
            addAssociationToEdge(edge, assocRole);
        }
        // 2) connect to type node
        connectEdgeToTypeNode(edge, typeUri);
        //
        return buildAssociation(edge.getId(), typeUri, topicRoles, assocRoles);
    }

    @Override
    public void addTopicToAssociation(long assocId, TopicRole topicRole) {
        addTopicToEdge(hg.getHyperEdge(assocId), topicRole);
    }

    @Override
    public void addAssociationToAssociation(long assocId, AssociationRole assocRole) {
        addAssociationToEdge(hg.getHyperEdge(assocId), assocRole);
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
        hg.getHyperNode(0).setAttribute("core_migration_nr", migrationNr);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private Topic buildTopic(HyperNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Tried to build a Topic from a null HyperNode");
        }
        //
        return buildTopic(node.getId(), node.getString("uri"), new TopicValue(node.get("value")),
            getTopicTypeUri(node));
    }

    private Topic buildTopic(long id, String uri, TopicValue value, String typeUri) {
        return new HGTopic(id, uri, value, typeUri, null);   // composite=null
    }

    // ---

    private Association buildAssociation(HyperEdge edge) {
        if (edge == null) {
            throw new IllegalArgumentException("Tried to build an Association from a null HyperEdge");
        }
        //
        return buildAssociation(edge.getId(), getAssociationTypeUri(edge), getTopicRoles(edge),
                                                                           getAssociationRoles(edge));
    }

    private Association buildAssociation(long id, String typeUri, Set<TopicRole> topicRoles,
                                                                  Set<AssociationRole> assocRoles) {
        return new HGAssociation(id, typeUri, topicRoles, assocRoles);
    }

    // ---

    private void applyAssocTypeFilter(Set<ConnectedHyperNode> nodes, String assocTypeUri) {
        ConnectedHyperNode n = null;
        HyperEdge edge = null;
        try {
            for (ConnectedHyperNode node : nodes) {
                n = node;
                edge = node.getConnectingHyperEdge();
                String uri = getAssociationTypeUri(edge);
                if (!uri.equals(assocTypeUri)) {
                    nodes.remove(node);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Applying association type filter \"" + assocTypeUri +
                "\" failed (" + edge + ",\n" + n.getHyperNode() + ")", e);
        }
    }

    // ---

    private HyperNode lookupTopic(String uri) {
        HyperNode topic = lookupHyperNode(uri);
        if (topic == null) {
            throw new RuntimeException("Topic \"" + uri + "\" not found");
        }
        return topic;
    }

    private HyperNode lookupTopicType(String topicTypeUri) {
        HyperNode topicType = lookupHyperNode(topicTypeUri);
        if (topicType == null) {
            throw new RuntimeException("Topic type \"" + topicTypeUri + "\" not found");
        }
        return topicType;
    }

    private HyperNode lookupAssociationType(String assocTypeUri) {
        HyperNode assocType = lookupHyperNode(assocTypeUri);
        if (assocType == null) {
            throw new RuntimeException("Association type \"" + assocTypeUri + "\" not found");
        }
        return assocType;
    }

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
            throw new RuntimeException("Topic with URI \"" + uri + "\" exists already");
        }
    }

    private HyperNode lookupHyperNode(String uri) {
        return hg.getHyperNode("uri", uri);
    }

    // ---

    /**
     * Determines the topic type of a hyper node.
     *
     * @return  The topic type's URI.
     */
    private String getTopicTypeUri(HyperNode node) {
        return fetchTypeNode(node).getString("uri");
    }

    /**
     * Determines the association type of a hyper edge.
     *
     * @return  The association type's URI.
     */
    private String getAssociationTypeUri(HyperEdge edge) {
        return fetchTypeNode(edge).getString("uri");
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
            throw new RuntimeException("No type node is connected to " + edge);
        }
        return typeNode.getHyperNode();
    }

    // ---

    private void connectNodeToTypeNode(HyperNode node, String typeUri) {
        HyperNode topicType = lookupTopicType(typeUri);
        HyperEdge connectingEdge = hg.createHyperEdge();
        connectingEdge.addHyperNode(topicType, "dm3.core.type");
        connectingEdge.addHyperNode(node, "dm3.core.instance");
    }

    private void connectEdgeToTypeNode(HyperEdge edge, String typeUri) {
        HyperNode topicType = lookupAssociationType(typeUri);
        HyperEdge connectingEdge = hg.createHyperEdge();
        connectingEdge.addHyperNode(topicType, "dm3.core.type");
        connectingEdge.addHyperEdge(edge, "dm3.core.instance");
    }

    // ---

    private void addTopicToEdge(HyperEdge edge, TopicRole topicRole) {
        String roleTypeUri = topicRole.getRoleTypeUri();
        lookupRoleType(roleTypeUri);    // consistency check
        edge.addHyperNode(getRoleNode(topicRole), roleTypeUri);
    }

    private void addAssociationToEdge(HyperEdge edge, AssociationRole assocRole) {
        String roleTypeUri = assocRole.getRoleTypeUri();
        lookupRoleType(roleTypeUri);    // consistency check
        edge.addHyperEdge(getRoleEdge(assocRole), roleTypeUri);
    }

    // ---

    private Set<TopicRole> getTopicRoles(HyperEdge edge) {
        Set<TopicRole> topicRoles = new HashSet();
        for (HyperNodeRole role : edge.getHyperNodes()) {
            topicRoles.add(new TopicRole(role.getHyperNode().getId(), role.getRoleType()));
        }
        return topicRoles;
    }

    private Set<AssociationRole> getAssociationRoles(HyperEdge edge) {
        Set<AssociationRole> assocRoles = new HashSet();
        for (HyperEdgeRole role : edge.getHyperEdges()) {
            assocRoles.add(new AssociationRole(role.getHyperEdge().getId(), role.getRoleType()));
        }
        return assocRoles;
    }

    // ---

    private HyperNode getRoleNode(TopicRole topicRole) {
        if (topicRole.topicIdentifiedByUri()) {
            return lookupTopic(topicRole.getTopicUri());
        } else {
            return hg.getHyperNode(topicRole.getTopicId());
        }
    }

    private HyperEdge getRoleEdge(AssociationRole assocRole) {
        return hg.getHyperEdge(assocRole.getAssociationId());
    }

    // ---

    private void setupMetaTypeNode() {
        HyperNode refNode = hg.getHyperNode(0);
        refNode.setAttribute("uri", "dm3.core.meta_type", IndexMode.KEY);
        refNode.setAttribute("value", "Meta Type");
    }
}
