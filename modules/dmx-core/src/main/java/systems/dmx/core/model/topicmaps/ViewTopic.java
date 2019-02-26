package systems.dmx.core.model.topicmaps;

import systems.dmx.core.model.TopicModel;



/**
 * A topic viewmodel as contained in a topicmap viewmodel.
 * <p>
 * That is a generic topic model enriched by view properties. Standard view properties are "dmx.topicmaps.x",
 * "dmx.topicmaps.y", and "dmx.topicmaps.visibility". Additional view properties can be added by plugins (by
 * implementing a Viewmodel Customizer).
 */
public interface ViewTopic extends TopicModel {

    ViewProperties getViewProperties();

    // ---

    /**
     * Convencience method to access the "dmx.topicmaps.x" standard view property.
     */
    int getX();

    /**
     * Convencience method to access the "dmx.topicmaps.y" standard view property.
     */
    int getY();

    /**
     * Convencience method to access the "dmx.topicmaps.visibility" standard view property.
     */
    boolean getVisibility();
}
