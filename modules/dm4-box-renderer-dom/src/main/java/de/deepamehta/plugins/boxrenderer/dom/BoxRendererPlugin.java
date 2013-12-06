package de.deepamehta.plugins.boxrenderer.dom;

import de.deepamehta.plugins.topicmaps.ViewmodelCustomizer;
import de.deepamehta.plugins.topicmaps.service.TopicmapsService;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.annotation.ConsumesService;

import java.util.logging.Logger;



public class BoxRendererPlugin extends PluginActivator implements ViewmodelCustomizer {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String PROP_COLOR    = "dm4.boxrenderer.color";
    private static final String PROP_EXPANDED = "dm4.boxrenderer.expanded";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    // *** Hook Implementations ***

    @Override
    @ConsumesService("de.deepamehta.plugins.topicmaps.service.TopicmapsService")
    public void serviceArrived(PluginService service) {
        ((TopicmapsService) service).registerViewmodelCustomizer(this);
    }

    @Override
    public void serviceGone(PluginService service) {
        // Note: unregistering is important. Otherwise the Topicmaps plugin would hold a viewmodel
        // customizer with a stale dms instance as soon as the Box Renderer is redeployed.
        // A subsequent storeViewProperties() call (see below) would fail.
        ((TopicmapsService) service).unregisterViewmodelCustomizer(this);
    }

    // *** ViewmodelCustomizer Implementation ***

    @Override
    public void enrichViewProperties(Topic topic, CompositeValueModel viewProps) {
        boolean expanded = _enrichViewProperties(topic, viewProps);
        if (expanded) {
            topic.loadChildTopics("dm4.notes.text");
        }
    }

    @Override
    public void storeViewProperties(Topic topic, CompositeValueModel viewProps) {
        storeColor(topic, viewProps);
        storeExpanded(topic, viewProps);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private boolean _enrichViewProperties(Topic topic, CompositeValueModel viewProps) {
        // 1) color
        if (topic.hasProperty(PROP_COLOR)) {
            String color = (String) topic.getProperty(PROP_COLOR);
            viewProps.put(PROP_COLOR, color);
        }
        // 2) expanded
        boolean expanded = false;
        if (topic.getTypeUri().equals("dm4.notes.note")) {
            if (topic.hasProperty(PROP_EXPANDED)) {
                expanded = (Boolean) topic.getProperty(PROP_EXPANDED);
                viewProps.put(PROP_EXPANDED, expanded);
            }
        }
        return expanded;
    }

    // ---

    private void storeColor(Topic topic, CompositeValueModel viewProps) {
        if (viewProps.has(PROP_COLOR)) {
            String color = viewProps.getString(PROP_COLOR);
            topic.setProperty(PROP_COLOR, color, false);        // addToIndex = false
        }
    }

    private void storeExpanded(Topic topic, CompositeValueModel viewProps) {
        if (viewProps.has(PROP_EXPANDED)) {
            boolean expanded = viewProps.getBoolean(PROP_EXPANDED);
            topic.setProperty(PROP_EXPANDED, expanded, false);  // addToIndex = false
            // ### TODO: store the expanded flag *per-topicmap*
        }
    }
}
