package de.deepamehta.plugins.accesscontrol.event;

import de.deepamehta.core.service.EventListener;



public interface PostLoginUserListener extends EventListener {

    void postLoginUser(String username);
}
