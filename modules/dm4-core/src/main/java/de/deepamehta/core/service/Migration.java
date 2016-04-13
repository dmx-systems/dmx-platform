package de.deepamehta.core.service;

import de.deepamehta.core.DeepaMehtaType;
import de.deepamehta.core.service.ModelFactory;



public abstract class Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected CoreService dm4;
    protected ModelFactory mf;

    // -------------------------------------------------------------------------------------------------- Public Methods

    // ### TODO: make this internal. Define a public Migration interface?
    public void setCoreService(CoreService dm4) {
        this.dm4 = dm4;
        this.mf = dm4.getModelFactory();
    }

    public abstract void run();

    // ----------------------------------------------------------------------------------------------- Protected Methods

    // Convenience method ### FIXME: belongs to Webclient module
    protected final void addTopicTypeSetting(String topicTypeUri, String setting, Object value) {
        addTypeSetting(dm4.getTopicType(topicTypeUri), setting, value);
    }

    // Convenience method ### FIXME: belongs to Webclient module
    protected final void addAssociationTypeSetting(String assocTypeUri, String setting, Object value) {
        addTypeSetting(dm4.getAssociationType(assocTypeUri), setting, value);
    }

    // ---

    // Convenience method ### FIXME: belongs to Webclient module
    protected final void addTypeSetting(DeepaMehtaType type, String setting, Object value) {
        type.getViewConfig().addSetting("dm4.webclient.view_config", "dm4.webclient." + setting, value);
    }
}
