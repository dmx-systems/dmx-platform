package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.ViewConfiguration;
import de.deepamehta.core.model.ViewConfigurationModel;



/**
 * View configurations that are attached to the {@link CoreService}.
 */
class AttachedViewConfiguration implements ViewConfiguration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private final ViewConfigurationModel model;
    private final EmbeddedService dms;

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedViewConfiguration(ViewConfigurationModel model, EmbeddedService dms) {
        this.model = model;
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Iterable<TopicModel> getConfigTopics() {
        return model.getConfigTopics();
    }
}
