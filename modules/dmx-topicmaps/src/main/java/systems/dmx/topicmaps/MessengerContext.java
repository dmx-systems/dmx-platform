package systems.dmx.topicmaps;

import systems.dmx.core.service.CoreService;

import javax.servlet.http.HttpServletRequest;



interface MessengerContext {

    CoreService getCoreService();

    HttpServletRequest getRequest();
}
