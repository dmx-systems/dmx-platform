package de.deepamehta.core.service;

import de.deepamehta.core.Type;
import de.deepamehta.core.service.ModelFactory;



public abstract class Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected DeepaMehtaService dms;
    protected ModelFactory mf;

    // -------------------------------------------------------------------------------------------------- Public Methods

    // ### TODO: make this internal. Define a public Migration interface?
    public void setCoreService(DeepaMehtaService dms) {
        this.dms = dms;
        this.mf = dms.getModelFactory();
    }

    public abstract void run();

    // ----------------------------------------------------------------------------------------------- Protected Methods

    // Convenience method ### FIXME: belongs to Webclient module
    protected final void addTopicTypeSetting(String topicTypeUri, String setting, Object value) {
        addTypeSetting(dms.getTopicType(topicTypeUri), setting, value);
    }

    // Convenience method ### FIXME: belongs to Webclient module
    protected final void addAssociationTypeSetting(String assocTypeUri, String setting, Object value) {
        addTypeSetting(dms.getAssociationType(assocTypeUri), setting, value);
    }

    // ---

    // Convenience method ### FIXME: belongs to Webclient module
    protected final void addTypeSetting(Type type, String setting, Object value) {
        type.getViewConfig().addSetting("dm4.webclient.view_config", "dm4.webclient." + setting, value);
    }
}
