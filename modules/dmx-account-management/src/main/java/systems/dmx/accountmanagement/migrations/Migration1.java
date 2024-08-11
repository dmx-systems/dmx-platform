package systems.dmx.accountmanagement.migrations;

import systems.dmx.accesscontrol.AccessControlService;
import systems.dmx.accountmanagement.AccountManagementService;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;
import systems.dmx.core.service.accesscontrol.Credentials;


/**
 * Creates "admin" user account.
 * <p>
 * Part of DMX 5.3.5
 * Runs ALWAYS
 * <p>
 * Note: Depends on migrations from dmx-accesscontrol
 */
public class Migration1 extends Migration {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private AccountManagementService accountManagementService;

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        accountManagementService.createUserAccount(new Credentials(
            AccessControlService.ADMIN_USERNAME,
            AccessControlService.ADMIN_INITIAL_PASSWORD
        ));
    }
}
