package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.TopicModel;

import java.util.List;
import java.util.logging.Logger;



/**
 * A topic that is attached to the {@link CoreService}.
 */
class TopicImpl extends DeepaMehtaObjectImpl implements Topic {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicImpl(TopicModelImpl model, PersistenceLayer pl) {
        super(model, pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************
    // *** Topic Implementation ***
    // ****************************



    @Override
    public final void update(TopicModel updateModel) {
        model.update((TopicModelImpl) updateModel);    // ### FIXME: call through pl for access control
    }

    @Override
    public final void delete() {
        pl.deleteTopic(getModel());
    }

    // ---

    @Override
    public final Topic findChildTopic(String topicTypeUri) {
        TopicModelImpl topic = getModel().findChildTopic(topicTypeUri);
        return topic != null ? topic.instantiate() : null;
    }

    // ---

    @Override
    public final Topic loadChildTopics() {
        model.loadChildTopics();
        return this;
    }

    @Override
    public final Topic loadChildTopics(String assocDefUri) {
        model.loadChildTopics(assocDefUri);
        return this;
    }

    // ---

    // Note: overridden by DeepaMehtaTypeImpl
    @Override
    public TopicModelImpl getModel() {
        return (TopicModelImpl) model;
    }



    // ***************************************
    // *** DeepaMehtaObject Implementation ***
    // ***************************************



    // === Traversal ===

    // ### TODO: move logic to model

    // --- Topic Retrieval ---

    @Override
    public final List<RelatedTopic> getRelatedTopics(List assocTypeUris, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                         String othersTopicTypeUri) {
        List<RelatedTopicModelImpl> topics = pl.fetchTopicRelatedTopics(getId(), assocTypeUris, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
        return pl.checkReadAccessAndInstantiate(topics);
    }

    // --- Association Retrieval ---

    @Override
    public final RelatedAssociation getRelatedAssociation(String assocTypeUri, String myRoleTypeUri,
                                                          String othersRoleTypeUri, String othersAssocTypeUri) {
        RelatedAssociationModelImpl assoc = pl.fetchTopicRelatedAssociation(getId(),
            assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersAssocTypeUri);
        return assoc != null ? pl.<RelatedAssociation>checkReadAccessAndInstantiate(assoc) : null;
    }

    @Override
    public final List<RelatedAssociation> getRelatedAssociations(String assocTypeUri, String myRoleTypeUri,
                                                                 String othersRoleTypeUri, String othersAssocTypeUri) {
        List<RelatedAssociationModelImpl> assocs = pl.fetchTopicRelatedAssociations(getId(), assocTypeUri,
            myRoleTypeUri, othersRoleTypeUri, othersAssocTypeUri);
        return pl.checkReadAccessAndInstantiate(assocs);
    }

    // ---

    @Override
    public final Association getAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                       long othersTopicId) {
        AssociationModelImpl assoc = pl.fetchAssociation(assocTypeUri, getId(), othersTopicId, myRoleTypeUri,
            othersRoleTypeUri);
        return assoc != null ? pl.<Association>checkReadAccessAndInstantiate(assoc) : null;
    }

    @Override
    public final List<Association> getAssociations() {
        return pl.checkReadAccessAndInstantiate(pl.fetchTopicAssociations(getId()));
    }
}
