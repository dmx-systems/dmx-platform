package de.deepamehta.core.storage.spi;

import de.deepamehta.core.ResultSet;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.storage.MehtaObjectRole;

import java.util.List;
import java.util.Set;



/**
 * The MehtaGraph service. ### FIXDOC
 * It provides methods for creation and retrieval of {@link MehtaNode}s and {@link MehtaEdge}s.
 * <p>
 * To obtain a MehtaGraph service instance call {@link MehtaGraphFactory#createInstance}.
 */
public interface MehtaGraph {



    // === Topics ===

    void createMehtaNode(TopicModel topicModel);

    TopicModel getMehtaNode(long id);
    TopicModel getMehtaNode(String key, Object value);

    List<TopicModel> getMehtaNodes(String key, Object value);

    List<TopicModel> queryMehtaNodes(Object value);
    List<TopicModel> queryMehtaNodes(String key, Object value);

    void setTopicUri(long topicId, String uri);
    void setTopicValue(long topicId, SimpleValue value, IndexMode indexMode, String indexKey);

    void deleteTopic(long topicId);



    // === Associations ===

    MehtaEdge createMehtaEdge(MehtaObjectRole object1, MehtaObjectRole object2);

    MehtaEdge getMehtaEdge(long id);

    Set<MehtaEdge> getMehtaEdges(long node1Id, long node2Id);
    Set<MehtaEdge> getMehtaEdges(long node1Id, long node2Id, String roleType1, String roleType2);
    Set<MehtaEdge> getMehtaEdgesBetweenNodeAndEdge(long nodeId, long edgeId, String nodeRoleType, String edgeRoleType);



    // === Mehta Objects ### TODO ===

    MehtaObject getMehtaObject(long id);



    // === Traversal ===

    ResultSet<RelatedTopicModel> getTopicRelatedTopics(long topicId, String assocTypeUri, String myRoleTypeUri,
                                            String othersRoleTypeUri, String othersTopicTypeUri, int maxResultSize);

    Set<RelatedAssociationModel> getTopicRelatedAssociations(long topicId, String assocTypeUri, String myRoleTypeUri,
                                            String othersRoleTypeUri, String othersAssocTypeUri);

    // ---

    Set<AssociationModel> getTopicAssociations(long topicId);

    Set<AssociationModel> getAssociationAssociations(long assocId);



    // === Misc ===

    MehtaGraphTransaction beginTx();
    void shutdown();
}
