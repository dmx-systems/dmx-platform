package systems.dmx.files;

import systems.dmx.config.ConfigService;
import systems.dmx.core.Topic;
import systems.dmx.core.osgi.CoreActivator;
import systems.dmx.core.service.CoreService;
import systems.dmx.core.service.accesscontrol.PrivilegedAccess;
import static systems.dmx.files.Constants.*;

import javax.servlet.http.HttpServletRequest;



class DiskQuotaCheck {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    HttpServletRequest request;
    CoreService dmx;

    // ---------------------------------------------------------------------------------------------------- Constructors

    DiskQuotaCheck(HttpServletRequest request, CoreService dmx) {
        this.request = request;
        this.dmx = dmx;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void check(long fileSize) {
        // Note: we can't use AccessControlService (cyclic dependency), so we do privileged access
        PrivilegedAccess pa = dmx.getPrivilegedAccess();
        if (!pa.inRequestScope(request)) {
            return;
        }
        String username = pa.getUsername(request);
        if (username == null) {
            throw new RuntimeException("User <anonymous> has no disk quota");
        }
        dmx.fireEvent(FilesPlugin.CHECK_DISK_QUOTA, username, fileSize, diskQuota(username));
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private long diskQuota(String username) {
        Topic usernameTopic = dmx.getPrivilegedAccess().getUsernameTopic(username);
        ConfigService cs = CoreActivator.getService(ConfigService.class);
        Topic configTopic = cs.getConfigTopic(DISK_QUOTA, usernameTopic.getId());
        return 1024 * 1024 * configTopic.getSimpleValue().intValue();
    }
}
