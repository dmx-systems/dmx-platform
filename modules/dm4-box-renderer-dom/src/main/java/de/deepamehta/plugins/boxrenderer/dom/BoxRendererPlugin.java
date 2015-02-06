package de.deepamehta.plugins.boxrenderer.dom;

import de.deepamehta.plugins.topicmaps.ViewmodelCustomizer;
import de.deepamehta.plugins.topicmaps.model.ViewProperties;
import de.deepamehta.plugins.topicmaps.service.TopicmapsService;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.PluginService;

import java.util.logging.Logger;



public class BoxRendererPlugin extends PluginActivator implements ViewmodelCustomizer {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String PROP_COLOR    = "dm4.boxrenderer.color";
    private static final String PROP_EXPANDED = "dm4.boxrenderer.expanded";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    // Note: this instance variable is not used but we must declare it in order to initiate service tracking.
    // The Topicmaps service is accessed only on-the-fly within the serviceArrived() and serviceGone() hooks.
    @Inject
    private TopicmapsService topicmapsService;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    // *** Hook Implementations ***

    @Override
    public void serviceArrived(PluginService service) {
        ((TopicmapsService) service).registerViewmodelCustomizer(this);
    }

    @Override
    public void serviceGone(PluginService service) {
        // Note 1: unregistering is crucial. Otherwise the Topicmaps plugin would hold a viewmodel customizer with
        // a stale dms instance as soon as the Box Renderer is redeployed. A subsequent storeViewProperties() call
        // (see below) would fail.
        // Note 2: we must unregister via serviceGone() hook, that is immediately when the Topicmaps service is about
        // to go away. Using the shutdown() hook instead would be too late as the Topicmaps service might already gone.
        ((TopicmapsService) service).unregisterViewmodelCustomizer(this);
    }

    // *** ViewmodelCustomizer Implementation ***

    @Override
    public void enrichViewProperties(RelatedTopic topic, ViewProperties viewProps) {
        boolean expanded = _enrichViewProperties(topic, viewProps);
        if (expanded) {
            topic.loadChildTopics("dm4.notes.text");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private boolean _enrichViewProperties(RelatedTopic topic, ViewProperties viewProps) {
        Association mapcontextAssoc = topic.getRelatingAssociation();
        // 1) color
        if (mapcontextAssoc.hasProperty(PROP_COLOR)) {
            String color = (String) mapcontextAssoc.getProperty(PROP_COLOR);
            viewProps.put(PROP_COLOR, color);
        }
        // 2) expanded
        boolean expanded = false;
        if (topic.getTypeUri().equals("dm4.notes.note")) {
            if (mapcontextAssoc.hasProperty(PROP_EXPANDED)) {
                expanded = (Boolean) mapcontextAssoc.getProperty(PROP_EXPANDED);
                viewProps.put(PROP_EXPANDED, expanded);
            }
        }
        return expanded;
    }
}
