package de.deepamehta.core.storage.impl;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.AssociationDefinition;
import de.deepamehta.core.model.AssociationType;
import de.deepamehta.core.model.MetaType;
import de.deepamehta.core.model.Role;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicData;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.model.TopicTypeDefinition;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.storage.DeepaMehtaStorage;
import de.deepamehta.core.storage.DeepaMehtaTransaction;

import de.deepamehta.hypergraph.ConnectedHyperNode;
import de.deepamehta.hypergraph.HyperEdge;
import de.deepamehta.hypergraph.HyperGraph;
import de.deepamehta.hypergraph.HyperNode;
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
    private HGTypeCache typeCache;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public HGStorageBridge(HyperGraph hg) {
        this.hg = hg;
        this.typeCache = new HGTypeCache(this);
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
        // create node
        HyperNode node = hg.createHyperNode();
        if (topicData.getUri() != null) {
            node.setAttribute("uri", topicData.getUri(), IndexMode.KEY);
        }
        node.setAttribute("value", topicData.getValue().value());
        // associate with type
        HyperNode topicType = lookupTopicType(topicData.getTypeUri());
        HyperEdge edge = hg.createHyperEdge();  // FIXME: use association type ("dm3.core.instantiation")
        edge.addHyperNode(topicType, "dm3.core.type");
        edge.addHyperNode(node, "dm3.core.instance");
        //
        return buildTopic(node);
    }

    // === Associations ===

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

    // === Types ===

    @Override
    public TopicType getTopicType(String typeUri) {
        return buildTopicType(lookupTopicType(typeUri));
    }

    @Override
    public TopicTypeDefinition getTopicTypeDefinition(String typeUri) {
        return typeCache.get(typeUri);
    }

    @Override
    public MetaType createMetaType(MetaType metaType) {
        Topic topic = createTopic(metaType);
        return new MetaType(topic);
    }

    @Override
    public TopicType createTopicType(TopicType topicType) {
        Topic topic = createTopic(topicType);
        return new TopicType(topic, topicType.getDataTypeUri());
    }

    @Override
    public AssociationType createAssociationType(AssociationType assocType) {
        Topic topic = createTopic(assocType);
        return new AssociationType(topic);
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
            throw new NullPointerException("Tried to build a Topic from a null HyperNode");
        }
        return new HGTopic(node.getId(), node.getString("uri", null), new TopicValue(node.get("value")),
            getTopicTypeUri(node), null);     // composite=null
    }

    TopicType buildTopicType(HyperNode node) {
        return new TopicType(buildTopic(node), null);       // FIXME: dataTypeUri=null
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
