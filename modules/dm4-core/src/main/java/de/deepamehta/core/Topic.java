package de.deepamehta.core;

import de.deepamehta.core.model.TopicModel;

import java.util.List;



/**
 * Specification of a topic -- DeepaMehta's central data object.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface Topic extends DeepaMehtaObject {



    // === Model ===

    Topic loadChildTopics();
    Topic loadChildTopics(String childTypeUri);

    // ---

    TopicModel getModel();



    // === Updating ===

    void update(TopicModel model);
}
