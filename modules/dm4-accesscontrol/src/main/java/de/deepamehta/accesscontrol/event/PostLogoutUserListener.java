package de.deepamehta.accesscontrol.event;

import de.deepamehta.core.service.EventListener;



public interface PostLogoutUserListener extends EventListener {

    void postLogoutUser(String username);
}
