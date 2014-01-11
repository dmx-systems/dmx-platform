package de.deepamehta.plugins.accesscontrol.event;

import de.deepamehta.core.service.EventListener;



public interface PostLogoutUserListener extends EventListener {

    void postLogoutUser(String username);
}
