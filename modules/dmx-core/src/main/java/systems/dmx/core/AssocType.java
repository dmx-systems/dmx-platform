package systems.dmx.core;

import systems.dmx.core.model.AssocTypeModel;



/**
 * Part of DMX's type system, like a class whose instances are {@link Assoc}s.
 */
public interface AssocType extends DMXType {

    // === Updating ===

    void update(AssocTypeModel model);
}
