package systems.dmx.core;

import systems.dmx.core.model.TopicTypeModel;



/**
 * Part of DMX's type system, like a class whose instances are {@link Topic}s.
 */
public interface TopicType extends DMXType {

    // === Updating ===

    void update(TopicTypeModel model);
}
