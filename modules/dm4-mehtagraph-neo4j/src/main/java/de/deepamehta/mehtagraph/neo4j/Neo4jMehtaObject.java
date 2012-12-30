package de.deepamehta.mehtagraph.neo4j;

import de.deepamehta.mehtagraph.ConnectedMehtaEdge;
import de.deepamehta.mehtagraph.ConnectedMehtaNode;
import de.deepamehta.mehtagraph.spi.MehtaEdge;
import de.deepamehta.mehtagraph.spi.MehtaObject;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



abstract class Neo4jMehtaObject extends Neo4jBase implements MehtaObject {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected final Node node;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    protected Neo4jMehtaObject(Node node, Neo4jBase base) {
        super(base);
        this.node = node;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === MehtaObject Implementation ===

    @Override
    public long getId() {
        return node.getId();
    }

    // --- Get Attributes ---

    @Override
    public String getString(String key) {
        return (String) getObject(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return (String) getObject(key, defaultValue);
    }

    @Override
    public int getInteger(String key) {
        return (Integer) getObject(key);
    }

    @Override
    public int getInteger(String key, int defaultValue) {
        return (Integer) getObject(key, defaultValue);
    }

    @Override
    public boolean getBoolean(String key) {
        return (Boolean) getObject(key);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return (Boolean) getObject(key, defaultValue);
    }

    @Override
    public Object getObject(String key) {
        return node.getProperty(key);
    }

    @Override
    public Object getObject(String key, Object defaultValue) {
        return node.getProperty(key, defaultValue);
    }

    // ---

    @Override
    public Iterable<String> getAttributeKeys() {
        return node.getPropertyKeys();
    }

    public boolean hasAttribute(String key) {
        return node.hasProperty(key);
    }

    // --- Set Attributes ---

    @Override
    public String setString(String key, String value) {
        return (String) setObject(key, value);
    }

    @Override
    public Integer setInteger(String key, int value) {
        return (Integer) setObject(key, value);
    }

    @Override
    public Boolean setBoolean(String key, boolean value) {
        return (Boolean) setObject(key, value);
    }

    @Override
    public Object setObject(String key, Object value) {
        Object oldValue = getObject(key, null);
        node.setProperty(key, value);
        return oldValue;
    }

    // --- Indexing ---

    // Note: the indexing methods remain abstract here

    // --- Traversal ---

    @Override
    public Set<MehtaEdge> getMehtaEdges(String myRoleType) {
        Iterable<Relationship> rels;
        if (myRoleType == null) {
            rels = node.getRelationships(Direction.INCOMING);
        } else {
            rels = node.getRelationships(getRelationshipType(myRoleType), Direction.INCOMING);
        }
        //
        return buildMehtaEdges(rels);
    }

    // ---

    @Override
    public ConnectedMehtaNode getConnectedMehtaNode(String myRoleType, String othersRoleType) {
        Set<ConnectedMehtaNode> nodes = getConnectedMehtaNodes(myRoleType, othersRoleType);
        switch (nodes.size()) {
        case 0:
            return null;
        case 1:
            return nodes.iterator().next();
        default:
            throw new RuntimeException("Ambiguity: there are " + nodes.size() + " connected nodes (" + this +
                ", myRoleType=\"" + myRoleType + "\", othersRoleType=\"" + othersRoleType + "\")");
        }
    }

    @Override
    public Set<ConnectedMehtaNode> getConnectedMehtaNodes(String myRoleType, String othersRoleType) {
        return new TraveralResultBuilder(this, traverseToMehtaNodes(myRoleType, othersRoleType)) {
            @Override
            Object buildResult(Node connectedNode, Node auxiliaryNode) {
                return new ConnectedMehtaNode(buildMehtaNode(connectedNode), buildMehtaEdge(auxiliaryNode));
            }
        }.getResult();
    }

    // ---

    @Override
    public ConnectedMehtaEdge getConnectedMehtaEdge(String myRoleType, String othersRoleType) {
        Set<ConnectedMehtaEdge> edges = getConnectedMehtaEdges(myRoleType, othersRoleType);
        switch (edges.size()) {
        case 0:
            return null;
        case 1:
            return edges.iterator().next();
        default:
            throw new RuntimeException("Ambiguity: there are " + edges.size() + " connected edges (" + this +
                ", myRoleType=\"" + myRoleType + "\", othersRoleType=\"" + othersRoleType + "\")");
        }
    }

    @Override
    public Set<ConnectedMehtaEdge> getConnectedMehtaEdges(String myRoleType, String othersRoleType) {
        return new TraveralResultBuilder(this, traverseToMehtaEdges(myRoleType, othersRoleType)) {
            @Override
            Object buildResult(Node connectedNode, Node auxiliaryNode) {
                return new ConnectedMehtaEdge(buildMehtaEdge(connectedNode), buildMehtaEdge(auxiliaryNode));
            }
        }.getResult();
    }

    // --- Deletion ---

    @Override
    public void delete() {
        node.delete();
    }



    // ----------------------------------------------------------------------------------------------- Protected Methods

    protected final String getAttributesString(PropertyContainer container) {
        Map<String, Object> properties = getProperties(container);
        //
        StringBuilder builder = new StringBuilder("{");
        for (String key : properties.keySet()) {
            if (builder.length() > 1) {
                builder.append(", ");
            }
            builder.append(key + "=" + properties.get(key));
        }
        builder.append("}");
        return builder.toString();
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    Node getNode() {
        return node;
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    // === Helper ===

    private Set<MehtaEdge> buildMehtaEdges(Iterable<Relationship> relationships) {
        Set edges = new HashSet();
        for (Relationship rel : relationships) {
            Node auxiliaryNode = rel.getStartNode();
            edges.add(buildMehtaEdge(auxiliaryNode));
        }
        return edges;
    }

    private Map<String, Object> getProperties(PropertyContainer container) {
        Map properties = new HashMap();
        for (String key : container.getPropertyKeys()) {
            properties.put(key, container.getProperty(key));
        }
        return properties;
    }
}
