package systems.dmx.core.service.event;

import systems.dmx.core.RoleType;
import systems.dmx.core.service.EventListener;



public interface IntroduceRoleType extends EventListener {

    void introduceRoleType(RoleType roleType);
}
