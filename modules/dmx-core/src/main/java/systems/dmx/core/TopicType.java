package systems.dmx.core;

import systems.dmx.core.model.TopicTypeModel;



/**
 * Specification of a topic type -- part of DMX's type system, like a class.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface TopicType extends DMXType {

    // === Updating ===

    void update(TopicTypeModel model);
}
