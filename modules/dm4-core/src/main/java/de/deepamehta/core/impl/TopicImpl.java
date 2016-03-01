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
 * A topic that is attached to the {@link DeepaMehtaService}.
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
        super.update(newModel);
    }

    // ---

    @Override
    public Topic loadChildTopics() {
        return (Topic) super.loadChildTopics();
    }

    @Override
    public Topic loadChildTopics(String assocDefUri) {
        return (Topic) super.loadChildTopics(assocDefUri);
    }

    // ---

    @Override
    public TopicModelImpl getModel() {
        return (TopicModelImpl) super.getModel();
    }



    // ***************************************
    // *** DeepaMehtaObject Implementation ***
    // ***************************************



    // === Traversal ===

    // --- Topic Retrieval ---

    @Override
    public ResultList<RelatedTopic> getRelatedTopics(List assocTypeUris, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                            String othersTopicTypeUri) {
        ResultList<RelatedTopicModel> topics = pl.fetchTopicRelatedTopics(getId(), assocTypeUris,
            myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
        return pl.instantiateRelatedTopics(topics);
    }

    // --- Association Retrieval ---

    @Override
    public RelatedAssociation getRelatedAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                            String othersAssocTypeUri) {
        RelatedAssociationModel assoc = pl.fetchTopicRelatedAssociation(getId(),
            assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersAssocTypeUri);
        return assoc != null ? pl.instantiateRelatedAssociation(assoc) : null;
    }

    @Override
    public ResultList<RelatedAssociation> getRelatedAssociations(String assocTypeUri, String myRoleTypeUri,
                                                                 String othersRoleTypeUri, String othersAssocTypeUri) {
        ResultList<RelatedAssociationModel> assocs = pl.fetchTopicRelatedAssociations(getId(),
            assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersAssocTypeUri);
        return pl.instantiateRelatedAssociations(assocs);
    }

    // ---

    @Override
    public Association getAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                 long othersTopicId) {
        AssociationModelImpl assoc = pl.fetchAssociation(assocTypeUri, getId(), othersTopicId, myRoleTypeUri,
            othersRoleTypeUri);
        return assoc != null ? pl.instantiateAssociation(assoc) : null;
    }

    @Override
    public List<Association> getAssociations() {
        return pl.instantiateAssociations(pl.fetchTopicAssociations(getId()));
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

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Implementation of the abstract methods ===

    @Override
    String className() {
        return "topic";
    }

    @Override
    void updateChildTopics(ChildTopicsModel childTopics) {
        update(mf.newTopicModel(childTopics));
    }

    // ---

    @Override
    final RelatedTopicModel fetchRelatedTopic(String assocTypeUri, String myRoleTypeUri,
                                              String othersRoleTypeUri, String othersTopicTypeUri) {
        return pl.fetchTopicRelatedTopic(getId(), assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri);
    }

    @Override
    final ResultList<RelatedTopicModel> fetchRelatedTopics(String assocTypeUri, String myRoleTypeUri,
                                                           String othersRoleTypeUri, String othersTopicTypeUri) {
        return pl.fetchTopicRelatedTopics(getId(), assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri);
    }

    // ---

    @Override
    TopicType getType() {
        return pl.getTopicType(getTypeUri());
    }
}
