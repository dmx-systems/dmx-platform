package de.deepamehta.core;

import de.deepamehta.core.model.TopicModel;



/**
 * Specification of a topic -- DeepaMehta's central data object.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface Topic extends DMXObject {

    void update(TopicModel model);

    // ---

    /**
     * Searches this topic's child topics for a topic of the given type.
     * Only the child topics which are already loaded into memory are searched; the DB is not accessed.
     * <p>
     * The first topic found is returned, according to a depth-first search.
     * For multiple-value childs the first topic is returned.
     * If the given type matches this topic directly it is returned immediately.
     * <p>
     * The search is driven by this topic's type definition. That is child topics which do not adhere
     * to the type definition are not found.
     * <p>
     * TODO: make this generic by moving to DMXObject interface?
     *
     * @return  the found topic, or <code>null</code>.
     */
    Topic findChildTopic(String topicTypeUri);

    // ---

    @Override
    Topic loadChildTopics();

    @Override
    Topic loadChildTopics(String assocDefUri);

    // ---

    @Override
    TopicModel getModel();
}
