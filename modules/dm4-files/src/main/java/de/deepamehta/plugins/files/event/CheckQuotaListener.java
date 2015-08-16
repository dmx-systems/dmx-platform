package de.deepamehta.plugins.files.event;

import de.deepamehta.core.service.EventListener;



public interface CheckQuotaListener extends EventListener {

    void checkQuota(long fileSize, long userQuota);
}
