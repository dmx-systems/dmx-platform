package systems.dmx.core.impl;

import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.RoleModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.service.DMXEvent;
import systems.dmx.core.service.Directive;

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
        pl.storeTopicValue(id, value, typeUri, isHtml());
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
    final DMXEvent getPreUpdateEvent() {
        return CoreEvent.PRE_UPDATE_TOPIC;
    }

    @Override
    final DMXEvent getPostUpdateEvent() {
        return CoreEvent.POST_UPDATE_TOPIC;
    }

    @Override
    final DMXEvent getPreDeleteEvent() {
        return CoreEvent.PRE_DELETE_TOPIC;
    }

    @Override
    final DMXEvent getPostDeleteEvent() {
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
        if (typeUri.equals("dmx.core.topic_type") || typeUri.equals("dmx.core.assoc_type")) {
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
            for (CompDefModel assocDef : getType().getCompDefs()) {
                String assocDefUri    = assocDef.getCompDefUri();
                String cardinalityUri = assocDef.getChildCardinalityUri();
                TopicModelImpl childTopic = null;
                if (cardinalityUri.equals("dmx.core.one")) {
                    childTopic = childTopics.getTopicOrNull(assocDefUri);                                // no DB access
                } else if (cardinalityUri.equals("dmx.core.many")) {
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
    // This is because a type is not of type "dmx.core.topic_type" but of type "dmx.core.meta_type".
    private AssociationModelImpl fetchInstantiation() {
        RelatedTopicModelImpl topicType = getRelatedTopic("dmx.core.instantiation", "dmx.core.instance",
            "dmx.core.type", "dmx.core.topic_type");
        //
        if (topicType == null) {
            throw new RuntimeException("Topic " + id + " is not associated to a topic type");
        }
        //
        return topicType.getRelatingAssociation();
    }
}
