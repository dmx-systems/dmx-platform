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

    /**
     * Convenience method for plugin authors to set a Webclient view config value for a certain topic type.
     *
     * @param   topicTypeUri    The URI of the topic type whose view configuration value to set.
     * @param   setting         Last component of the child type URI whose value to set, e.g. "icon".
     * @param   value           The config value (String, Integer, Long, Double, or Boolean).
     */
    protected final void setTopicTypeViewConfigValue(String topicTypeUri, String setting, Object value) {
        setViewConfigValue(dm4.getTopicType(topicTypeUri), setting, value);
    }

    /**
     * Convenience method for plugin authors to set a Webclient view config value for a certain assoc type.
     *
     * @param   assocTypeUri    The URI of the assoc type whose view configuration value to set.
     * @param   setting         Last component of the child type URI whose value to set, e.g. "color".
     * @param   value           The config value (String, Integer, Long, Double, or Boolean).
     */
    protected final void setAssocTypeViewConfigValue(String assocTypeUri, String setting, Object value) {
        setViewConfigValue(dm4.getAssociationType(assocTypeUri), setting, value);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void setViewConfigValue(DeepaMehtaType type, String setting, Object value) {
        type.getViewConfig().setConfigValue("dm4.webclient.view_config", "dm4.webclient." + setting, value);
    }
}
