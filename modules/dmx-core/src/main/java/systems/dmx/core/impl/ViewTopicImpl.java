package systems.dmx.core.impl;

import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.topicmaps.ViewTopic;
import systems.dmx.core.model.topicmaps.ViewProps;

import org.codehaus.jettison.json.JSONObject;



// TODO: rethink inheritance. Can we have a common "ObjectViewModel" for both, topics and assocs?
// Is this a case for Java 8 interfaces, which can have a default implementation?
class ViewTopicImpl extends TopicModelImpl implements ViewTopic {

    // --- Instance Variables ---

    private ViewProps viewProps;

    // --- Constructors ---

    ViewTopicImpl(TopicModelImpl topic, ViewProps viewProps) {
        super(topic);
        this.viewProps = viewProps;
    }

    // --- Public Methods ---

    public ViewProps getViewProps() {
        return viewProps;
    }

    // ---

    public int getX() {
        return viewProps.getInt("dmx.topicmaps.x");
    }

    public int getY() {
        return viewProps.getInt("dmx.topicmaps.y");
    }

    public boolean getVisibility() {
        return viewProps.getBoolean("dmx.topicmaps.visibility");
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            return super.toJSON().put("viewProps", viewProps.toJSON());
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }
}
