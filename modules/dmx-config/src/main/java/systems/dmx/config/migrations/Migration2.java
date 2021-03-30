package systems.dmx.config.migrations;

import systems.dmx.core.Assoc;
import systems.dmx.core.service.Migration;
import systems.dmx.core.service.accesscontrol.PrivilegedAccess;



/**
 * Creates workspace assignments for existing "Configuration" assocs.
 * <p>
 * Part of DMX 5.2
 * Runs ALWAYS
 */
public class Migration2 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        PrivilegedAccess pa = dmx.getPrivilegedAccess();
        long workspaceId = pa.getAdminWorkspaceId();
        for (Assoc assoc : dmx.getAssocsByType("dmx.config.configuration")) {
            pa.assignToWorkspace(assoc, workspaceId);
        }
    }
}
