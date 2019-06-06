package systems.dmx.files.event;

import systems.dmx.core.service.EventListener;



public interface CheckDiskQuota extends EventListener {

    void checkDiskQuota(String username, long fileSize, long diskQuota);
}
