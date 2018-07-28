package de.deepamehta.files.event;

import de.deepamehta.core.service.EventListener;



public interface CheckDiskQuotaListener extends EventListener {

    void checkDiskQuota(String username, long fileSize, long diskQuota);
}
