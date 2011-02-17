package de.deepamehta.core.storage.neo4j;

import de.deepamehta.core.model.DataField;
import de.deepamehta.core.model.Properties;
import de.deepamehta.core.model.PropValue;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.model.RelatedTopic;
import de.deepamehta.core.model.Relation;
import de.deepamehta.core.storage.Storage;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.TraversalPosition;
import org.neo4j.graphdb.Traverser.Order;
import org.neo4j.graphdb.traversal.PruneEvaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.helpers.Predicate;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.Traversal;
import org.neo4j.index.IndexHits;
import org.neo4j.index.IndexService;
import org.neo4j.index.lucene.LuceneIndexService;
import org.neo4j.index.lucene.LuceneFulltextQueryIndexService;
import org.neo4j.meta.model.MetaModel;
import org.neo4j.meta.model.MetaModelClass;
import org.neo4j.meta.model.MetaModelImpl;
import org.neo4j.meta.model.MetaModelNamespace;
import org.neo4j.meta.model.MetaModelProperty;
import org.neo4j.meta.model.MetaModelRelTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



public class Neo4jStorage implements Storage {

    private final Logger logger = Logger.getLogger(getClass().getName());

    private GraphDatabaseService graphDb;
    private IndexService index;
    private LuceneFulltextQueryIndexService fulltextIndex;

    // Note: the meta-model namespace is package private in order to let a Neo4jTopicType rename itself when its
    // URI changes. See Neo4jTopicType.setTypeUri().
    // We do it this way because we don't want extend the core service resp. the storage interfaces.
    MetaModelNamespace namespace;

    // Note: the type cache is package private in order to let a Neo4jTopicType re-hash itself when its URI changes.
    // See Neo4jTopicType.setTypeUri().
    // We do it this way because we don't want extend the core service resp. the storage interfaces.
    final TypeCache typeCache;

    // SEARCH_RESULT relations are not part of the knowledge base but help to visualize / navigate result sets.
    static enum RelType implements RelationshipType {
        RELATION, SEARCH_RESULT,
        SEQUENCE_START, SEQUENCE
    }

    // ---------------------------------------------------------------------------------------------------- Constructors

