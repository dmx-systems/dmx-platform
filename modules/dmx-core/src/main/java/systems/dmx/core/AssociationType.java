package systems.dmx.core;

import systems.dmx.core.model.AssociationTypeModel;



public interface AssociationType extends DMXType {

    // === Updating ===

    void update(AssociationTypeModel model);
}
