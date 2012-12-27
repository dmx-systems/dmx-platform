package de.deepamehta.mehtagraph.spi;

import de.deepamehta.mehtagraph.MehtaObjectRole;

import java.util.List;
import java.util.Set;



/**
 * The MehtaGraph service.
 * It provides methods for creation and retrieval of {@link MehtaNode}s and {@link MehtaEdge}s.
 * <p>
 * To obtain a MehtaGraph service instance call {@link MehtaGraphFactory#createInstance}.
 */
public interface MehtaGraph {

    // === Mehta Nodes ===

    MehtaNode createMehtaNode();

    MehtaNode getMehtaNode(long id);
    MehtaNode getMehtaNode(String key, Object value);

    // ###
    List<MehtaNode> getMehtaNodes(String key, Object value);

    List<MehtaNode> queryMehtaNodes(Object value);
    List<MehtaNode> queryMehtaNodes(String key, Object value);

    // === Mehta Edges ===

    MehtaEdge createMehtaEdge(MehtaObjectRole object1, MehtaObjectRole object2);

    MehtaEdge getMehtaEdge(long id);

    Set<MehtaEdge> getMehtaEdges(long node1Id, long node2Id);
    Set<MehtaEdge> getMehtaEdges(long node1Id, long node2Id, String roleType1, String roleType2);
    Set<MehtaEdge> getMehtaEdgesBetweenNodeAndEdge(long nodeId, long edgeId, String nodeRoleType, String edgeRoleType);

    // === Mehta Objects ===

    MehtaObject getMehtaObject(long id);

    // === Misc ===

    MehtaGraphTransaction beginTx();
    void shutdown();
}
