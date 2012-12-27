package de.deepamehta.mehtagraph.neo4j;

import de.deepamehta.mehtagraph.spi.MehtaNode;

import org.neo4j.graphdb.Node;



class Neo4jMehtaNode extends Neo4jMehtaObject implements MehtaNode {

    // ---------------------------------------------------------------------------------------------------- Constructors

    Neo4jMehtaNode(Node node, Neo4jBase base) {
        super(node, base);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String toString() {
        return "mehta node " + getId() + " " + getAttributesString(node);
    }
}
