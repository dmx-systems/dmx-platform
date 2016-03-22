package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.ResultList;

import java.util.List;
import java.util.logging.Logger;



/**
 * A topic that is attached to the {@link CoreService}.
 */
class TopicImpl extends DeepaMehtaObjectImpl implements Topic {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicImpl(TopicModelImpl model, PersistenceLayer pl) {
        super(model, pl);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************
    // *** Topic Implementation ***
    // ****************************



    @Override
    public void update(TopicModel newModel) {
        model.update(newModel);
    }

    // ---

    @Override
    public Topic loadChildTopics() {
        model.loadChildTopics();
        return this;
    }

    @Override
    public Topic loadChildTopics(String assocDefUri) {
        model.loadChildTopics(assocDefUri);
        return this;
    }

    // ---

    @Override
    public TopicType getType() {
        return pl.getTopicType(getTypeUri());
    }

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
    public ResultList<RelatedTopic> getRelatedTopics(List assocTypeUris, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                            String othersTopicTypeUri) {
        ResultList<RelatedTopicModelImpl> topics = pl.fetchTopicRelatedTopics(getId(), assocTypeUris,
            myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
        return new ResultList(pl.checkReadAccessAndInstantiate(topics));
    }

    // --- Association Retrieval ---

    @Override
    public RelatedAssociation getRelatedAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                            String othersAssocTypeUri) {
        RelatedAssociationModelImpl assoc = pl.fetchTopicRelatedAssociation(getId(),
            assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersAssocTypeUri);
        return assoc != null ? pl.<RelatedAssociation>checkReadAccessAndInstantiate(assoc) : null;
    }

    @Override
    public ResultList<RelatedAssociation> getRelatedAssociations(String assocTypeUri, String myRoleTypeUri,
                                                                 String othersRoleTypeUri, String othersAssocTypeUri) {
        ResultList<RelatedAssociationModelImpl> assocs = pl.fetchTopicRelatedAssociations(getId(),
            assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersAssocTypeUri);
        return new ResultList(pl.checkReadAccessAndInstantiate(assocs));
    }

    // ---

    @Override
    public Association getAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                 long othersTopicId) {
        AssociationModelImpl assoc = pl.fetchAssociation(assocTypeUri, getId(), othersTopicId, myRoleTypeUri,
            othersRoleTypeUri);
        return assoc != null ? pl.<Association>checkReadAccessAndInstantiate(assoc) : null;
    }

    @Override
    public List<Association> getAssociations() {
        return pl.checkReadAccessAndInstantiate(pl.fetchTopicAssociations(getId()));
    }



    // === Properties ===

    @Override
    public void setProperty(String propUri, Object propValue, boolean addToIndex) {
        pl.storeTopicProperty(getId(), propUri, propValue, addToIndex);
    }

    @Override
    public void removeProperty(String propUri) {
        pl.removeTopicProperty(getId(), propUri);
    }
}
