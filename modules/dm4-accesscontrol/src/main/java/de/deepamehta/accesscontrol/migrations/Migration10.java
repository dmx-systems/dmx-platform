package de.deepamehta.accesscontrol.migrations;

import de.deepamehta.accesscontrol.AccessControlService;

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
 * Part of DM 4.7
 * <p>
 * Moved from migration 4 to migration 10 as the "Login enabled" config
 * topic type (created in migration 9) must exist already.
 */
public class Migration10 extends Migration {

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
