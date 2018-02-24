package de.deepamehta.pinning;

import de.deepamehta.topicmaps.TopicmapsService;
import de.deepamehta.topicmaps.ViewmodelCustomizer;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.model.topicmaps.ViewProperties;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Inject;

import java.util.logging.Logger;



public class PinningPlugin extends PluginActivator implements ViewmodelCustomizer {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String PROP_PINNED = "dm5.pinning.pinned";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    // Note: this instance variable is not used but we must declare it in order to initiate service tracking.
    // The Topicmaps service is accessed only on-the-fly within the serviceArrived() and serviceGone() hooks.
    @Inject
    private TopicmapsService topicmapsService;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    // *** Hook Implementations ***

    @Override
    public void serviceArrived(Object service) {
        ((TopicmapsService) service).registerViewmodelCustomizer(this);
    }

    @Override
    public void serviceGone(Object service) {
        // Note 1: unregistering is crucial. Otherwise the Topicmaps plugin would hold a viewmodel customizer with
        // a stale dm4 instance as soon as the DM5 Pinning plugin is redeployed. A subsequent storeViewProperties()
        // call (see below) would fail.
        // Note 2: we must unregister via serviceGone() hook, that is immediately when the Topicmaps service is about
        // to go away. Using the shutdown() hook instead would be too late as the Topicmaps service might already gone.
        ((TopicmapsService) service).unregisterViewmodelCustomizer(this);
    }

    // *** ViewmodelCustomizer Implementation ***

    @Override
    public void enrichViewProperties(RelatedTopic topic, ViewProperties viewProps) {
        Association mapcontextAssoc = topic.getRelatingAssociation();
        if (mapcontextAssoc.hasProperty(PROP_PINNED)) {
            viewProps.put(PROP_PINNED, mapcontextAssoc.getProperty(PROP_PINNED));
        }
    }
}
