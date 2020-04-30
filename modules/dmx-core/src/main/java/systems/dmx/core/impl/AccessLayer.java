package systems.dmx.core.impl;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.DMXObject;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.storage.spi.DMXStorage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



/**
 * Storage vendor agnostic access control on top of vendor specific storage.
 *
 * 2 kinds of methods:
 *   - access controlled: get/create/update
 *   - direct DB access: fetch/store (as derived from storage impl)
 *
 * ### TODO: hold storage object in instance variable (instead deriving) to make direct DB access more explicit
 */
public final class AccessLayer {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String URI_PREFIX_TOPIC_TYPE       = "domain.project.topic_type_";
    private static final String URI_PREFIX_ASSOCIATION_TYPE = "domain.project.assoc_type_";
    private static final String URI_PREFIX_ROLE_TYPE        = "domain.project.role_type_";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    DMXStorage db;
    StorageDecorator sd;
    TypeStorage typeStorage;
    EventManager em;
    ModelFactoryImpl mf;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AccessLayer(DMXStorage db) {
        this.db = db;
        this.sd = new StorageDecorator(db);
        // Note: mf must be initialzed before the type storage is instantiated
        this.em = new EventManager();
        this.mf = (ModelFactoryImpl) db.getModelFactory();
        this.typeStorage = new TypeStorage(this);
        //
        // Note: this is a constructor side effect. This is a cyclic dependency.
        // ### TODO: explain why we do it.
        mf.al = this;
        //
        bootstrapTypeCache();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Topics ===

    TopicModelImpl getTopic(long topicId) {
        try {
            return db.fetchTopic(topicId).checkReadAccess();
        } catch (Exception e) {
            throw new RuntimeException("Fetching topic " + topicId + " failed", e);
        }
    }

    TopicModelImpl getTopicByUri(String uri) {
        try {
            TopicModelImpl topic = db.fetchTopic("uri", uri);
            return topic != null ? topic.checkReadAccess() : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching topic failed, uri=\"" + uri + "\"", e);
        }
    }

    List<TopicModelImpl> getTopicsByType(String topicTypeUri) {
        try {
            return filterReadables(_getTopicType(topicTypeUri).getAllInstances());
        } catch (Exception e) {
            throw new RuntimeException("Fetching topics by type failed, topicTypeUri=\"" + topicTypeUri + "\"", e);
        }
    }

