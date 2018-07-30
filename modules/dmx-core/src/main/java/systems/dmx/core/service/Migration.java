package systems.dmx.core.service;

import systems.dmx.core.DMXType;
import systems.dmx.core.service.ModelFactory;



public abstract class Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected CoreService dm4;
    protected ModelFactory mf;

    // -------------------------------------------------------------------------------------------------- Public Methods

    // ### TODO: make this internal. Define a public Migration interface?
    public void setCoreService(CoreService dm4) {
        this.dm4 = dm4;
        this.mf = dmx.getModelFactory();
    }

    public abstract void run();

    // ----------------------------------------------------------------------------------------------- Protected Methods

    // Note: exceptionally here the Core has some knowledge about the Webclient.
    // ### TODO: move these methods to the Webclient service.

    /**
     * Convenience method for plugin authors to set a Webclient view config value for a certain topic type.
     *
     * @param   topicTypeUri    The URI of the topic type whose view configuration value to set.
     * @param   setting         Last component of the child type URI whose value to set, e.g. "icon".
     * @param   value           The config value (String, Integer, Long, Double, or Boolean).
     */
    protected final void setTopicTypeViewConfigValue(String topicTypeUri, String setting, Object value) {
        setViewConfigValue(dmx.getTopicType(topicTypeUri), setting, value);
    }

    /**
     * Convenience method for plugin authors to set a Webclient view config value for a certain assoc type.
     *
     * @param   assocTypeUri    The URI of the assoc type whose view configuration value to set.
     * @param   setting         Last component of the child type URI whose value to set, e.g. "color".
     * @param   value           The config value (String, Integer, Long, Double, or Boolean).
     */
    protected final void setAssocTypeViewConfigValue(String assocTypeUri, String setting, Object value) {
        setViewConfigValue(dmx.getAssociationType(assocTypeUri), setting, value);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void setViewConfigValue(DMXType type, String setting, Object value) {
        type.getViewConfig().setConfigValue("dmx.webclient.view_config", "dmx.webclient." + setting, value);
    }
}
