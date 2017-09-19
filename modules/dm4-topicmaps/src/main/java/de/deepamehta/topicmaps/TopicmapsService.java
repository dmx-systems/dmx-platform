package de.deepamehta.topicmaps;

import de.deepamehta.topicmaps.model.TopicmapViewmodel;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.topicmaps.ViewProperties;



public interface TopicmapsService {

    // ------------------------------------------------------------------------------------------------------- Constants

    static final String DEFAULT_TOPICMAP_NAME     = "untitled";
    static final String DEFAULT_TOPICMAP_RENDERER = "dm4.webclient.default_topicmap_renderer";

    // -------------------------------------------------------------------------------------------------- Public Methods

    /**
     * @return  the created Topicmap topic.
     */
    Topic createTopicmap(String name, String topicmapRendererUri, boolean isPrivate);

    // ---

    /**
     * @param   includeChilds   if true the topics contained in the topicmap will include their child topics.
     */
    TopicmapViewmodel getTopicmap(long topicmapId, boolean includeChilds);

    boolean isTopicInTopicmap(long topicmapId, long topicId);

    boolean isAssociationInTopicmap(long topicmapId, long assocId);

    // ---

    /**
     * Adds a topic to a topicmap. If the topic is added already an exception is thrown.
     */
    void addTopicToTopicmap(long topicmapId, long topicId, ViewProperties viewProps);

    /**
     * Convenience method to add a topic with the standard view properties.
     */
    void addTopicToTopicmap(long topicmapId, long topicId, int x, int y, boolean visibility);

    /**
     * Adds an association to a topicmap. If the association is added already an exception is thrown.
     */
    void addAssociationToTopicmap(long topicmapId, long assocId);

    // Note: this is needed in order to reveal a related topic in a *single* request. Otherwise client-sync might fail
    // due to asynchronicity. A client might receive the "addAssoc" WebSocket message *before* the "addTopic" message.
    void addRelatedTopicToTopicmap(long topicmapId, long topicId, long assocId, ViewProperties viewProps);

    // ---

    void setViewProperties(long topicmapId, long topicId, ViewProperties viewProps);

    /**
     * Convenience method to update the "dm4.topicmaps.x" and "dm4.topicmaps.y" standard view properties.
     */
    void setTopicPosition(long topicmapId, long topicId, int x, int y);

    /**
     * Convenience method to update the "dm4.topicmaps.visibility" standard view property.
     */
    void setTopicVisibility(long topicmapId, long topicId, boolean visibility);

    void removeAssociationFromTopicmap(long topicmapId, long assocId);

    // ---

    void setClusterPosition(long topicmapId, ClusterCoords coords);

    void setTopicmapTranslation(long topicmapId, int transX, int transY);

    // ---

    void registerTopicmapRenderer(TopicmapRenderer renderer);

    // ### TODO: unregister needed? Might a renderer hold a stale dm4 instance?

    // ---

    void registerViewmodelCustomizer(ViewmodelCustomizer customizer);

    void unregisterViewmodelCustomizer(ViewmodelCustomizer customizer);
}
