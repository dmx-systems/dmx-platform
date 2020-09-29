package systems.dmx.core.impl;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.RelatedObjectModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.service.DMXEvent;
import systems.dmx.core.service.Directive;

import java.util.List;



public class TopicModelImpl extends DMXObjectModelImpl implements TopicModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicModelImpl(DMXObjectModelImpl object) {
        super(object);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Implementation of the abstract methods ===

    @Override
    public PlayerModel createPlayerModel(String roleTypeUri) {
        return mf.newTopicPlayerModel(id, roleTypeUri);
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
        return new TopicImpl(this, al);
    }

    @Override
    final TopicModelImpl createModelWithChildTopics(ChildTopicsModel childTopics) {
        return mf.newTopicModel(typeUri, childTopics);
    }

    // ---

    @Override
    final TopicTypeModelImpl getType() {
        return al.typeStorage.getTopicType(typeUri);
    }

    @Override
    final List<AssocModelImpl> getAssocs() {
        return al.db.fetchTopicAssocs(id);
    }

    // ---

    @Override
    final RelatedTopicModelImpl getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                           String othersTopicTypeUri) {
        return al.sd.fetchTopicRelatedTopic(id, assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
    }

    @Override
    final List<RelatedTopicModelImpl> getRelatedTopics(String assocTypeUri, String myRoleTypeUri,
                                                                                           String othersRoleTypeUri,
                                                                                           String othersTopicTypeUri) {
        return al.db.fetchTopicRelatedTopics(id, assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
    }

    @Override
    final <M extends RelatedObjectModel> List<M> getRelatedObjects(String assocTypeUri, String myRoleTypeUri,
                                                                   String othersRoleTypeUri, String othersTypeUri) {
        return al.db.fetchTopicRelatedObjects(id, assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTypeUri);
    }

    // ---

    @Override
    final void storeUri() {
        al.db.storeTopicUri(id, uri);
    }

    @Override
    final void storeTypeUri() {
        reassignInstantiation();
        al.db.storeTopicTypeUri(id, typeUri);
    }

    @Override
    final void storeSimpleValue() {
        al.db.storeTopicValue(id, value, typeUri, isHtml());
    }

    @Override
    final void storeProperty(String propUri, Object propValue, boolean addToIndex) {
        al.db.storeTopicProperty(id, propUri, propValue, addToIndex);
    }

    @Override
    final void removeProperty(String propUri) {
        al.db.deleteTopicProperty(id, propUri);
    }

    // ---

    @Override
    final void _delete() {
        al.db.deleteTopic(id);
    }

    // ---

    @Override
    final <M extends DMXObjectModelImpl> M checkReadAccess() {
        al.checkTopicReadAccess(id);
        return (M) this;
    }

    @Override
    final void checkWriteAccess() {
        al.checkTopicWriteAccess(id);
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
        if (typeUri.equals(TOPIC_TYPE) || typeUri.equals(ASSOC_TYPE)) {
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
            for (CompDefModel compDef : getType().getCompDefs()) {
                String compDefUri     = compDef.getCompDefUri();
                String cardinalityUri = compDef.getChildCardinalityUri();
                TopicModelImpl childTopic = null;
                if (cardinalityUri.equals(ONE)) {
                    childTopic = childTopics.getTopicOrNull(compDefUri);                                 // no DB access
                } else if (cardinalityUri.equals(MANY)) {
                    List<RelatedTopicModelImpl> _childTopics = childTopics.getTopicsOrNull(compDefUri);  // no DB access
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
        al.createTopicInstantiation(id, typeUri);
    }

    // Note: this method works only for instances, not for types.
    // This is because a type is not of type "dmx.core.topic_type" but of type "dmx.core.meta_type".
    private AssocModelImpl fetchInstantiation() {
        RelatedTopicModelImpl topicType = getRelatedTopic(INSTANTIATION, INSTANCE, TYPE, TOPIC_TYPE);
        //
        if (topicType == null) {
            throw new RuntimeException("Topic " + id + " is not associated to a topic type");
        }
        //
        return topicType.getRelatingAssoc();
    }
}
