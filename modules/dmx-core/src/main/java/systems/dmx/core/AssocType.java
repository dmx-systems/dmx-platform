package systems.dmx.core;

import systems.dmx.core.model.AssocTypeModel;



public interface AssocType extends DMXType {

    // === Updating ===

    void update(AssocTypeModel model);
}
