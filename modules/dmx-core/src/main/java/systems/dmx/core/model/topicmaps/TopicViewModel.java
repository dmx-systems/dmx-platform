package systems.dmx.core.model.topicmaps;

import systems.dmx.core.model.TopicModel;



/**
 * A topic viewmodel as contained in a topicmap viewmodel.
 * <p>
 * That is a generic topic model enriched by view properties. Standard view properties are "dm4.topicmaps.x",
 * "dm4.topicmaps.y", and "dm4.topicmaps.visibility". Additional view properties can be added by plugins (by
 * implementing a Viewmodel Customizer).
 */
public interface TopicViewModel extends TopicModel {

    ViewProperties getViewProperties();

    // ---

    /**
     * Convencience method to access the "dm4.topicmaps.x" standard view property.
     */
    int getX();

    /**
     * Convencience method to access the "dm4.topicmaps.y" standard view property.
     */
    int getY();

    /**
     * Convencience method to access the "dm4.topicmaps.visibility" standard view property.
     */
    boolean getVisibility();
}
