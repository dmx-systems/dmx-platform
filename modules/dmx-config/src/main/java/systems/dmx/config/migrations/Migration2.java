package systems.dmx.config.migrations;

import static systems.dmx.config.Constants.*;
import systems.dmx.core.Assoc;
import systems.dmx.core.service.Migration;
import systems.dmx.core.service.accesscontrol.PrivilegedAccess;



/**
 * Creates missing workspace assignments for existing "Configuration" assocs.
 * <p>
 * Part of DMX 5.2
 * Runs only in UPDATE mode.
 */
public class Migration2 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        PrivilegedAccess pa = dmx.getPrivilegedAccess();
        long workspaceId = pa.getAdminWorkspaceId();
        for (Assoc assoc : dmx.getAssocsByType(CONFIGURATION)) {
            pa.assignToWorkspace(assoc, workspaceId);
        }
    }
}
