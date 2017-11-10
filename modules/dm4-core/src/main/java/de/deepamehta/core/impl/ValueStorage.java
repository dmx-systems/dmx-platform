package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicReferenceModel;
import de.deepamehta.core.model.TypeModel;

import java.util.List;
import java.util.logging.Logger;



/**
 * Helper for storing/fetching simple values and composite value models.
 *
 * ### TODO: drop this class
 */
class ValueStorage {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private PersistenceLayer pl;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    ValueStorage(PersistenceLayer pl) {
        this.pl = pl;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * Fetches the child topic models (recursively) of the given parent object model and updates it in-place.
     * ### TODO: recursion is required in some cases (e.g. when fetching a topic through REST API) but is possibly
     * overhead in others (e.g. when updating composite structures).
     */
    void fetchChildTopics(DeepaMehtaObjectModel parent) {
        for (AssociationDefinitionModel assocDef : ((DeepaMehtaObjectModelImpl) parent).getType().getAssocDefs()) {
            fetchChildTopics(parent, assocDef);
        }
    }

    /**
     * Fetches the child topic models (recursively) of the given parent object model and updates it in-place.
     * ### TODO: recursion is required in some cases (e.g. when fetching a topic through REST API) but is possibly
     * overhead in others (e.g. when updating composite structures).
     * <p>
     * Works for both, "one" and "many" association definitions.
     *
     * @param   assocDef    The child topic models according to this association definition are fetched.
     */
    void fetchChildTopics(DeepaMehtaObjectModel parent, AssociationDefinitionModel assocDef) {
        try {
            ChildTopicsModel childTopics = parent.getChildTopicsModel();
            String cardinalityUri = assocDef.getChildCardinalityUri();
            String assocDefUri    = assocDef.getAssocDefUri();
            if (cardinalityUri.equals("dm4.core.one")) {
                RelatedTopicModel childTopic = fetchChildTopic(parent.getId(), assocDef);
                // Note: topics just created have no child topics yet
                if (childTopic != null) {
                    childTopics.put(assocDefUri, childTopic);
                    fetchChildTopics(childTopic);    // recursion
                }
            } else if (cardinalityUri.equals("dm4.core.many")) {
                for (RelatedTopicModel childTopic : fetchChildTopics(parent.getId(), assocDef)) {
                    childTopics.add(assocDefUri, childTopic);
                    fetchChildTopics(childTopic);    // recursion
                }
            } else {
                throw new RuntimeException("\"" + cardinalityUri + "\" is an unexpected cardinality URI");
            }
        } catch (Exception e) {
            throw new RuntimeException("Fetching the \"" + assocDef.getAssocDefUri() + "\" child topics of object " +
                parent.getId() + " failed", e);
        }
    }

    // ---

    /**
     * Stores and indexes the specified model's value, either a simple value or a composite value (child topics).
     * Depending on the model type's data type dispatches either to storeSimpleValue() or to storeChildTopics().
     * <p>
     * Called to store the initial value of a newly created topic/association.
     *
     * ### TODO: drop it
     */
    void storeValue(DeepaMehtaObjectModelImpl model) {
        if (model.isSimple()) {
            model.storeSimpleValue();
        } else {
            storeChildTopics(model);
            model.calculateLabelAndUpdate();
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Stores the composite value (child topics) of the specified topic or association model.
     * Called to store the initial value of a newly created topic/association.
     * <p>
     * Note: the given model can contain childs not defined in the type definition.
     * Only the childs defined in the type definition are stored.
     */
    private void storeChildTopics(DeepaMehtaObjectModelImpl parent) {
        ChildTopicsModelImpl model = null;
        try {
            model = parent.getChildTopicsModel();
            for (AssociationDefinitionModel assocDef : parent.getType().getAssocDefs()) {
                String assocDefUri    = assocDef.getAssocDefUri();
                String cardinalityUri = assocDef.getChildCardinalityUri();
                if (cardinalityUri.equals("dm4.core.one")) {
                    RelatedTopicModelImpl childTopic = model.getTopicOrNull(assocDefUri);
                    if (childTopic != null) {   // skip if not contained in create request
                        storeChildTopic(childTopic, parent, assocDef);
                    }
                } else if (cardinalityUri.equals("dm4.core.many")) {
                    List<RelatedTopicModelImpl> childTopics = model.getTopicsOrNull(assocDefUri);
                    if (childTopics != null) {  // skip if not contained in create request
                        for (RelatedTopicModelImpl childTopic : childTopics) {
                            storeChildTopic(childTopic, parent, assocDef);
                        }
                    }
                } else {
                    throw new RuntimeException("\"" + cardinalityUri + "\" is an unexpected cardinality URI");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Storing the child topics of object " + parent.getId() + " failed (" +
                model + ")", e);
        }
    }

    private void storeChildTopic(RelatedTopicModelImpl childTopic, DeepaMehtaObjectModel parent,
                                                                   AssociationDefinitionModel assocDef) {
        if (childTopic instanceof TopicReferenceModel) {
            resolveReference((TopicReferenceModel) childTopic);
        } else {
            pl.createTopic(childTopic);
        }
        associateChildTopic(parent, childTopic, assocDef);
    }

    // ---

    /**
     * Replaces a reference with the real thing.
     */
    void resolveReference(TopicReferenceModel topicRef) {
        topicRef.set(fetchReferencedTopic(topicRef));
    }

    private DeepaMehtaObjectModel fetchReferencedTopic(TopicReferenceModel topicRef) {
        // Note: the resolved topic must be fetched including its child topics.
        // They might be required for label calculation and/or at client-side.
        if (topicRef.isReferenceById()) {
            return pl.fetchTopic(topicRef.getId()).loadChildTopics();
        } else if (topicRef.isReferenceByUri()) {
            TopicModelImpl topic = pl.fetchTopic("uri", new SimpleValue(topicRef.getUri()));
            if (topic == null) {
                throw new RuntimeException("Topic with URI \"" + topicRef.getUri() + "\" not found");
            }
            return topic.loadChildTopics();
        } else {
            throw new RuntimeException("Invalid " + topicRef);
        }
    }

    // ---

    /**
     * Creates an association between the given parent object ("Parent" role) and the child topic ("Child" role).
     * The association type is taken from the given association definition.
     */
    void associateChildTopic(DeepaMehtaObjectModel parent, RelatedTopicModel childTopic,
                                                           AssociationDefinitionModel assocDef) {
        AssociationModel assoc = childTopic.getRelatingAssociation();
        assoc.setTypeUri(assocDef.getInstanceLevelAssocTypeUri());
        assoc.setRoleModel1(parent.createRoleModel("dm4.core.parent"));
        assoc.setRoleModel2(childTopic.createRoleModel("dm4.core.child"));
        pl.createAssociation((AssociationModelImpl) assoc);
    }



    // === Helper ===

    /**
     * Fetches and returns a child topic or <code>null</code> if no such topic extists.
     */
    private RelatedTopicModel fetchChildTopic(long parentId, AssociationDefinitionModel assocDef) {
        return pl.fetchRelatedTopic(
            parentId,
            assocDef.getInstanceLevelAssocTypeUri(),
            "dm4.core.parent", "dm4.core.child",
            assocDef.getChildTypeUri()
        );
    }

    private List<RelatedTopicModelImpl> fetchChildTopics(long parentId, AssociationDefinitionModel assocDef) {
        return pl.fetchRelatedTopics(
            parentId,
            assocDef.getInstanceLevelAssocTypeUri(),
            "dm4.core.parent", "dm4.core.child",
            assocDef.getChildTypeUri()
        );
    }
}
