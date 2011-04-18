package de.deepamehta.core.service;

import de.deepamehta.core.util.JSONHelper;

import java.io.InputStream;



public abstract class Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected CoreService dms;

    // -------------------------------------------------------------------------------------------------- Public Methods

    public void setService(CoreService dms) {
        this.dms = dms;
    }

    public abstract void run();

    // ----------------------------------------------------------------------------------------------- Protected Methods

    protected void readMigrationFile(String migrationFile) {
        InputStream migrationIn = getClass().getResourceAsStream(migrationFile);
        JSONHelper.readMigrationFile(migrationIn, migrationFile, dms);
    }
}
