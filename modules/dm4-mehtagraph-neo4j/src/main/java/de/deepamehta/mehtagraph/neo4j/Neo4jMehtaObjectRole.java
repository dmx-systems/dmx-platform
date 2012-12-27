package de.deepamehta.mehtagraph.neo4j;

import de.deepamehta.mehtagraph.MehtaObjectRole;
import de.deepamehta.mehtagraph.spi.MehtaObject;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;



class Neo4jMehtaObjectRole extends MehtaObjectRole {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Relationship rel;

    // ---------------------------------------------------------------------------------------------------- Constructors

    Neo4jMehtaObjectRole(MehtaObject mehtaObject, String roleType, Relationship rel) {
        super(mehtaObject, roleType);
        this.rel = rel;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // === MehtaObjectRole Overrides ===

    @Override
    public void setRoleType(String roleType) {
        // update memory
        super.setRoleType(roleType);
        // update DB
        storeRoleType(roleType);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void storeRoleType(String roleType) {
        Node startNode = rel.getStartNode();
        Node endNode = rel.getEndNode();
        rel.delete();
        RelationshipType relType = ((Neo4jMehtaObject) getMehtaObject()).getRelationshipType(roleType);
        startNode.createRelationshipTo(endNode, relType);
    }
}
