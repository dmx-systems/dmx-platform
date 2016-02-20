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

    TopicImpl(TopicModel model, EmbeddedService dms) {
        super(model, dms);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************
    // *** DeepaMehtaObjectImpl Overrides ***
    // **************************************



    // === Updating ===

    @Override
    public void update(TopicModel model) {
        _update(model);
        //
        dms.fireEvent(CoreEvent.POST_UPDATE_TOPIC_REQUEST, this);
    }



    // ****************************
    // *** Topic Implementation ***
    // ****************************



    @Override
    public Topic loadChildTopics() {
        return (Topic) super.loadChildTopics();
    }

    @Override
    public Topic loadChildTopics(String childTypeUri) {
        return (Topic) super.loadChildTopics(childTypeUri);
    }

    // ---

    @Override
    public TopicModel getModel() {
        return (TopicModel) super.getModel();
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
        return dms.instantiateRelatedTopics(topics);
    }

    // --- Association Retrieval ---

    @Override
    public RelatedAssociation getRelatedAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                            String othersAssocTypeUri) {
        RelatedAssociationModel assoc = pl.fetchTopicRelatedAssociation(getId(),
            assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersAssocTypeUri);
        return assoc != null ? dms.instantiateRelatedAssociation(assoc) : null;
    }

    @Override
    public ResultList<RelatedAssociation> getRelatedAssociations(String assocTypeUri, String myRoleTypeUri,
                                                                 String othersRoleTypeUri, String othersAssocTypeUri) {
        ResultList<RelatedAssociationModel> assocs = pl.fetchTopicRelatedAssociations(getId(),
            assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersAssocTypeUri);
        return dms.instantiateRelatedAssociations(assocs);
    }

    // ---

    @Override
    public Association getAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                   long othersTopicId) {
        AssociationModel assoc = pl.fetchAssociation(assocTypeUri, getId(), othersTopicId,
            myRoleTypeUri, othersRoleTypeUri);
        return assoc != null ? dms.instantiateAssociation(assoc) : null;
    }

    @Override
    public List<Association> getAssociations() {
        return dms.instantiateAssociations(pl.fetchTopicAssociations(getId()));
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

    /**
     * Low-level update method which does not fire the POST_UPDATE_TOPIC_REQUEST event.
     * <p>
     * Called multiple times while updating the child topics (see ChildTopicsImpl).
     * POST_UPDATE_TOPIC_REQUEST on the other hand must be fired only once (per update request).
     */
    void _update(TopicModel model) {
        logger.info("Updating topic " + getId() + " (typeUri=\"" + getTypeUri() + "\")");
        //
        dms.fireEvent(CoreEvent.PRE_UPDATE_TOPIC, this, model);
        //
        TopicModel oldModel = getModel().clone();
        super.update(model);
        //
        dms.fireEvent(CoreEvent.POST_UPDATE_TOPIC, this, model, oldModel);
    }



    // === Implementation of the abstract methods ===

    @Override
    String className() {
        return "topic";
    }

    @Override
    void updateChildTopics(ChildTopicsModel childTopics) {
        update(mf.newTopicModel(childTopics));
    }

    @Override
    Directive getUpdateDirective() {
        return Directive.UPDATE_TOPIC;
    }

    @Override
    final void storeUri() {
        pl.storeTopicUri(getId(), getUri());
    }

    @Override
    final void storeTypeUri() {
        reassignInstantiation();
        pl.storeTopicTypeUri(getId(), getTypeUri());
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
        return dms.getTopicType(getTypeUri());
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private void reassignInstantiation() {
        // remove current assignment
        fetchInstantiation().delete();
        // create new assignment
        dms.createTopicInstantiation(getId(), getTypeUri());
    }

    // Note: this method works only for instances, not for types.
    // This is because a type is not of type "dm4.core.topic_type" but of type "dm4.core.meta_type".
    private Association fetchInstantiation() {
        RelatedTopic topicType = getRelatedTopic("dm4.core.instantiation", "dm4.core.instance", "dm4.core.type",
            "dm4.core.topic_type");
        //
        if (topicType == null) {
            throw new RuntimeException("Topic " + getId() + " is not associated to a topic type");
        }
        //
        return topicType.getRelatingAssociation();
    }
}
