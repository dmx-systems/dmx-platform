package de.deepamehta.mehtagraph.neo4j;

import de.deepamehta.mehtagraph.MehtaGraphIndexMode;
import de.deepamehta.mehtagraph.MehtaObjectRole;
import de.deepamehta.mehtagraph.spi.MehtaEdge;
import de.deepamehta.mehtagraph.spi.MehtaObject;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;



class Neo4jMehtaEdge extends Neo4jMehtaObject implements MehtaEdge {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    Neo4jMehtaEdge(Node node, Neo4jBase base) {
        super(node, base);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === MehtaEdge Implementation ===

    @Override
    public List<MehtaObjectRole> getMehtaObjects() {
        List<MehtaObjectRole> mehtaObjects = new ArrayList();
        for (Relationship rel : getRelationships()) {
            Node node = rel.getEndNode();
            String roleType = rel.getType().name();
            MehtaObject mehtaObject = buildMehtaObject(node);
            mehtaObjects.add(new Neo4jMehtaObjectRole(mehtaObject, roleType, rel));
        }
        // sanity check
        if (mehtaObjects.size() != 2) {
            // Note: custom toString() stringifier called here to avoid endless recursion.
            // The default stringifier would call getMehtaObjects() again and fail endlessly.
            throw new RuntimeException("Data inconsistency: mehta edge " + getId() + " connects " +
                mehtaObjects.size() + " mehta objects instead of 2 (" + toString(mehtaObjects) + ")");
        }
        //
        return mehtaObjects;
    }

    @Override
    public MehtaObjectRole getMehtaObject(long objectId) {
        List<MehtaObjectRole> roles = getMehtaObjects();
        long id1 = roles.get(0).getMehtaObject().getId();
        long id2 = roles.get(1).getMehtaObject().getId();
        //
        if (id1 == objectId && id2 == objectId) {
            throw new RuntimeException("Self-connected mehta objects are not supported (" + this + ")");
        }
        //
        if (id1 == objectId) {
            return roles.get(0);
        } else if (id2 == objectId) {
            return roles.get(1);
        } else {
            throw new RuntimeException("Mehta object " + objectId + " plays no role in " + this);
        }
    }

    @Override
    public MehtaObject getMehtaObject(String roleType) {
        Relationship rel = node.getSingleRelationship(getRelationshipType(roleType), Direction.OUTGOING);
        if (rel == null) return null;
        return buildMehtaObject(rel.getEndNode());
    }



    // === MehtaObject Implementation ===

    @Override
    public void indexAttribute(MehtaGraphIndexMode indexMode, Object value, Object oldValue) {
        throw new RuntimeException("MehtaEdge attribute indexing not implemented");
    }

    @Override
    public void indexAttribute(MehtaGraphIndexMode indexMode, String indexKey, Object value, Object oldValue) {
        throw new RuntimeException("MehtaEdge attribute indexing not implemented");
    }



    // === Neo4jMehtaObject Overrides ===

    @Override
    public void delete() {
        // delete the 2 relationships making up this mehta edge
        for (Relationship rel : getRelationships()) {
            rel.delete();
        }
        // delete the auxiliary node
        super.delete();
    }



    // === Java API ===

    @Override
    public String toString() {
        return toString(getMehtaObjects());
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    void addMehtaObject(MehtaObjectRole object) {
        Node dstNode = ((Neo4jMehtaObject) object.getMehtaObject()).getNode();
        node.createRelationshipTo(dstNode, getRelationshipType(object.getRoleType()));
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Returns the 2 relationships making up this mehta edge.
     */
    private Iterable<Relationship> getRelationships() {
        return node.getRelationships(Direction.OUTGOING);
    }

    /**
     * Custom stringifier to avoid endless recursion in case an error occurs in getMehtaObjects().
     */
    private String toString(List<MehtaObjectRole> mehtaObjects) {
        StringBuilder str = new StringBuilder("mehta edge " + getId() + " " + getAttributesString(node));
        for (MehtaObjectRole mehtaObject : mehtaObjects) {
            str.append("\n        " + mehtaObject);
        }
        return str.toString();
    }
}
