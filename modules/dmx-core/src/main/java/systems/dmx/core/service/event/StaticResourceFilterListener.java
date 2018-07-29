package systems.dmx.core.service.event;

import systems.dmx.core.service.EventListener;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



public interface StaticResourceFilterListener extends EventListener {

    void staticResourceFilter(HttpServletRequest request, HttpServletResponse response);
}
