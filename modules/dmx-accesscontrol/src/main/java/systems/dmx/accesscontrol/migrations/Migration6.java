package systems.dmx.accesscontrol.migrations;

import systems.dmx.accesscontrol.AccessControlService;

import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;
import systems.dmx.core.service.accesscontrol.Credentials;



/**
 * Create "admin" user account.
 * <p>
 * Part of DMX 5.0
 * Runs ALWAYS
 * <p>
 * Note: both must exist already, the "Login enabled" config topic type (created in migration 5), and the
 * "Administration" workspace (created in migration 2).
 */
public class Migration6 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private AccessControlService acService;

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        acService.createUserAccount(new Credentials(
            AccessControlService.ADMIN_USERNAME,
            AccessControlService.ADMIN_INITIAL_PASSWORD
        ));
    }
}