    TopicModelImpl getTopicByValue(String key, SimpleValue value) {
        try {
            TopicModelImpl topic = db.fetchTopic(key, value.value());
            return topic != null ? topic.checkReadAccess() : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching topic failed, key=\"" + key + "\", value=" + value, e);
        }
    }

    List<TopicModelImpl> getTopicsByValue(String key, SimpleValue value) {
        try {
            return filterReadables(db.fetchTopics(key, value.value()));
        } catch (Exception e) {
            throw new RuntimeException("Fetching topics failed, key=\"" + key + "\", value=" + value, e);
        }
    }

    List<TopicModelImpl> queryTopics(String key, SimpleValue value) {
        try {
            return filterReadables(db.queryTopics(key, value.value()));
        } catch (Exception e) {
            throw new RuntimeException("Querying topics failed, key=\"" + key + "\", value=" + value, e);
        }
    }

    List<TopicModelImpl> queryTopicsFulltext(String query, String topicTypeUri, boolean searchChildTopics) {
        try {
            logger.info("Querying topics fulltext, query=\"" + query + "\", topicTypeUri=\"" +
                topicTypeUri + "\", searchChildTopics=" + searchChildTopics);
            List<TopicModelImpl> topics;
            if (topicTypeUri != null && searchChildTopics) {
                topics = parentTopics(topicTypeUri, db.queryTopicsFulltext(null, query));
            } else {
                topics = db.queryTopicsFulltext(topicTypeUri, query);
            }
            return filterReadables(topics);
        } catch (Exception e) {
            throw new RuntimeException("Querying topics fulltext failed, query=\"" + query + "\", topicTypeUri=\"" +
                topicTypeUri + "\", searchChildTopics=" + searchChildTopics, e);
        }
    }

    Iterable<TopicModelImpl> getAllTopics() {
        return new ReadableIterable(db.fetchAllTopics());
    }

    // ---

    TopicModelImpl createTopic(TopicModelImpl model) {
        try {
            em.fireEvent(CoreEvent.PRE_CREATE_TOPIC, model);
            model.preCreate();
            return updateValues(model, null);
        } catch (Exception e) {
            throw new RuntimeException("Creating topic failed, model=" + model, e);
        }
    }

    // ---

    TopicModelImpl createSingleTopic(TopicModelImpl model) {
        return createSingleTopic(model, null, null);
    }

    TopicModelImpl createSingleTopic(TopicModelImpl model, String uriPrefix) {
        return createSingleTopic(model, uriPrefix, null);
    }

    TopicModelImpl createSingleTopic(TopicModelImpl model, Runnable runnable) {
        return createSingleTopic(model, null, runnable);
    }

    /**
     * Creates a single topic in the DB.
     * No child topics are created.
     */
    private TopicModelImpl createSingleTopic(TopicModelImpl model, String uriPrefix, Runnable runnable) {
        try {
            // 1) store in DB
            db.storeTopic(model);
            if (model.getType().isSimple()) {
                model.storeSimpleValue();
            }
            createTopicInstantiation(model.getId(), model.getTypeUri());
            // 2) set default URI
            // If no URI is given the topic gets a default URI based on its ID, if requested.
            // Note: this must be done *after* the topic is stored. The ID is not known before.
            // Note: in case no URI was given: once stored a topic's URI is empty (not null).
            if (uriPrefix != null && model.getUri().equals("")) {
                model.updateUri(uriPrefix + model.getId());     // update memory + DB
            }
            //
            if (runnable != null) {
                runnable.run();
            }
            //
            model.postCreate();
            em.fireEvent(CoreEvent.POST_CREATE_TOPIC, model.instantiate());
            return model;
        } catch (Exception e) {
            throw new RuntimeException("Creating single topic failed, model=" + model + ", uriPrefix=\"" + uriPrefix +
                "\"", e);
        }
    }

    // ---

    void updateTopic(TopicModelImpl updateModel) {
        try {
            updateTopic(
                db.fetchTopic(updateModel.getId()),
                updateModel
            );
        } catch (Exception e) {
            throw new RuntimeException("Fetching and updating topic " + updateModel.getId() + " failed", e);
        }
    }

    void updateTopic(TopicModelImpl topic, TopicModelImpl updateModel) {
        try {
            topic.checkWriteAccess();
            topic.update(updateModel);
        } catch (Exception e) {
            throw new RuntimeException("Updating topic " + topic.getId() + " failed", e);
        }
    }

    // ---

    /**
     * Convenience.
     */
    void deleteTopic(long topicId) {
        try {
            deleteTopic(db.fetchTopic(topicId));
        } catch (Exception e) {
            throw new RuntimeException("Fetching and deleting topic " + topicId + " failed", e);
        }
    }

    void deleteTopic(TopicModelImpl topic) {
        try {
            topic.checkWriteAccess();
            topic.delete();
        } catch (Exception e) {
            throw new RuntimeException("Deleting topic " + topic.getId() + " failed", e);
        }
    }



    // === Associations ===

    AssocModelImpl getAssoc(long assocId) {
        try {
            return db.fetchAssoc(assocId).checkReadAccess();
        } catch (Exception e) {
            throw new RuntimeException("Fetching assoc " + assocId + " failed", e);
        }
    }

    AssocModelImpl getAssocByValue(String key, SimpleValue value) {
        try {
            AssocModelImpl assoc = db.fetchAssoc(key, value.value());
            return assoc != null ? assoc.checkReadAccess() : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching assoc failed, key=\"" + key + "\", value=" + value, e);
        }
    }

    List<AssocModelImpl> queryAssocs(String key, SimpleValue value) {
        try {
            return filterReadables(db.queryAssocs(key, value.value()));
        } catch (Exception e) {
            throw new RuntimeException("Querying assocs failed, key=\"" + key + "\", value=" + value, e);
        }
    }

    AssocModelImpl getAssocBetweenTopicAndTopic(String assocTypeUri, long topic1Id, long topic2Id, String roleTypeUri1,
                                                String roleTypeUri2) {
        String info = "assocTypeUri=\"" + assocTypeUri + "\", topic1Id=" + topic1Id + ", topic2Id=" + topic2Id +
            ", roleTypeUri1=\"" + roleTypeUri1 + "\", roleTypeUri2=\"" + roleTypeUri2 + "\"";
        try {
            AssocModelImpl assoc = sd.fetchAssoc(assocTypeUri, topic1Id, topic2Id, roleTypeUri1, roleTypeUri2);
            return assoc != null ? assoc.checkReadAccess() : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching assoc failed, " + info, e);
        }
    }

    AssocModelImpl getAssocBetweenTopicAndAssoc(String assocTypeUri, long topicId, long assocId,
                                                String topicRoleTypeUri, String assocRoleTypeUri) {
        String info = "assocTypeUri=\"" + assocTypeUri + "\", topicId=" + topicId + ", assocId=" + assocId +
            ", topicRoleTypeUri=\"" + topicRoleTypeUri + "\", assocRoleTypeUri=\"" + assocRoleTypeUri + "\"";
        try {
            AssocModelImpl assoc = sd.fetchAssocBetweenTopicAndAssoc(assocTypeUri, topicId, assocId, topicRoleTypeUri,
                assocRoleTypeUri);
            return assoc != null ? assoc.checkReadAccess() : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching assoc failed, " + info, e);
        }
    }

    // ---

    List<AssocModelImpl> getAssocsByType(String assocTypeUri) {
        try {
            return filterReadables(_getAssocType(assocTypeUri).getAllInstances());
        } catch (Exception e) {
            throw new RuntimeException("Fetching assocs by type failed, assocTypeUri=\"" + assocTypeUri + "\"", e);
        }
    }

    List<AssocModelImpl> getAssocs(long topic1Id, long topic2Id) {
        return getAssocs(null, topic1Id, topic2Id);   // assocTypeUri=null
    }

    List<AssocModelImpl> getAssocs(String assocTypeUri, long topic1Id, long topic2Id) {
        return getAssocs(assocTypeUri, topic1Id, topic2Id, null, null);   // roleTypeUri1=null, roleTypeUri2=null
    }

    // Note: not part of core service; called e.g. by AssocModelImpl.duplicateCheck()
    List<AssocModelImpl> getAssocs(String assocTypeUri, long topic1Id, long topic2Id, String roleTypeUri1,
                                                                                      String roleTypeUri2) {
        logger.fine("assocTypeUri=\"" + assocTypeUri + "\", topic1Id=" + topic1Id + ", topic2Id=" + topic2Id +
            ", roleTypeUri1=\"" + roleTypeUri1 + "\", roleTypeUri2=\"" + roleTypeUri2 + "\"");
        try {
            return filterReadables(db.fetchAssocs(assocTypeUri, topic1Id, topic2Id, roleTypeUri1, roleTypeUri2));
        } catch (Exception e) {
            throw new RuntimeException("Fetching assocs between topics " + topic1Id + " and " + topic2Id +
                " failed, assocTypeUri=\"" + assocTypeUri + "\", roleTypeUri1=\"" + roleTypeUri1 +
                "\", roleTypeUri2=\"" + roleTypeUri2 + "\"", e);
        }
    }

    // ---

    Iterable<AssocModelImpl> getAllAssocs() {
        return new ReadableIterable(db.fetchAllAssocs());
    }

    List<PlayerModel> getPlayerModels(long assocId) {
        return db.fetchPlayerModels(assocId);
    }

    // ---

    /**
     * Convenience.
     */
    AssocModelImpl createAssoc(String typeUri, PlayerModel player1, PlayerModel player2) {
        return createAssoc(mf.newAssocModel(typeUri, player1, player2));
    }

    /**
     * Creates a new association in the DB.
     */
    AssocModelImpl createAssoc(AssocModelImpl model) {
        try {
            em.fireEvent(CoreEvent.PRE_CREATE_ASSOCIATION, model);
            model.preCreate();
            //
            // store in DB
            db.storeAssoc(model);
            AssocModelImpl _model = updateValues(model, null);
            createAssocInstantiation(_model.getId(), _model.getTypeUri());
            //
            // Note: the postCreate() hook is invoked on the update model, *not* on the value integration result
            // (_model). Otherwise the programmatic vs. interactive detection would not work (see postCreate() comment
            // at CompDefModelImpl). "model" might be an CompDefModel while "_model" is always an AssocModel.
            model.postCreate();
            em.fireEvent(CoreEvent.POST_CREATE_ASSOCIATION, _model.instantiate());
            return _model;
        } catch (Exception e) {
            throw new RuntimeException("Creating association failed, model=" + model, e);
        }
    }

    // ---

    void updateAssoc(AssocModelImpl updateModel) {
        try {
            AssocModelImpl model = db.fetchAssoc(updateModel.getId());
            updateAssoc(model, updateModel);
            //
            // Note: there is no possible POST_UPDATE_ASSOCIATION_REQUEST event to fire here (compare to updateTopic()).
            // It would be equivalent to POST_UPDATE_ASSOCIATION. Per request exactly one association is updated.
            // Its children are always topics (never associations).
        } catch (Exception e) {
            throw new RuntimeException("Fetching and updating association " + updateModel.getId() + " failed", e);
        }
    }

    void updateAssoc(AssocModelImpl assoc, AssocModelImpl updateModel) {
        try {
            assoc.checkWriteAccess();
            assoc.update(updateModel);
        } catch (Exception e) {
            throw new RuntimeException("Updating association " + assoc.getId() + " failed, assoc=" + assoc +
                ", updateModel=" + updateModel, e);
        }
    }

    // ---

    /**
     * Convenience.
     */
    void deleteAssoc(long assocId) {
        try {
            deleteAssoc(db.fetchAssoc(assocId));
        } catch (IllegalStateException e) {
            // Note: db.fetchAssoc() might throw IllegalStateException and is no problem.
            // This happens when the association is deleted already. In this case nothing needs to be performed.
            //
            // Compare to DMXObjectModelImpl.delete()
            // TODO: introduce storage-vendor neutral DM exception.
            if (e.getMessage().equals("Node[" + assocId + "] has been deleted in this tx")) {
                logger.info("### Assoc " + assocId + " has already been deleted in this transaction. " +
                    "This can happen while delete-multi.");
            } else {
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("Fetching and deleting association " + assocId + " failed", e);
        }
    }

    void deleteAssoc(AssocModelImpl assoc) {
        try {
            assoc.checkWriteAccess();
            assoc.delete();
        } catch (Exception e) {
            throw new RuntimeException("Deleting association " + assoc.getId() + " failed", e);
        }
    }



    // ===

    void createTopicInstantiation(long topicId, String topicTypeUri) {
        try {
            AssocModelImpl assoc = mf.newAssocModel(INSTANTIATION,
                mf.newTopicPlayerModel(topicTypeUri, TYPE),
                mf.newTopicPlayerModel(topicId, INSTANCE)
            );
            db.storeAssoc(assoc);   // direct storage calls used here ### explain
            db.storeAssocValue(assoc.id, assoc.value, assoc.typeUri, false);     // isHtml=false
            createAssocInstantiation(assoc.id, assoc.typeUri);
        } catch (Exception e) {
            throw new RuntimeException("Associating topic " + topicId + " with topic type \"" +
                topicTypeUri + "\" failed", e);
        }
    }

    void createAssocInstantiation(long assocId, String assocTypeUri) {
        try {
            AssocModelImpl assoc = mf.newAssocModel(INSTANTIATION,
                mf.newTopicPlayerModel(assocTypeUri, TYPE),
                mf.newAssocPlayerModel(assocId, INSTANCE)
            );
            db.storeAssoc(assoc);   // direct storage calls used here ### explain
            db.storeAssocValue(assoc.id, assoc.value, assoc.typeUri, false);     // isHtml=false
        } catch (Exception e) {
            throw new RuntimeException("Associating association " + assocId + " with association type \"" +
                assocTypeUri + "\" failed", e);
        }
    }



    // === Types ===

    TopicTypeModelImpl getTopicType(String uri) {
        return _getTopicType(uri).checkReadAccess();
    }

    TopicTypeModelImpl getTopicTypeImplicitly(long topicId) {
        checkTopicReadAccess(topicId);
        return _getTopicType(typeUri(topicId));
    }

    // ---

    AssocTypeModelImpl getAssocType(String uri) {
        return _getAssocType(uri).checkReadAccess();
    }

    AssocTypeModelImpl getAssocTypeImplicitly(long assocId) {
        checkAssocReadAccess(assocId);
        return _getAssocType(typeUri(assocId));
    }

    // ---

    List<TopicTypeModelImpl> getAllTopicTypes() {
        try {
            List<TopicTypeModelImpl> topicTypes = new ArrayList();
            for (String uri : getTopicTypeUris()) {
                topicTypes.add(_getTopicType(uri));
            }
            return topicTypes;
        } catch (Exception e) {
            throw new RuntimeException("Fetching all topic types failed", e);
        }
    }

    List<AssocTypeModelImpl> getAllAssocTypes() {
        try {
            List<AssocTypeModelImpl> assocTypes = new ArrayList();
            for (String uri : getAssocTypeUris()) {
                assocTypes.add(_getAssocType(uri));
            }
            return assocTypes;
        } catch (Exception e) {
            throw new RuntimeException("Fetching all association types failed", e);
        }
    }

    // ---

    TopicTypeModelImpl createTopicType(TopicTypeModelImpl model) {
        try {
            em.fireEvent(CoreEvent.PRE_CREATE_TOPIC_TYPE, model);
            //
            // store in DB
            createType(model, URI_PREFIX_TOPIC_TYPE);
            //
            em.fireEvent(CoreEvent.INTRODUCE_TOPIC_TYPE, model.instantiate());
            //
            return model;
        } catch (Exception e) {
            throw new RuntimeException("Creating topic type \"" + model.getUri() + "\" failed", e);
        }
    }

    AssocTypeModelImpl createAssocType(AssocTypeModelImpl model) {
        try {
            em.fireEvent(CoreEvent.PRE_CREATE_ASSOCIATION_TYPE, model);
            //
            // store in DB
            createType(model, URI_PREFIX_ASSOCIATION_TYPE);
            //
            em.fireEvent(CoreEvent.INTRODUCE_ASSOCIATION_TYPE, model.instantiate());
            //
            return model;
        } catch (Exception e) {
            throw new RuntimeException("Creating association type \"" + model.getUri() + "\" failed", e);
        }
    }

    // ---

    void updateTopicType(TopicTypeModelImpl updateModel) {
        try {
            // Note: type lookup is by ID. The URI might have changed, the ID does not.
            TopicModelImpl topic = db.fetchTopic(updateModel.getId());
            topic.checkWriteAccess();
            _getTopicType(topic.getUri()).update(updateModel);
        } catch (Exception e) {
            throw new RuntimeException("Updating topic type failed, updateModel=" + updateModel, e);
        }
    }

    void updateAssocType(AssocTypeModelImpl updateModel) {
        try {
            // Note: type lookup is by ID. The URI might have changed, the ID does not.
            TopicModelImpl topic = db.fetchTopic(updateModel.getId());
            topic.checkWriteAccess();
            _getAssocType(topic.getUri()).update(updateModel);
        } catch (Exception e) {
            throw new RuntimeException("Updating association type failed, updateModel=" + updateModel, e);
        }
    }

    // ---

    void deleteTopicType(String topicTypeUri) {
        try {
            TypeModelImpl type = _getTopicType(topicTypeUri);
            type.checkWriteAccess();
            type.delete();
            // ### TODO: delete view config topics
        } catch (Exception e) {
            throw new RuntimeException("Deleting topic type \"" + topicTypeUri + "\" failed", e);
        }
    }

    void deleteAssocType(String assocTypeUri) {
        try {
            TypeModelImpl type = _getAssocType(assocTypeUri);
            type.checkWriteAccess();
            type.delete();
            // ### TODO: delete view config topics
        } catch (Exception e) {
            throw new RuntimeException("Deleting association type \"" + assocTypeUri + "\" failed", e);
        }
    }

    // ---

    TopicModelImpl createRoleType(TopicModelImpl model) {
        // check type URI argument
        String typeUri = model.getTypeUri();
        if (typeUri == null) {
            model.setTypeUri(ROLE_TYPE);
        } else {
            if (!typeUri.equals(ROLE_TYPE)) {
                throw new IllegalArgumentException("A role type is supposed to be of type \"dmx.core.role_type\" " +
                    "(found: \"" + typeUri + "\")");
            }
        }
        //
        return createSingleTopic(model, URI_PREFIX_ROLE_TYPE);
    }

    // ---

    /**
     * Type cache direct access. No permission check.
     */
    TopicTypeModelImpl _getTopicType(String uri) {
        return typeStorage.getTopicType(uri);
    }

    /**
     * Type cache direct access. No permission check.
     */
    AssocTypeModelImpl _getAssocType(String uri) {
        return typeStorage.getAssocType(uri);
    }



    // === Generic Object ===

    DMXObjectModelImpl getObject(long id) {
        return db.fetchObject(id).checkReadAccess();
    }



    // === Traversal ===

    // --- Topic Source ---

    List<RelatedTopicModelImpl> getTopicRelatedTopics(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                      String othersRoleTypeUri, String othersTopicTypeUri) {
        return filterReadables(db.fetchTopicRelatedTopics(topicId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri));
    }

    RelatedAssocModelImpl getTopicRelatedAssoc(long topicId, String assocTypeUri, String myRoleTypeUri,
                                               String othersRoleTypeUri, String othersAssocTypeUri) {
        RelatedAssocModelImpl assoc = sd.fetchTopicRelatedAssoc(topicId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri);
        return assoc != null ? assoc.checkReadAccess() : null;
    }

    List<RelatedAssocModelImpl> getTopicRelatedAssocs(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                      String othersRoleTypeUri, String othersAssocTypeUri) {
        return filterReadables(db.fetchTopicRelatedAssocs(topicId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri));
    }

    List<AssocModelImpl> getTopicAssocs(long topicId) {
        return filterReadables(db.fetchTopicAssocs(topicId));
    }

    // --- Assoc Source ---

    List<RelatedTopicModelImpl> getAssocRelatedTopics(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                      String othersRoleTypeUri, String othersTopicTypeUri) {
        return filterReadables(db.fetchAssocRelatedTopics(assocId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri));
    }

    RelatedAssocModelImpl getAssocRelatedAssoc(long assocId, String assocTypeUri, String myRoleTypeUri,
                                               String othersRoleTypeUri, String othersAssocTypeUri) {
        RelatedAssocModelImpl assoc = sd.fetchAssocRelatedAssoc(assocId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri);
        return assoc != null ? assoc.checkReadAccess() : null;
    }

    List<RelatedAssocModelImpl> getAssocRelatedAssocs(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                      String othersRoleTypeUri, String othersAssocTypeUri) {
        return filterReadables(db.fetchAssocRelatedAssocs(assocId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri));
    }

    List<AssocModelImpl> getAssocAssocs(long assocId) {
        return filterReadables(db.fetchAssocAssocs(assocId));
    }

    // --- Object Source ---

    RelatedTopicModelImpl getRelatedTopic(long objectId, String assocTypeUri, String myRoleTypeUri,
                                          String othersRoleTypeUri, String othersTopicTypeUri) {
        RelatedTopicModelImpl topic = sd.fetchRelatedTopic(objectId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri);
        return topic != null ? topic.checkReadAccess() : null;
    }

    List<RelatedTopicModelImpl> getRelatedTopics(long objectId, String assocTypeUri, String myRoleTypeUri,
                                                 String othersRoleTypeUri, String othersTopicTypeUri) {
        return filterReadables(db.fetchRelatedTopics(objectId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri));
    }



    // === Properties ===

    List<TopicModelImpl> getTopicsByProperty(String propUri, Object propValue) {
        return filterReadables(db.fetchTopicsByProperty(propUri, propValue));
    }

    List<TopicModelImpl> getTopicsByPropertyRange(String propUri, Number from, Number to) {
        return filterReadables(db.fetchTopicsByPropertyRange(propUri, from, to));
    }

    List<AssocModelImpl> getAssocsByProperty(String propUri, Object propValue) {
        return filterReadables(db.fetchAssocsByProperty(propUri, propValue));
    }

    List<AssocModelImpl> getAssocsByPropertyRange(String propUri, Number from, Number to) {
        return filterReadables(db.fetchAssocsByPropertyRange(propUri, from, to));
    }



    // === Access Control ===

    <M extends DMXObjectModelImpl> List<M> filterReadables(List<M> models) {
        Iterator<? extends DMXObjectModelImpl> i = models.iterator();
        while (i.hasNext()) {
            if (!i.next().isReadable()) {
                i.remove();
            }
        }
        return models;
    }

    <O extends DMXObject> List<O> instantiate(Iterable<? extends DMXObjectModelImpl> models) {
        List<O> objects = new ArrayList();
        for (DMXObjectModelImpl model : models) {
            objects.add(model.instantiate());
        }
        return objects;
    }

    // ---

    /**
     * @throws  AccessControlException  if the current user has no permission.
     */
    void checkTopicReadAccess(long topicId) {
        em.fireEvent(CoreEvent.CHECK_TOPIC_READ_ACCESS, topicId);
    }

    /**
     * @throws  AccessControlException  if the current user has no permission.
     */
    void checkAssocReadAccess(long assocId) {
        em.fireEvent(CoreEvent.CHECK_ASSOCIATION_READ_ACCESS, assocId);
    }

    // ---

    /**
     * @throws  AccessControlException  if the current user has no permission.
     */
    void checkTopicWriteAccess(long topicId) {
        em.fireEvent(CoreEvent.CHECK_TOPIC_WRITE_ACCESS, topicId);
    }

    /**
     * @throws  AccessControlException  if the current user has no permission.
     */
    void checkAssocWriteAccess(long assocId) {
        em.fireEvent(CoreEvent.CHECK_ASSOCIATION_WRITE_ACCESS, assocId);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Returns parent topics of the given type as found by child-to-parent traversal starting at the given child topics.
     */
    List<TopicModelImpl> parentTopics(String topicTypeUri, List<TopicModelImpl> childTopics) {
        List<TopicModelImpl> parentTopics = new ArrayList();
        for (TopicModelImpl childTopic : childTopics) {
            for (TopicModelImpl parentTopic : parentTopics(topicTypeUri, childTopic)) {
                if (!parentTopics.contains(parentTopic)) {
                    parentTopics.add(parentTopic);
                }
            }
        }
        return parentTopics;
    }

    List<TopicModelImpl> parentTopics(String topicTypeUri, TopicModelImpl childTopic) {
        List<TopicModelImpl> parentTopics = new ArrayList();
        // FIXME: check child topic read permission
        if (childTopic.typeUri.equals(topicTypeUri)) {
            parentTopics.add(childTopic);
        } else {
            for (TopicModelImpl parentTopic : childTopic.getRelatedTopics((String) null, CHILD, PARENT, null)) {
                parentTopics.addAll(parentTopics(topicTypeUri, parentTopic));
            }
        }
        return parentTopics;
    }

    // ---

    private List<String> getTopicTypeUris() {
        try {
            List<String> topicTypeUris = new ArrayList();
            // add meta types
            topicTypeUris.add(TOPIC_TYPE);
            topicTypeUris.add(ASSOC_TYPE);
            topicTypeUris.add(META_TYPE);
            // add regular types
            for (TopicModelImpl topicType : filterReadables(db.fetchTopics("typeUri", TOPIC_TYPE))) {
                topicTypeUris.add(topicType.getUri());
            }
            return topicTypeUris;
        } catch (Exception e) {
            throw new RuntimeException("Fetching list of topic type URIs failed", e);
        }
    }

    private List<String> getAssocTypeUris() {
        try {
            List<String> assocTypeUris = new ArrayList();
            for (TopicModelImpl assocType : filterReadables(db.fetchTopics("typeUri", ASSOC_TYPE))) {
                assocTypeUris.add(assocType.getUri());
            }
            return assocTypeUris;
        } catch (Exception e) {
            throw new RuntimeException("Fetching list of association type URIs failed", e);
        }
    }

    // ---

    private void createType(TypeModelImpl model, String uriPrefix) {
        // Note: the type topic is instantiated explicitly on a `TopicModel` (which is freshly created from the
        // `TypeModel`). Creating the type topic from the `TypeModel` directly would fail as topic creation implies
        // topic instantiation, and due to the polymorphic `instantiate()` method a `Type` object would be instantiated
        // (instead a `Topic` object). But instantiating a type implies per-user type projection, that is removing the
        // comp defs not readable by the current user. But at the time the type topic is stored in the DB its assoc
        // defs are not yet stored, and the readability check would fail.
        TopicModelImpl typeTopic = mf.newTopicModel(model);
        createSingleTopic(typeTopic, uriPrefix);        // create generic topic
        //
        model.id = typeTopic.id;
        model.uri = typeTopic.uri;
        //
        typeStorage.storeType(model);                   // store type-specific parts
    }

    private String typeUri(long objectId) {
        return (String) db.fetchProperty(objectId, "typeUri");
    }

    private void bootstrapTypeCache() {
        TopicTypeModelImpl metaMetaType = mf.newTopicTypeModel("dmx.core.meta_meta_type", "Meta Meta Type", TEXT);
        metaMetaType.setTypeUri("dmx.core.meta_meta_meta_type");
        typeStorage.putInTypeCache(metaMetaType);
    }

    // ---

    private <M extends DMXObjectModelImpl> M updateValues(M updateModel, M targetObject) {
        M value = new ValueIntegrator(this).integrate(updateModel, targetObject, null).value;
        // sanity check
        if (value == null) {
            throw new RuntimeException("ValueIntegrator yields no result");
        }
        //
        return value;
    }
}
