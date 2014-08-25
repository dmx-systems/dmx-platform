package de.deepamehta.core;

import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.service.Directives;



/**
 * Specification of a topic type -- part of DeepaMehta's type system, like a class.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface TopicType extends Type {

    // === Updating ===

    void update(TopicTypeModel model, Directives directives);
}
