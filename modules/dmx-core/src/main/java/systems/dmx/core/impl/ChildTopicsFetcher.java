package systems.dmx.core.impl;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.DMXObjectModel;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.util.DMXUtils;

import java.util.List;
import java.util.logging.Logger;



class ChildTopicsFetcher {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AccessLayer al;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    ChildTopicsFetcher(AccessLayer al) {
        this.al = al;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * Fetches the child topic models (recursively) of the given object model and updates it in-place.
     * ### TODO: recursion is required in some cases (e.g. when fetching a topic through REST API) but is possibly
     * overhead in others (e.g. when updating composite structures).
     * <p>
     * Works for both, "one" and "many" comp defs.
     *
     * @param   compDef     The child topic models according to this comp def are fetched.
     */
    void fetch(DMXObjectModel object, CompDefModel compDef, boolean deep) {
        try {
            ChildTopicsModel childTopics = object.getChildTopics();
            String cardinalityUri = compDef.getChildCardinalityUri();
            String compDefUri     = compDef.getCompDefUri();
            if (cardinalityUri.equals(ONE)) {
                RelatedTopicModelImpl childTopic = fetchChildTopic(object.getId(), compDef);
                // Note: topics just created have no child topics yet
                if (childTopic != null) {
                    childTopics.set(compDefUri, childTopic);
                    if (deep) {
                        fetchChildTopics(childTopic, deep);    // recursion
                    }
                }
            } else if (cardinalityUri.equals(MANY)) {
                List<? extends RelatedTopicModel> _childTopics = fetchChildTopics(object.getId(), compDef);
                int a = 0;
                for (RelatedAssocModelImpl assoc : newChildTopicsSequence(object.getId(), compDef)) {
                    RelatedTopicModel childTopic = DMXUtils.findByAssocId(assoc.id, _childTopics);
                    if (childTopic == null) {
                        throw new RuntimeException("DB inconsistency: assoc " + assoc.id +
                            " is in sequence but not in " + _childTopics + ", assoc=" + assoc + ", child topic=" +
                            assoc.getDMXObjectByRole(CHILD));
                    }
                    childTopics.add(compDefUri, childTopic);
                    a++;
                }
                if (a != _childTopics.size()) {
                    if (a > 0) {
                        throw new RuntimeException("DB inconsistency: " + a + " values in sequence when " +
                            "there should be " + _childTopics.size() + ", parentTopicId=" + object.getId() +
                            ", compDefUri=\"" + compDefUri + "\"");
                    } else if (_childTopics.size() > 0) {
                        logger.info("### No sequence for " + _childTopics.size() + " \"" + compDefUri + "\" values");
                        for (RelatedTopicModel childTopic : _childTopics) {
                            childTopics.add(compDefUri, childTopic);
                        }
                    }
                }
                if (deep) {
                    List<? extends RelatedTopicModel> topics = childTopics.getTopicsOrNull(compDefUri);
                    if (topics != null) {
                        for (RelatedTopicModel topic : topics) {
                            fetchChildTopics((DMXObjectModelImpl) topic, deep);    // recursion
                        }
                    }
                }
            } else {
                throw new RuntimeException("\"" + cardinalityUri + "\" is an unexpected cardinality URI");
            }
        } catch (Exception e) {
            throw new RuntimeException("Fetching the \"" + compDef.getCompDefUri() + "\" child topics of object " +
                object.getId() + " failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    // ### TODO: copy in ValueIntegrator
    private ChildTopicsSequence newChildTopicsSequence(long parentTopicId, CompDefModel compDef) {
        return new ChildTopicsSequence(
            parentTopicId,
            compDef.getChildTypeUri(),
            compDef.getInstanceLevelAssocTypeUri(),
            al
        );
    }

    /**
     * Fetches the child topic models (recursively) of the given object model and updates it in-place.
     * ### TODO: recursion is required in some cases (e.g. when fetching a topic through REST API) but is possibly
     * overhead in others (e.g. when updating composite structures).
     */
    private void fetchChildTopics(DMXObjectModelImpl object, boolean deep) {
        for (CompDefModel compDef : object.getType().getCompDefs()) {
            fetch(object, compDef, deep);
        }
    }

    // ---

    /**
     * Fetches and returns a child topic or <code>null</code> if no such topic extists.
     */
    private RelatedTopicModelImpl fetchChildTopic(long objectId, CompDefModel compDef) {
        return al.sd.fetchRelatedTopic(         // direct DB access is required as sequence is not per-user
            objectId,
            compDef.getInstanceLevelAssocTypeUri(),
            PARENT, CHILD,
            compDef.getChildTypeUri()
        );
    }

    private List<RelatedTopicModelImpl> fetchChildTopics(long objectId, CompDefModel compDef) {
        return al.db.fetchRelatedTopics(        // direct DB access is required as sequence is not per-user
            objectId,
            compDef.getInstanceLevelAssocTypeUri(),
            PARENT, CHILD,
            compDef.getChildTypeUri()
        );
    }
}
