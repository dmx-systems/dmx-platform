package de.deepamehta.plugins.accesscontrol.migrations;

import de.deepamehta.plugins.accesscontrol.service.AccessControlService;

import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;
import de.deepamehta.core.service.accesscontrol.Credentials;



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

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private AccessControlService acService;

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        acService.createUserAccount(new Credentials(
            AccessControlService.ADMIN_USERNAME,
            AccessControlService.ADMIN_DEFAULT_PASSWORD
        ));
    }
}
