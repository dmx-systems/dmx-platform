package systems.dmx.core.impl;

import systems.dmx.core.Assoc;
import systems.dmx.core.RelatedAssoc;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.model.DMXObjectModel;
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

    TopicImpl(TopicModelImpl model, AccessLayer al) {
        super(model, al);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *************
    // *** Topic ***
    // *************



    @Override
    public final <M extends DMXObjectModel> void update(M updateModel) {
        al.updateTopic(getModel(), (TopicModelImpl) updateModel);
    }

    @Override
    public final void delete() {
        al.deleteTopic(getModel());
    }

    // ---

    @Override
    public final Topic findChildTopic(String topicTypeUri) {
        TopicModelImpl topic = getModel().findChildTopic(topicTypeUri);
        return topic != null ? topic.instantiate() : null;
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

    // --- Assoc Retrieval ---

    @Override
    public final RelatedAssoc getRelatedAssoc(String assocTypeUri, String myRoleTypeUri,
                                              String othersRoleTypeUri, String othersAssocTypeUri) {
        RelatedAssocModelImpl assoc = al.getTopicRelatedAssoc(getId(), assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri);
        return assoc != null ? assoc.instantiate() : null;
    }

    @Override
    public final List<RelatedAssoc> getRelatedAssocs(String assocTypeUri, String myRoleTypeUri,
                                                     String othersRoleTypeUri, String othersAssocTypeUri) {
        return al.instantiate(al.getTopicRelatedAssocs(getId(), assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri));
    }

    // ---

    @Override
    public final Assoc getAssoc(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                long othersTopicId) {
        AssocModelImpl assoc = al.getAssoc(assocTypeUri, getId(), othersTopicId, myRoleTypeUri, othersRoleTypeUri);
        return assoc != null ? assoc.instantiate() : null;
    }

    @Override
    public final List<Assoc> getAssocs() {
        return al.instantiate(al.getTopicAssocs(getId()));
    }
}
