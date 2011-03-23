package de.deepamehta.core.storage.impl;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.AssociationDefinition;
import de.deepamehta.core.model.Role;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicData;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.storage.DeepaMehtaStorage;
import de.deepamehta.core.storage.DeepaMehtaTransaction;

import de.deepamehta.hypergraph.ConnectedHyperNode;
import de.deepamehta.hypergraph.HyperEdge;
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

    @Override
    public boolean topicExists(String key, TopicValue value) {
        return hg.getHyperNode(key, value.value()) != null;
    }

    @Override
    public Topic getRelatedTopic(long topicId, String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri) {
        // FIXME: assocTypeUri not respected
        ConnectedHyperNode node = hg.getHyperNode(topicId).getConnectedHyperNode(myRoleTypeUri, othersRoleTypeUri);
        return node != null ? buildTopic(node.getHyperNode()) : null;
    }

    @Override
    public Set<Topic> getRelatedTopics(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                                          String othersRoleTypeUri) {
        // FIXME: assocTypeUri not respected
        Set<ConnectedHyperNode> nodes = hg.getHyperNode(topicId).getConnectedHyperNodes(myRoleTypeUri,
                                                                                        othersRoleTypeUri);
        Set<Topic> topics = new HashSet();
        for (ConnectedHyperNode node : nodes) {
            topics.add(buildTopic(node.getHyperNode()));
        }
        return topics;
    }

    @Override
    public void setTopicValue(long topicId, TopicValue value) {
        hg.getHyperNode(topicId).setAttribute("value", value.value());
    }

    @Override
    public Topic createTopic(TopicData topicData) {
        // 1) create node
        HyperNode node = hg.createHyperNode();
        // Note: "uri" and "value" are optional
        if (topicData.getUri() != null) {
            node.setAttribute("uri", topicData.getUri(), IndexMode.KEY);
        }
        if (topicData.getValue() != null) {
            node.setAttribute("value", topicData.getValue().value());
        }
        // 2) associate with type
        HyperNode topicType = lookupTopicType(topicData.getTypeUri());
        HyperEdge edge = hg.createHyperEdge();  // FIXME: use association type ("dm3.core.instantiation")
        edge.addHyperNode(topicType, "dm3.core.type");
        edge.addHyperNode(node, "dm3.core.instance");
        //
        return buildTopic(node);
    }

    // === Associations ===

    @Override
    public Set<Association> getAssociations(long topicId, String myRoleType) {
        Set<Association> assocs = new HashSet();
        HyperNode node = hg.getHyperNode(topicId);
        for (HyperEdge edge : node.getHyperEdges(myRoleType)) {
            Set<Role> roles = new HashSet();
            for (HyperNodeRole role : edge.getHyperNodes()) {
                roles.add(new Role(role.getHyperNode().getId(), role.getRoleType()));
            }
            assocs.add(new Association(edge.getId(), null, roles));   // FIXME: assocTypeUri=null
        }
        return assocs;
    }

    @Override
    public Association createAssociation(Association assoc) {
        HyperEdge edge = hg.createHyperEdge();  // FIXME: use association type
        for (Role role : assoc.getRoles()) {
            HyperNode node;
            if (role.topicIdentifiedById()) {
                node = hg.getHyperNode(role.getTopicId());
            } else {
                node = lookupTopic(role.getTopicUri());
            }
            edge.addHyperNode(node, role.getRoleTypeUri());
        }
        return new Association(edge.getId(), assoc.getTypeUri(), assoc.getRoles());
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
            setupMetaNode();
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

    // ----------------------------------------------------------------------------------------- Package Private Methods

    HyperNode lookupTopic(String uri) {
        HyperNode topic = hg.getHyperNode("uri", uri);
        if (topic == null) {
            throw new RuntimeException("Topic \"" + uri + "\" not found");
        }
        return topic;
    }

    HyperNode lookupTopicType(String typeUri) {
        HyperNode topicType = hg.getHyperNode("uri", typeUri);
        if (topicType == null) {
            throw new RuntimeException("Topic type \"" + typeUri + "\" not found");
        }
        return topicType;
    }

    // ---

    Topic buildTopic(HyperNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Tried to build a Topic from a null HyperNode");
        }
        // build value
        TopicValue value = null;
        Object v = node.get("value", null);
        if (v != null) {
            value = new TopicValue(v);
        }
        // Note: "uri" and "value" are optional
        return new HGTopic(node.getId(), node.getString("uri", null), value, getTopicTypeUri(node), null);
                                                                                                    // composite=null
    }

    // ---

    String getTopicTypeUri(HyperNode node) {
        return getTopicType(node).getString("uri");
    }

    // ---

    HyperNode getTopicType(HyperNode node) {
        // FIXME: should we additionally check weather the edge type is "dm3.core.instantiation"?
        return node.getConnectedHyperNode("dm3.core.instance", "dm3.core.type").getHyperNode();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void setupMetaNode() {
        HyperNode refNode = hg.getHyperNode(0);
        refNode.setAttribute("uri", "dm3.core.meta_type", IndexMode.KEY);
        refNode.setAttribute("value", "Meta Type");
    }
}
