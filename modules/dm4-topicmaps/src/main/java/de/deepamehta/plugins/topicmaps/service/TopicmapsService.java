package de.deepamehta.plugins.topicmaps.service;

import de.deepamehta.plugins.topicmaps.ClusterCoords;
import de.deepamehta.plugins.topicmaps.TopicmapRenderer;
import de.deepamehta.plugins.topicmaps.TopicmapViewmodel;
import de.deepamehta.plugins.topicmaps.ViewmodelCustomizer;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;



public interface TopicmapsService extends PluginService {

    TopicmapViewmodel getTopicmap(long topicmapId);

    // ---

    Topic createTopicmap(String name,             String topicmapRendererUri, ClientState clientState);
    Topic createTopicmap(String name, String uri, String topicmapRendererUri, ClientState clientState);

    // ---

    void addTopicToTopicmap(long topicmapId, long topicId, CompositeValueModel viewProps);

    void addAssociationToTopicmap(long topicmapId, long assocId);

    // ---

    void setViewProperties(long topicmapId, long topicId, CompositeValueModel viewProps);

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

    void setTopicmapTranslation(long topicmapId, int trans_x, int trans_y);

    // ---

    void registerTopicmapRenderer(TopicmapRenderer renderer);
    // ### TODO: unregister needed? Might a renderer hold a stale dms instance?

    void registerViewmodelCustomizer(ViewmodelCustomizer customizer);
    void unregisterViewmodelCustomizer(ViewmodelCustomizer customizer);
}
