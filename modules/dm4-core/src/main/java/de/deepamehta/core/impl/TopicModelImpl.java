package de.deepamehta.core.impl;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.service.DeepaMehtaEvent;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.ResultList;

import org.codehaus.jettison.json.JSONObject;

import java.util.List;



class TopicModelImpl extends DeepaMehtaObjectModelImpl implements TopicModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicModelImpl(DeepaMehtaObjectModelImpl object) {
        super(object);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Implementation of the abstract methods ===

    @Override
    public RoleModel createRoleModel(String roleTypeUri) {
        return mf.newTopicRoleModel(id, roleTypeUri);
    }



    // === Java API ===

    @Override
    public TopicModel clone() {
        try {
            return (TopicModel) super.clone();
        } catch (Exception e) {
            throw new RuntimeException("Cloning a TopicModel failed", e);
        }
    }

    @Override
    public String toString() {
        return "topic (" + super.toString() + ")";
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    @Override
    String className() {
        return "topic";
    }

    @Override
    Topic instantiate() {
        return new TopicImpl(this, pl);
    }

    // ---

    @Override
    TopicTypeModel getType() {
        return pl.typeStorage.getTopicType(typeUri);
    }

    @Override
    List<AssociationModel> getAssociations() {
        return pl.fetchTopicAssociations(id);
    }

    // ---

    @Override
    RelatedTopicModelImpl getRelatedTopic(String assocTypeUri, String myRoleTypeUri,
                                          String othersRoleTypeUri, String othersTopicTypeUri) {
        return pl.fetchTopicRelatedTopic(id, assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
    }

    @Override
    ResultList<RelatedTopicModel> getRelatedTopics(String assocTypeUri, String myRoleTypeUri,
                                                   String othersRoleTypeUri, String othersTopicTypeUri) {
        return pl.fetchTopicRelatedTopics(id, assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
    }

    @Override
    ResultList<RelatedTopicModel> getRelatedTopics(List assocTypeUris, String myRoleTypeUri,
                                                   String othersRoleTypeUri, String othersTopicTypeUri) {
        return pl.fetchTopicRelatedTopics(id, assocTypeUris, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
    }

    // ---

    @Override
    void storeUri() {
        pl.storeTopicUri(id, uri);
    }

    @Override
    void storeTypeUri() {
        reassignInstantiation();
        pl.storeTopicTypeUri(id, typeUri);
    }

    @Override
    void storeSimpleValue() {
        TypeModel type = getType();
        pl.storeTopicValue(id, value, type.getIndexModes(), type.getUri(), getIndexValue());
    }

    @Override
    void indexSimpleValue(IndexMode indexMode) {
        pl.indexTopicValue(id, indexMode, typeUri, getIndexValue());
    }

    // ---

    @Override
    void updateChildTopics(ChildTopicsModel childTopics) {
        update(mf.newTopicModel(childTopics));
    }

    @Override
    void _delete() {
        pl._deleteTopic(id);
    }

    // ---

    @Override
    DeepaMehtaEvent getPreGetEvent() {
        return CoreEvent.PRE_GET_TOPIC;
    }

    @Override
    DeepaMehtaEvent getPreUpdateEvent() {
        return CoreEvent.PRE_UPDATE_TOPIC;
    }

    @Override
    DeepaMehtaEvent getPostUpdateEvent() {
        return CoreEvent.POST_UPDATE_TOPIC;
    }

    @Override
    DeepaMehtaEvent getPreDeleteEvent() {
        return CoreEvent.PRE_DELETE_TOPIC;
    }

    @Override
    DeepaMehtaEvent getPostDeleteEvent() {
        return CoreEvent.POST_DELETE_TOPIC;
    }

    // ---

    @Override
    Directive getUpdateDirective() {
        return Directive.UPDATE_TOPIC;
    }

    @Override
    Directive getDeleteDirective() {
        return Directive.DELETE_TOPIC;
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private void reassignInstantiation() {
        // remove current assignment
        fetchInstantiation().delete();
        // create new assignment
        pl.createTopicInstantiation(id, typeUri);
    }

    // Note: this method works only for instances, not for types.
    // This is because a type is not of type "dm4.core.topic_type" but of type "dm4.core.meta_type".
    private AssociationModelImpl fetchInstantiation() {
        RelatedTopicModelImpl topicType = getRelatedTopic("dm4.core.instantiation", "dm4.core.instance",
            "dm4.core.type", "dm4.core.topic_type");
        //
        if (topicType == null) {
            throw new RuntimeException("Topic " + id + " is not associated to a topic type");
        }
        //
        return topicType.getRelatingAssociation();
    }
}
