package de.deepamehta.plugins.accesscontrol.migrations;

import de.deepamehta.core.service.Migration;



/**
 * Creates the "admin" user account.
 * Runs only in CLEAN_INSTALL mode.
 * <p>
 * Note: when UPDATEing to 4.5 the "admin" user account already exists.
 * It was created via postInstall() hook (which is obsolete in 4.5)
 * <p>
 * Part of DM 4.5
 */
public class Migration4 extends Migration {

    @Override
    public void run() {
        // content moved to Migration10.java as the "Login enabled" config topic type (created
        // in migration 9 as of DM 4.7) must exist already
    }
}
