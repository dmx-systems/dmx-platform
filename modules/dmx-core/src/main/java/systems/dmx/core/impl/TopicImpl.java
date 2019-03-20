package systems.dmx.core.impl;

import systems.dmx.core.Association;
import systems.dmx.core.RelatedAssociation;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.model.TopicModel;

import java.util.List;
import java.util.logging.Logger;



/**
 * A topic that is attached to the {@link CoreService}.
 */
class TopicImpl extends DMXObjectImpl implements Topic {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicImpl(TopicModelImpl model, PersistenceLayer pl) {
        super(model, pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *************
    // *** Topic ***
    // *************



    @Override
    public final void update(TopicModel updateModel) {
        pl.updateTopic(getModel(), (TopicModelImpl) updateModel);
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
        super.loadChildTopics();
        return this;
    }

    @Override
    public final Topic loadChildTopics(String assocDefUri) {
        super.loadChildTopics(assocDefUri);
        return this;
    }

    // ---

    // Note: overridden by DMXTypeImpl
    @Override
    public TopicModelImpl getModel() {
        return (TopicModelImpl) model;
    }



    // *****************
    // *** DMXObject ***
    // *****************



    // === Traversal ===

    // ### TODO: consider adding model convenience, would require model renamings (get -> fetch)

    // --- Association Retrieval ---

    @Override
    public final RelatedAssociation getRelatedAssociation(String assocTypeUri, String myRoleTypeUri,
                                                          String othersRoleTypeUri, String othersAssocTypeUri) {
        RelatedAssociationModelImpl assoc = pl.getTopicRelatedAssociation(getId(), assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersAssocTypeUri);
        return assoc != null ? assoc.instantiate() : null;
        // ### FIXME: add access contol
    }

    @Override
    public final List<RelatedAssociation> getRelatedAssociations(String assocTypeUri, String myRoleTypeUri,
                                                                 String othersRoleTypeUri, String othersAssocTypeUri) {
        return pl.instantiate(pl.getTopicRelatedAssociations(getId(), assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri));
        // ### FIXME: add access contol
    }

    // ---

    @Override
    public final Association getAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                       long othersTopicId) {
        return pl.getAssociation(assocTypeUri, getId(), othersTopicId, myRoleTypeUri, othersRoleTypeUri);
        // ### FIXME: add access contol
    }

    @Override
    public final List<Association> getAssociations() {
        return pl.instantiate(pl.getTopicAssociations(getId()));
        // ### FIXME: add access contol
    }
}