    public Neo4jStorage(String dbPath) {
        logger.info("Creating DB and indexing services");
        this.typeCache = new TypeCache(this);
        //
        graphDb = new EmbeddedGraphDatabase(dbPath);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ******************************
    // *** Storage Implementation ***
    // ******************************



    // --- Topics ---

    @Override
    public Topic getTopic(long id) {
        logger.info("Getting node " + id);
        return buildTopic(graphDb.getNodeById(id), true);
    }

    @Override
    public Topic getTopic(String key, PropValue value) {
        logger.info("Getting node by property (" + key + "=" + value + ")");
        Node node = index.getSingleNode(key, value.value());
        return node != null ? buildTopic(node, true) : null;
    }

    @Override
    public Topic getTopic(String typeUri, String key, PropValue value) {
        logger.info("Getting node (typeUri=" + typeUri + ", " + key + "=" + value + ")");
        IndexHits<Node> nodes = fulltextIndex.getNodesExactMatch(key, value.value());
        Node resultNode = null;
        // apply type filter
        for (Node node : nodes) {
            if (getTypeUri(node).equals(typeUri)) {
                if (resultNode != null) {
                    throw new RuntimeException("Ambiguity: more than one topic matches " +
                        "(typeUri=" + typeUri + ", " + key + "=" + value + ")");
                }
                resultNode = node;
            }
        }
        //
        return resultNode != null ? buildTopic(resultNode, true) : null;
    }

    @Override
    public PropValue getTopicProperty(long topicId, String key) {
        return new PropValue(graphDb.getNodeById(topicId).getProperty(key, null));
    }

    @Override
    public List<Topic> getTopics(String typeUri) {
        List topics = new ArrayList();
        for (Node node : getMetaClass(typeUri).getDirectInstances()) {
            // Note: the topic properties remain uninitialzed here.
            // It is up to the plugins to provide selected properties (see providePropertiesHook()).
            topics.add(buildTopic(node, false));
        }
        return topics;
    }

    @Override
    public List<Topic> getTopics(String key, Object value) {
        IndexHits<Node> nodes = index.getNodes(key, value);
        logger.info("Getting nodes by property (" + key + "=" + value + ") => " + nodes.size() + " nodes");
        List topics = new ArrayList();
        for (Node node : nodes) {
            topics.add(buildTopic(node, false));    // properties remain uninitialized
        }
        return topics;
    }

    @Override
    public List<RelatedTopic> getRelatedTopics(long topicId, List<String> includeTopicTypes,
                                                             List<String> includeRelTypes,
                                                             List<String> excludeRelTypes) {
        // Note: we must exclude the meta-model's namespace and property nodes. They are not intended for
        // being exposed to the user (additionally, getTypeNode() would fail on these nodes).
        excludeRelTypes.add("META_CLASS;OUTGOING");
        excludeRelTypes.add("META_HAS_PROPERTY;INCOMING");
        excludeRelTypes.add("SEQUENCE_START;INCOMING");
        Node startNode = graphDb.getNodeById(topicId);
        Traverser traverser = createRelatedTopicsTraverser(startNode, includeTopicTypes,
                                                                      includeRelTypes, excludeRelTypes);
        List relTopics = new ArrayList();
        for (Path path : traverser) {
            Node node = path.endNode();
            Relationship relation = path.lastRelationship();
            //
            RelatedTopic relTopic = new RelatedTopic();
            // Note: the topic properties remain uninitialzed here.
            // It is up to the plugins to provide selected properties (see providePropertiesHook()).
            relTopic.setTopic(buildTopic(node, false));
            // Note: the relation properties remain uninitialzed here.
            // It is up to the plugins to provide selected properties (see providePropertiesHook()).
            relTopic.setRelation(buildRelation(relation, false));
            //
            relTopics.add(relTopic);
        }
        logger.info("=> " + relTopics.size() + " related nodes");
        return relTopics;
    }

    @Override
    public List<Topic> searchTopics(String searchTerm, String fieldUri, boolean wholeWord) {
        if (fieldUri == null) fieldUri = "default";
        if (!wholeWord) searchTerm += "*";
        IndexHits<Node> nodes = fulltextIndex.getNodes(fieldUri, searchTerm);
        logger.info("Searching \"" + searchTerm + "\" in field \"" + fieldUri + "\" => " + nodes.size() + " nodes");
        List topics = new ArrayList();
        for (Node node : nodes) {
            logger.fine("Adding node " + node.getId());
            // Filter result set. Note: a search should not find other searches.
            //
            // TODO: drop this filter. Items not intended for being find should not be indexed at all. Model change
            // required: the indexing mode must be specified per topic type/data field pair instead per data field.
            if (!getTypeUri(node).equals("de/deepamehta/core/topictype/SearchResult")) {
                topics.add(buildTopic(node, false));    // properties remain uninitialized
            }
        }
        logger.info("After filtering => " + topics.size() + " nodes");
        return topics;
    }

    @Override
    public Topic createTopic(String typeUri, Properties properties) {
        Node node = graphDb.createNode();
        logger.info("Creating node => ID=" + node.getId());
        getMetaClass(typeUri).getDirectInstances().add(node);       // set topic type
        setProperties(node, properties, typeUri);
        return new Topic(node.getId(), typeUri, null, properties);  // FIXME: label remains uninitialized
    }

    @Override
    public void setTopicProperties(long id, Properties properties) {
        logger.info("Setting properties of node " + id + ": " + properties);
        Node node = graphDb.getNodeById(id);
        setProperties(node, properties);
    }

    @Override
    public void deleteTopic(long id) {
        // Note: when this is called all the topic's relations are already deleted.
        // So we can't determine the topic's type anymore (and thus can't determine the index modes).
        logger.info("Deleting node " + id);
        Node node = graphDb.getNodeById(id);
        // update index
        removeFromIndex(node);
        //
        node.delete();
    }

    // --- Relations ---

    @Override
    public Relation getRelation(long id) {
        logger.info("Getting relationship " + id);
        Relationship relationship = graphDb.getRelationshipById(id);
        return buildRelation(relationship, true);
    }

    @Override
    public Set<Relation> getRelations(long topicId) {
        Set relations = new HashSet();
        Node node = graphDb.getNodeById(topicId);
        for (Relationship rel : node.getRelationships()) {
            relations.add(buildRelation(rel, false));
        }
        return relations;
    }

    @Override
    public Relation getRelation(long srcTopicId, long dstTopicId, String typeId, boolean isDirected) {
        logger.info("Getting relationship between nodes " + srcTopicId + " and " + dstTopicId);
        Relationship relationship = null;
        Node node = graphDb.getNodeById(srcTopicId);
        for (Relationship rel : node.getRelationships()) {
            if (!doRelationshipMatches(rel, node, dstTopicId, typeId, isDirected)) {
                continue;
            }
            // ambiguity?
            if (relationship != null) {
                throw new RuntimeException("Ambiguity: more than one relation matches (srcTopicId=" + srcTopicId +
                    ", dstTopicId=" + dstTopicId + ", typeId=" + typeId + ", isDirected=" + isDirected + ")");
            }
            //
            relationship = rel;
        }
        if (relationship != null) {
            logger.info("=> relationship found (ID=" + relationship.getId() + ")");
            return buildRelation(relationship, true);
        }
        logger.info("=> no such relationship");
        return null;
    }

    @Override
    public List<Relation> getRelations(long srcTopicId, long dstTopicId, String typeId, boolean isDirected) {
        logger.info("Getting relationships between nodes " + srcTopicId + " and " + dstTopicId);
        List<Relation> relations = new ArrayList();
        Node node = graphDb.getNodeById(srcTopicId);
        for (Relationship rel : node.getRelationships()) {
            if (!doRelationshipMatches(rel, node, dstTopicId, typeId, isDirected)) {
                continue;
            }
            relations.add(buildRelation(rel, false));
        }
        //
        if (!relations.isEmpty()) {
            logger.info("=> " + relations.size() + " relationships found");
        } else {
            logger.info("=> no such relationship");
        }
        //
        return relations;
    }

    @Override
    public Relation createRelation(String typeId, long srcTopicId, long dstTopicId, Properties properties) {
        logger.info("Creating \"" + typeId + "\" relationship from node " + srcTopicId + " to " + dstTopicId);
        Node srcNode = graphDb.getNodeById(srcTopicId);
        Node dstNode = graphDb.getNodeById(dstTopicId);
        Relationship relationship = srcNode.createRelationshipTo(dstNode, getRelationshipType(typeId));
        setProperties(relationship, properties);
        return new Relation(relationship.getId(), typeId, srcTopicId, dstTopicId, properties);
    }

    @Override
    public void setRelationProperties(long id, Properties properties) {
        logger.info("Setting properties of relationship " + id + ": " + properties);
        Relationship relationship = graphDb.getRelationshipById(id);
        setProperties(relationship, properties);
    }

    @Override
    public void deleteRelation(long id) {
        logger.info("Deleting relationship " + id);
        graphDb.getRelationshipById(id).delete();
    }

    // --- Types ---

    @Override
    public Set<String> getTopicTypeUris() {
        Set typeUris = new HashSet();
        for (MetaModelClass metaClass : getAllMetaClasses()) {
            typeUris.add(metaClass.getName());
        }
        return typeUris;
    }

    @Override
    public TopicType getTopicType(String typeUri) {
        return typeCache.get(typeUri);
    }

    @Override
    public TopicType createTopicType(Properties properties, List<DataField> dataFields) {
        TopicType topicType = new Neo4jTopicType(properties, dataFields, this);
        typeCache.put(topicType);
        return topicType;
    }

    @Override
    public void addDataField(String typeUri, DataField dataField) {
        getTopicType(typeUri).addDataField(dataField);
    }

    @Override
    public void updateDataField(String typeUri, DataField dataField) {
        getTopicType(typeUri).getDataField(dataField.getUri()).setProperties(dataField.getProperties());
    }

    @Override
    public void removeDataField(String typeUri, String fieldUri) {
        getTopicType(typeUri).removeDataField(fieldUri);
    }

    @Override
    public void setDataFieldOrder(String typeUri, List fieldUris) {
        getTopicType(typeUri).setDataFieldOrder(fieldUris);
    }

    // --- DB ---

    @Override
    public de.deepamehta.core.storage.Transaction beginTx() {
        return new Neo4jTransaction(graphDb);
    }

    /**
     * Performs storage layer initialization which is required to run in a transaction.
     */
    @Override
    public boolean init() {
        // 1) init indexing services
        index = new LuceneIndexService(graphDb);
        fulltextIndex = new LuceneFulltextQueryIndexService(graphDb);
        // 2) init meta model
        MetaModel model = new MetaModelImpl(graphDb, index);
        namespace = model.getGlobalNamespace();
        // 3) init migration number
        if (!graphDb.getReferenceNode().hasProperty("core_migration_nr")) {
            logger.info("Starting with a fresh DB -- Setting migration number to 0");
            setMigrationNr(0);
            return true;
        }
        return false;
    }

    @Override
    public void shutdown() {
        logger.info("Shutdown DB and indexing services");
        if (index != null) {
            index.shutdown();
            fulltextIndex.shutdown();
        } else {
            logger.warning("Indexing services not shutdown properly");
        }
        //
        graphDb.shutdown();
        graphDb = null;
    }

    @Override
    public int getMigrationNr() {
        return (Integer) graphDb.getReferenceNode().getProperty("core_migration_nr");
    }

    @Override
    public void setMigrationNr(int migrationNr) {
        graphDb.getReferenceNode().setProperty("core_migration_nr", migrationNr);
    }



    // ***********************
    // *** Package Helpers ***
    // ***********************



    // --- Topics ---

    /**
     * Builds a DeepaMehta {@link Topic} from a Neo4j node.
     *
     * @param   includeProperties   if true, the topic properties are fetched.
     */
    private Topic buildTopic(Node node, boolean includeProperties) {
        // 1) calculate type
        String typeUri = getTypeUri(node);
        // 2) calculate label
        String label;
        TopicType topicType = getTopicType(typeUri);
        String topicLabelFieldUri = topicType.getProperty("topic_label_field_uri", null).toString();
        if (topicLabelFieldUri != null) {
            label = node.getProperty(topicLabelFieldUri).toString();    // Note: property value can be a number as well
        } else {
            if (topicType.getDataFields().size() > 0) {
                // use value of first data field
                String fieldUri = topicType.getDataField(0).getUri();
                label = node.getProperty(fieldUri).toString();          // Note: property value can be a number as well
            } else {
                // there are no data fields -> the label can't be set
                label = "?";
            }
        }
        //
        Properties properties = includeProperties ? getProperties(node) : null;
        return new Topic(node.getId(), typeUri, label, properties);
    }

    // --- Relations ---

    /**
     * Builds a DeepaMehta relation from a Neo4j relationship.
     *
     * @param   includeProperties   if true, the relation properties are fetched.
     */
    private Relation buildRelation(Relationship rel, boolean includeProperties) {
        Properties properties = includeProperties ? getProperties(rel) : null;
        return new Relation(rel.getId(), rel.getType().name(),
            rel.getStartNode().getId(), rel.getEndNode().getId(), properties);
    }

    private boolean doRelationshipMatches(Relationship rel, Node node, long dstTopicId, String typeId,
                                                                                        boolean isDirected) {
        // do nodes match?
        Node relNode = rel.getOtherNode(node);
        if (relNode.getId() != dstTopicId) {
            return false;
        }
        // apply type filter
        if (typeId != null && !rel.getType().name().equals(typeId)) {
            return false;
        }
        // apply direction filter
        if (isDirected && rel.getStartNode().getId() != node.getId()) {
            return false;
        }
        //
        return true;
    }

    // --- Properties ---

    Properties getProperties(PropertyContainer container) {
        Properties properties = new Properties();
        for (String key : container.getPropertyKeys()) {
            properties.put(key, new PropValue(container.getProperty(key)));
        }
        return properties;
    }

    private void setProperties(PropertyContainer container, Properties properties) {
        String typeUri = null;
        if (container instanceof Node) {
            typeUri = getTypeUri((Node) container);
        }
        setProperties(container, properties, typeUri);
    }

    private void setProperties(PropertyContainer container, Properties properties, String typeUri) {
        if (properties == null) {
            throw new NullPointerException("setProperties() called with properties=null");
        }
        for (String key : properties.keySet()) {
            Object value = properties.get(key).value();
            Object oldValue = container.getProperty(key, null);     // null for newly created topics
            // 1) update DB
            container.setProperty(key, value);
            // 2) update index
            if (container instanceof Node) {
                // Note: we only index node properties.
                // Neo4j can't index relationship properties.
                indexProperty((Node) container, key, value, oldValue, typeUri);
            }
        }
    }

    private void indexProperty(Node node, String key, Object value, Object oldValue, String typeUri) {
        // Note: we only index instance nodes. Meta nodes (types) are responsible for indexing themself.
        if (typeUri.equals("de/deepamehta/core/topictype/TopicType")) {
            return;
        }
        // remove old value and index new value
        DataField dataField = getTopicType(typeUri).getDataField(key);
        String indexingMode = dataField.getIndexingMode();
        if (indexingMode.equals("OFF")) {
            return;
        } else if (indexingMode.equals("KEY")) {
            index.removeIndex(node, key);                               // remove old
            index.index(node, key, value);                              // index new
        } else if (indexingMode.equals("FULLTEXT")) {
            // Note: all the topic's FULLTEXT properties are indexed under the same key ("default").
            // So, when removing from index we must explicitley give the old value.
            if (oldValue != null) {
                fulltextIndex.removeIndex(node, "default", oldValue);   // remove old
            }
            fulltextIndex.index(node, "default", value);                // index new
        } else if (indexingMode.equals("FULLTEXT_KEY")) {
            fulltextIndex.removeIndex(node, key);                       // remove old
            fulltextIndex.index(node, key, value);                      // index new
        } else {
            throw new RuntimeException("Data field \"" + key + "\" of type definition \"" +
                typeUri + "\" has unexpectd indexing mode: \"" + indexingMode + "\"");
        }
    }

    /**
     * Completely removes a topic from the index. Called when a topic is deleted.
     */
    private void removeFromIndex(Node node) {
        // Note: we don't know the index mode so we just remove for every mode.
        // (In conjunction with node deletion it would not be easy to tell the index mode.)
        for (String key : node.getPropertyKeys()) {
            index.removeIndex(node, key);
            fulltextIndex.removeIndex(node, key);
        }
        fulltextIndex.removeIndex(node, "default");
    }

    // --- Types ---

    private String getTypeUri(Node node) {
        // FIXME: meta-types must be detected manually
        if (node.getProperty("de/deepamehta/core/property/TypeURI", null) != null) {
            // FIXME: a more elaborated criteria is required, e.g. an incoming TOPIC_TYPE relation
            return "de/deepamehta/core/topictype/TopicType";
        }
        return (String) getTypeNode(node).getProperty("de/deepamehta/core/property/TypeURI");
    }

    private Node getTypeNode(Node node) {
        // Old Neo4j API:
        // TraversalDescription desc = TraversalFactory.createTraversalDescription();
        // desc = desc.relationships(MetaModelRelTypes.META_HAS_INSTANCE, Direction.INCOMING);
        // desc = desc.filter(ReturnFilter.ALL_BUT_START_NODE);
        // Iterator<Node> i = desc.traverse(node).nodes().iterator();
        //
        // Old Neo4j API:
        // Iterator<Node> i = node.expand(MetaModelRelTypes.META_HAS_INSTANCE, Direction.INCOMING).nodes().iterator();
        //
        Relationship relation = node.getSingleRelationship(MetaModelRelTypes.META_HAS_INSTANCE, Direction.INCOMING);
        // error check
        if (relation == null) {
            throw new RuntimeException("Type of " + node + " is unknown " +
                "(there is no incoming META_HAS_INSTANCE relationship)");
        }
        //
        return relation.getOtherNode(node);
    }

    /* FIXME: not in use
    private boolean isMetaNode(Node node) {
        return node.hasRelationship(MetaModelRelTypes.META_CLASS,    Direction.INCOMING) ||
               node.hasRelationship(MetaModelRelTypes.META_PROPERTY, Direction.INCOMING);
    } */

    // ---

    private RelationshipType getRelationshipType(String typeId) {
        try {
            // 1st try: static types are returned directly
            return RelType.valueOf(typeId);
        } catch (IllegalArgumentException e) {
            // 2nd try: search through dynamic types
            for (RelationshipType relType : graphDb.getRelationshipTypes()) {
                if (relType.name().equals(typeId)) {
                    return relType;
                }
            }
            // Last resort: create new type
            logger.info("### Relation type \"" + typeId + "\" does not exist -- Creating it dynamically");
            return DynamicRelationshipType.withName(typeId);
        }
    }

    // --- Meta Model ---

    MetaModelClass getMetaClass(String typeUri) {
        MetaModelClass metaClass = namespace.getMetaClass(typeUri, false);
        if (metaClass == null) {
            throw new RuntimeException("Topic type \"" + typeUri + "\" is unknown");
        }
        return metaClass;
    }

    // FIXME: to be dropped
    MetaModelClass getMetaModelClass(String typeUri) {
        return namespace.getMetaClass(typeUri, false);
    }

    Collection<MetaModelClass> getAllMetaClasses() {
        return namespace.getMetaClasses();
    }

    MetaModelClass createMetaClass(String typeUri) {
        MetaModelClass metaClass = namespace.getMetaClass(typeUri, false);
        if (metaClass != null) {
            throw new RuntimeException("Topic type with URI \"" + typeUri + "\" already exists");
        }
        return namespace.getMetaClass(typeUri, true);
    }

    MetaModelProperty createMetaProperty(String fieldUri) {
        return namespace.getMetaProperty(fieldUri, true);
    }

    // --- Traversal ---

    private Traverser createRelatedTopicsTraverser(Node node, List<String> includeTopicTypes,
                                                              List<String> includeRelTypes,
                                                              List<String> excludeRelTypes) {
        // Doesn't work. Throws IllegalArgumentException if no relationship types are passed.
        //
        // return node.traverse(Order.BREADTH_FIRST, StopEvaluator.DEPTH_ONE,
        //     new RelatedTopicsFilter(includeTopicTypes, includeRelTypes, excludeRelTypes));
        //
        TraversalDescription desc = Traversal.description();
        desc = desc.filter(new RelatedTopicsFilter(includeTopicTypes, includeRelTypes, excludeRelTypes));
        desc = desc.prune(new DepthOnePruneEvaluator());
        return desc.traverse(node);
    }

    // private class RelatedTopicsFilter implements ReturnableEvaluator {
    private class RelatedTopicsFilter implements Predicate {

        private List<String> includeTopicTypes;
        private Map<String, Direction> includeRelTypes;
        private Map<String, Direction> excludeRelTypes;

        private RelatedTopicsFilter(List<String> includeTopicTypes,
                                    List<String> includeRelTypes, List<String> excludeRelTypes) {
            //
            this.includeTopicTypes = includeTopicTypes;
            this.includeRelTypes = parseRelTypeFilter(includeRelTypes);
            this.excludeRelTypes = parseRelTypeFilter(excludeRelTypes);
            //
        }

        @Override
        public boolean accept(Object item) {                // boolean isReturnableNode(TraversalPosition position) {
            Path path = (Path) item;
            if (path.length() == 0) {                       // if (position.isStartNode()) {
                return false;
            }
            // Note: we must apply the relation type filter first in order to sort out the meta-model's namespace and
            // property nodes before the topic type filter would see them (getTypeUri() would fail on these nodes).
            Node node = path.endNode();                     // Node node = position.currentNode();
            // 1) apply relation type filter
            Relationship rel = path.lastRelationship();     // Relationship rel = position.lastRelationshipTraversed();
            String relTypeName = rel.getType().name();
            // apply include filter
            if (!includeRelTypes.isEmpty()) {
                Direction dir = includeRelTypes.get(relTypeName);
                if (dir == null || !directionMatches(node, rel, dir)) {
                    return false;
                }
            } else {
                // apply exclude filter
                Direction dir = excludeRelTypes.get(relTypeName);
                if (dir != null && directionMatches(node, rel, dir)) {
                    return false;
                }
            }
            // 2) apply topic type filter
            if (!includeTopicTypes.isEmpty() && !includeTopicTypes.contains(getTypeUri(node))) {
                return false;
            }
            //
            return true;
        }

        // ---

        private Map parseRelTypeFilter(List<String> relTypes) {
            Map relTypeFilter = new HashMap();
            for (String relFilter : relTypes) {
                String[] relFilterTokens = relFilter.split(";");
                String relTypeName = relFilterTokens[0];
                Direction dir;
                if (relFilterTokens.length == 1) {
                    dir = Direction.BOTH;
                } else {
                    dir = Direction.valueOf(relFilterTokens[1]);
                }
                relTypeFilter.put(relTypeName, dir);
            }
            return relTypeFilter;
        }

        /**
         * Returns true if the relationship has the given direction from the perspective of the node.
         * Prerequisite: the given node is involved in the given relationship.
         */
        private boolean directionMatches(Node node, Relationship rel, Direction dir) {
            if (dir == Direction.BOTH) {
                return true;
            } if (dir == Direction.OUTGOING && node.equals(rel.getStartNode())) {
                return true;
            } else if (dir == Direction.INCOMING && node.equals(rel.getEndNode())) {
                return true;
            } else {
                return false;
            }
        }
    }

    private class DepthOnePruneEvaluator implements PruneEvaluator {

        @Override
        public boolean pruneAfter(Path path) {      // boolean pruneAfter(Position position) {
            return path.length() == 1;              // return position.depth() == 1;
        }
    }
}
