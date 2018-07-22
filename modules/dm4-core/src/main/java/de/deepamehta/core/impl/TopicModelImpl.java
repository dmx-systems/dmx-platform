package de.deepamehta.core.impl;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.service.DeepaMehtaEvent;
import de.deepamehta.core.service.Directive;

import java.util.List;



class TopicModelImpl extends DMXObjectModelImpl implements TopicModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicModelImpl(DMXObjectModelImpl object) {
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

    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    String className() {
        return "topic";
    }

    @Override
    TopicImpl instantiate() {
        return new TopicImpl(this, pl);
    }

    @Override
    final TopicModelImpl createModelWithChildTopics(ChildTopicsModel childTopics) {
        return mf.newTopicModel(typeUri, childTopics);
    }

    // ---

    @Override
    final TopicTypeModelImpl getType() {
        return pl.typeStorage.getTopicType(typeUri);
    }

    @Override
    final List<AssociationModelImpl> getAssociations() {
        return pl.fetchTopicAssociations(id);
    }

    // ---

    @Override
    final RelatedTopicModelImpl getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                           String othersTopicTypeUri) {
        return pl.fetchTopicRelatedTopic(id, assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
    }

    @Override
    final List<RelatedTopicModelImpl> getRelatedTopics(String assocTypeUri, String myRoleTypeUri,
                                                                                           String othersRoleTypeUri,
                                                                                           String othersTopicTypeUri) {
        return pl.fetchTopicRelatedTopics(id, assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
    }

    @Override
    final List<RelatedTopicModelImpl> getRelatedTopics(List assocTypeUris, String myRoleTypeUri,
                                                                                           String othersRoleTypeUri,
                                                                                           String othersTopicTypeUri) {
        return pl.fetchTopicRelatedTopics(id, assocTypeUris, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
    }

    // ---

    @Override
    final void storeUri() {
        pl.storeTopicUri(id, uri);
    }

    @Override
    final void storeTypeUri() {
        reassignInstantiation();
        pl.storeTopicTypeUri(id, typeUri);
    }

    @Override
    final void storeSimpleValue() {
        TypeModel type = getType();
        pl.storeTopicValue(id, value, type.getIndexModes(), type.getUri(), getIndexValue());
    }

    @Override
    final void indexSimpleValue(IndexMode indexMode) {
        pl.indexTopicValue(id, indexMode, typeUri, getIndexValue());
    }

    @Override
    final void storeProperty(String propUri, Object propValue, boolean addToIndex) {
        pl.storeTopicProperty(id, propUri, propValue, addToIndex);
    }

    @Override
    final void removeProperty(String propUri) {
        pl.removeTopicProperty(id, propUri);
    }

    // ---

    @Override
    final void _delete() {
        pl._deleteTopic(id);
    }

    // ---

    @Override
    final void checkReadAccess() {
        pl.checkTopicReadAccess(id);
    }

    @Override
    final void checkWriteAccess() {
        pl.checkTopicWriteAccess(id);
    }

    // ---

    @Override
    final DeepaMehtaEvent getPreUpdateEvent() {
        return CoreEvent.PRE_UPDATE_TOPIC;
    }

    @Override
    final DeepaMehtaEvent getPostUpdateEvent() {
        return CoreEvent.POST_UPDATE_TOPIC;
    }

    @Override
    final DeepaMehtaEvent getPreDeleteEvent() {
        return CoreEvent.PRE_DELETE_TOPIC;
    }

    @Override
    final DeepaMehtaEvent getPostDeleteEvent() {
        return CoreEvent.POST_DELETE_TOPIC;
    }

    // ---

    @Override
    final Directive getUpdateDirective() {
        return Directive.UPDATE_TOPIC;
    }

    @Override
    final Directive getDeleteDirective() {
        return Directive.DELETE_TOPIC;
    }



    // === Core Internal Hooks ===

    @Override
    void preDelete() {
        if (typeUri.equals("dm4.core.topic_type") || typeUri.equals("dm4.core.assoc_type")) {
            throw new RuntimeException("Tried to delete a type with a generic delete-topic call. " +
                "Use a delete-type call instead.");
        }
    }



    // ===

    TopicModelImpl findChildTopic(String topicTypeUri) {
        try {
            if (typeUri.equals(topicTypeUri)) {
                return this;
            }
            //
            for (AssociationDefinitionModel assocDef : getType().getAssocDefs()) {
                String assocDefUri    = assocDef.getAssocDefUri();
                String cardinalityUri = assocDef.getChildCardinalityUri();
                TopicModelImpl childTopic = null;
                if (cardinalityUri.equals("dm4.core.one")) {
                    childTopic = childTopics.getTopicOrNull(assocDefUri);                                // no DB access
                } else if (cardinalityUri.equals("dm4.core.many")) {
                    List<RelatedTopicModelImpl> _childTopics = childTopics.getTopicsOrNull(assocDefUri); // no DB access
                    if (_childTopics != null && !_childTopics.isEmpty()) {
                        childTopic = _childTopics.get(0);
                    }
                } else {
                    throw new RuntimeException("\"" + cardinalityUri + "\" is an unexpected cardinality URI");
                }
                // Note: topics just created have no child topics yet
                if (childTopic == null) {
                    continue;
                }
                // recursion
                childTopic = childTopic.findChildTopic(topicTypeUri);
                if (childTopic != null) {
                    return childTopic;
                }
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Searching topic " + id + " for \"" + topicTypeUri + "\" failed", e);
        }
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
