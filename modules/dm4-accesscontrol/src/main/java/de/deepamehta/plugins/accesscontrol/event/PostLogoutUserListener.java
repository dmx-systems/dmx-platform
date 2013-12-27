package de.deepamehta.plugins.accesscontrol.event;

import de.deepamehta.core.service.Listener;



public interface PostLogoutUserListener extends Listener {

    void postLogoutUser(String username);
}
