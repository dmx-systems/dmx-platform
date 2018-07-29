package systems.dmx.core.impl;

import systems.dmx.core.model.AssociationDefinitionModel;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.DMXObjectModel;

import java.util.List;
import java.util.logging.Logger;



class ChildTopicsFetcher {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private PersistenceLayer pl;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    ChildTopicsFetcher(PersistenceLayer pl) {
        this.pl = pl;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * Fetches the child topic models (recursively) of the given object model and updates it in-place.
     * ### TODO: recursion is required in some cases (e.g. when fetching a topic through REST API) but is possibly
     * overhead in others (e.g. when updating composite structures).
     * <p>
     * Works for both, "one" and "many" association definitions.
     *
     * @param   assocDef    The child topic models according to this association definition are fetched.
     */
    void fetch(DMXObjectModel object, AssociationDefinitionModel assocDef, boolean deep) {
        try {
            ChildTopicsModel childTopics = object.getChildTopicsModel();
            String cardinalityUri = assocDef.getChildCardinalityUri();
            String assocDefUri    = assocDef.getAssocDefUri();
            if (cardinalityUri.equals("dm4.core.one")) {
                RelatedTopicModelImpl childTopic = fetchChildTopic(object.getId(), assocDef);
                // Note: topics just created have no child topics yet
                if (childTopic != null) {
                    childTopics.put(assocDefUri, childTopic);
                    if (deep) {
                        fetchChildTopics(childTopic, deep);    // recursion
                    }
                }
            } else if (cardinalityUri.equals("dm4.core.many")) {
                for (RelatedTopicModelImpl childTopic : fetchChildTopics(object.getId(), assocDef)) {
                    childTopics.add(assocDefUri, childTopic);
                    if (deep) {
                        fetchChildTopics(childTopic, deep);    // recursion
                    }
                }
            } else {
                throw new RuntimeException("\"" + cardinalityUri + "\" is an unexpected cardinality URI");
            }
        } catch (Exception e) {
            throw new RuntimeException("Fetching the \"" + assocDef.getAssocDefUri() + "\" child topics of object " +
                object.getId() + " failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Fetches the child topic models (recursively) of the given object model and updates it in-place.
     * ### TODO: recursion is required in some cases (e.g. when fetching a topic through REST API) but is possibly
     * overhead in others (e.g. when updating composite structures).
     */
    private void fetchChildTopics(DMXObjectModelImpl object, boolean deep) {
        for (AssociationDefinitionModel assocDef : object.getType().getAssocDefs()) {
            fetch(object, assocDef, deep);
        }
    }

    // ---

    /**
     * Fetches and returns a child topic or <code>null</code> if no such topic extists.
     */
    private RelatedTopicModelImpl fetchChildTopic(long objectId, AssociationDefinitionModel assocDef) {
        return pl.fetchRelatedTopic(
            objectId,
            assocDef.getInstanceLevelAssocTypeUri(),
            "dm4.core.parent", "dm4.core.child",
            assocDef.getChildTypeUri()
        );
    }

    private List<RelatedTopicModelImpl> fetchChildTopics(long objectId, AssociationDefinitionModel assocDef) {
        return pl.fetchRelatedTopics(
            objectId,
            assocDef.getInstanceLevelAssocTypeUri(),
            "dm4.core.parent", "dm4.core.child",
            assocDef.getChildTypeUri()
        );
    }
}
