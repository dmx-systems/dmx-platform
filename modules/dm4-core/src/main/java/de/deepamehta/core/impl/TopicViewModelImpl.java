package de.deepamehta.core.impl;

import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.topicmaps.TopicViewModel;
import de.deepamehta.core.model.topicmaps.ViewProperties;

import org.codehaus.jettison.json.JSONObject;



class TopicViewModelImpl extends TopicModelImpl implements TopicViewModel {

    // --- Instance Variables ---

    private ViewProperties viewProps;

    // --- Constructors ---

    TopicViewModelImpl(TopicModelImpl topic, ViewProperties viewProps) {
        super(topic);
        this.viewProps = viewProps;
    }

    // --- Public Methods ---

    public ViewProperties getViewProperties() {
        return viewProps;
    }

    // ---

    public int getX() {
        return viewProps.getInt("dm4.topicmaps.x");
    }

    public int getY() {
        return viewProps.getInt("dm4.topicmaps.y");
    }

    public boolean getVisibility() {
        return viewProps.getBoolean("dm4.topicmaps.visibility");
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            o.put("view_props", viewProps.toJSON());
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }
}
