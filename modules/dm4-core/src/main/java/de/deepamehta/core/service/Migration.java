package de.deepamehta.core.service;

import de.deepamehta.core.Type;
import de.deepamehta.core.util.DeepaMehtaUtils;

import java.io.InputStream;



public abstract class Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected DeepaMehtaService dms;

    // -------------------------------------------------------------------------------------------------- Public Methods

    public void setCoreService(DeepaMehtaService dms) {
        this.dms = dms;
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

    // ---

    protected final void readMigrationFile(String migrationFile) {
        InputStream migrationIn = getClass().getResourceAsStream(migrationFile);
        DeepaMehtaUtils.readMigrationFile(migrationIn, migrationFile, dms);
    }
}
