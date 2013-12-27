package de.deepamehta.plugins.accesscontrol.event;

import de.deepamehta.core.service.Listener;



public interface PostLoginUserListener extends Listener {

    void postLoginUser(String username);
}
