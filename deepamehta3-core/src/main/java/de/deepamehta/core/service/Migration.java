package de.deepamehta.core.service;

import de.deepamehta.core.Type;
import de.deepamehta.core.util.JSONHelper;

import java.io.InputStream;



public abstract class Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected DeepaMehtaService dms;

    // -------------------------------------------------------------------------------------------------- Public Methods

    public void setService(DeepaMehtaService dms) {
        this.dms = dms;
    }

    public abstract void run();

    // ----------------------------------------------------------------------------------------------- Protected Methods

    protected final void addTopicTypeSetting(String topicTypeUri, String setting, Object value) {
        addTypeSetting(dms.getTopicType(topicTypeUri, null), setting, value);
    }

    protected final void addAssociationTypeSetting(String assocTypeUri, String setting, Object value) {
        addTypeSetting(dms.getAssociationType(assocTypeUri, null), setting, value);
    }

    // ---

    protected final void addTypeSetting(Type type, String setting, Object value) {
        type.getViewConfig().addSetting("dm4.webclient.view_config", "dm4.webclient." + setting, value);
    }

    // ---

    protected final void readMigrationFile(String migrationFile) {
        InputStream migrationIn = getClass().getResourceAsStream(migrationFile);
        JSONHelper.readMigrationFile(migrationIn, migrationFile, dms);
    }
}
