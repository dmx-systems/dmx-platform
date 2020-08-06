package systems.dmx.core;

import systems.dmx.core.model.TopicModel;



/**
 * A topic -- DMX's central data object.
 *
 * @author <a href="mailto:jri@dmx.berlin">Jörg Richter</a>
 */
public interface Topic extends DMXObject {

    /**
     * Searches this topic's child topics for a topic of the given type.
     * Only the child topics which are already loaded into memory are searched; the DB is not accessed.
     * <p>
     * The first topic found is returned, according to a depth-first search.
     * For multiple-value children the first topic is returned.
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
    TopicModel getModel();
}
