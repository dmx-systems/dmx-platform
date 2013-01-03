package de.deepamehta.core.storage.spi;

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
    void setTopicValue(long topicId, SimpleValue value, Set<IndexMode> indexModes, String indexKey);

    Object getTopicProperty(long topicId, String key);
    void setTopicProperty(long topicId, String key, Object value);
    boolean hasTopicProperty(long topicId, String key);

    void deleteTopic(long topicId);



    // === Associations ===

    MehtaEdge createMehtaEdge(MehtaObjectRole object1, MehtaObjectRole object2);

    AssociationModel getMehtaEdge(long id);

    Set<AssociationModel> getMehtaEdges(String assocTypeUri, long topicId1, long topicId2, String roleTypeUri1,
                                                                                           String roleTypeUri2);

    Set<AssociationModel> getMehtaEdgesBetweenNodeAndEdge(String assocTypeUri, long topicId, long assocId,
                                                          String topicRoleTypeUri, String assocRoleTypeUri);

    // ---

    void storeAssociationUri(long assocId, String uri);
    void storeAssociationValue(long assocId, SimpleValue value, Set<IndexMode> indexModes, String indexKey);

    void storeRoleTypeUri(long assocId, long playerId, String roleTypeUri);



    // === Mehta Objects ### TODO ===

    MehtaObject getMehtaObject(long id);



    // === Traversal ===

    Set<AssociationModel> getTopicAssociations(long topicId);

    Set<AssociationModel> getAssociationAssociations(long assocId);

    // ---

    Set<RelatedTopicModel> getTopicRelatedTopics(long topicId, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersTopicTypeUri);

    Set<RelatedAssociationModel> getTopicRelatedAssociations(long topicId, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri);

    // ---

    Set<RelatedTopicModel> getAssociationRelatedTopics(long assocId, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersTopicTypeUri);

    Set<RelatedAssociationModel> getAssociationRelatedAssociations(long assocId, String assocTypeUri,
                                             String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri);



    // === Misc ===

    MehtaGraphTransaction beginTx();
    void shutdown();
}
