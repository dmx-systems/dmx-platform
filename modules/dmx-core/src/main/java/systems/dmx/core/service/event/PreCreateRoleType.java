package systems.dmx.core.service.event;

import systems.dmx.core.model.RoleTypeModel;
import systems.dmx.core.service.EventListener;



public interface PreCreateRoleType extends EventListener {

    void preCreateRoleType(RoleTypeModel model);
}
