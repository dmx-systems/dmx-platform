package systems.dmx.accountmanagement.migrations;

import systems.dmx.accesscontrol.AccessControlService;
import systems.dmx.accountmanagement.AccountManagementService;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;
import systems.dmx.core.service.accesscontrol.Credentials;

import java.util.logging.Logger;



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

    @Inject
    private AccessControlService accessControlService;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        if (accessControlService.getUsernameTopic(AccessControlService.ADMIN_USERNAME) == null) {
            accountManagementService.createUserAccount(new Credentials(
                AccessControlService.ADMIN_USERNAME,
                AccessControlService.ADMIN_INITIAL_PASSWORD
            ));
        } else {
            // if platform is *updated* to 5.3.5 or later the admin account was created by dmx-accesscontrol already
            logger.info("### Creating admin account SKIPPED -- exists already");
        }
    }
}
