package systems.dmx.accountmanagement.migrations;

import systems.dmx.accesscontrol.AccessControlService;
import systems.dmx.accountmanagement.AccountManagementService;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;
import systems.dmx.core.service.accesscontrol.Credentials;


/**
 * Creates "admin" user account.
 * <p>
 * Part of DMX 5.0-beta-4
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
        try {
            accountManagementService.createUserAccount(new Credentials(
                    AccessControlService.ADMIN_USERNAME,
                    AccessControlService.ADMIN_INITIAL_PASSWORD
            ));
        } catch (Throwable e) {
            // TODO: Will certainly fail if a dmx-accesscontrol has created the user already.
            // TODO: Remove this try-catch when both implementations are working together
        }
    }
}
