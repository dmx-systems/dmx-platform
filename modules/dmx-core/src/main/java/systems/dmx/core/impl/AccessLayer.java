package systems.dmx.core.impl;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.DMXObject;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.RelatedObjectModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.storage.spi.DMXStorage;
import systems.dmx.core.util.DMXUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



/**
 * Vendor agnostic access control on top of vendor specific storage.
 */
public final class AccessLayer {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String URI_PREFIX_TOPIC_TYPE       = "domain.project.topic_type_";
    private static final String URI_PREFIX_ASSOCIATION_TYPE = "domain.project.assoc_type_";
    private static final String URI_PREFIX_ROLE_TYPE        = "domain.project.role_type_";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    public StorageDecorator sd;     // accessed by storage tests

    DMXStorage db;
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
            TopicModelImpl topic = sd.fetchTopic("uri", uri);
            return topic != null ? topic.checkReadAccess() : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching topic failed, uri=\"" + uri + "\"", e);
        }
    }

    List<TopicModelImpl> getTopicsByType(String topicTypeUri) {
        return filterReadables(_getTopicsByType(topicTypeUri));
    }

    TopicModelImpl getTopicByValue(String key, SimpleValue value) {
        try {
            TopicModelImpl topic = sd.fetchTopic(key, value.value());
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

    List<TopicModelImpl> queryTopics(String key, String query) {
        try {
            return filterReadables(db.queryTopics(key, query));
        } catch (Exception e) {
            throw new RuntimeException("Querying topics failed, key=\"" + key + "\", query=" + query, e);
        }
    }

    List<TopicModelImpl> queryTopicsFulltext(String query, String topicTypeUri, boolean searchChildTopics) {
        try {
            logger.fine("Querying topics fulltext, query=\"" + query + "\", topicTypeUri=" + topicTypeUri +
                ", searchChildTopics=" + searchChildTopics);
            List<TopicModelImpl> topics;
            if (topicTypeUri != null && searchChildTopics) {
                topics = parentObjects(topicTypeUri, db.queryTopicsFulltext(null, query));      // key=null
            } else {
                topics = db.queryTopicsFulltext(topicTypeUri, query);
            }
            return filterReadables(topics);
        } catch (Exception e) {
            throw new RuntimeException("Querying topics fulltext failed, query=\"" + query + "\", topicTypeUri=" +
                topicTypeUri + ", searchChildTopics=" + searchChildTopics, e);
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

    /**
     * Creates a single topic in the DB.
     * No child topics are created.
     */
    TopicModelImpl createSingleTopic(TopicModelImpl model) {
        try {
            // store in DB
            db.storeTopic(model);
            if (model.getType().isSimple()) {
                model.storeSimpleValue();
            }
            createTopicInstantiation(model.getId(), model.getTypeUri());
            //
            return model;
        } catch (Exception e) {
            throw new RuntimeException("Creating single topic failed, model=" + model, e);
        }
    }

    /**
     * Creates a sole type topic in the DB, auto-generates a default type URI if due, and fires POST_CREATE_TOPIC.
     * No type-specific parts are created.
     * <p>
     * Used to create topic types, assoc types, and role types.
     */
    private TopicModelImpl createTypeTopic(TopicModelImpl model, String uriPrefix) {
        try {
            createSingleTopic(model);
            // set default URI
            // If no URI is given the topic gets a default URI based on its ID, if requested.
            // Note: this must be done *after* the topic is stored. The ID is not known before.
            // Note: in case no URI was given: once stored a topic's URI is empty (not null).
            if (model.getUri().equals("")) {
                model.updateUri(uriPrefix + model.getId());     // update memory + DB
            }
            // Note: creating a type does not involve value integration, so POST_CREATE_TOPIC is fired from here.
            model.postCreate();
            em.fireEvent(CoreEvent.POST_CREATE_TOPIC, model.instantiate());
            //
            return model;
        } catch (Exception e) {
            throw new RuntimeException("Creating type topic failed, model=" + model + ", uriPrefix=\"" + uriPrefix +
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

    List<AssocModelImpl> getAssocsByType(String assocTypeUri) {
        return filterReadables(_getAssocsByType(assocTypeUri));
    }

    AssocModelImpl getAssocByValue(String key, SimpleValue value) {
        try {
            AssocModelImpl assoc = sd.fetchAssoc(key, value.value());
            return assoc != null ? assoc.checkReadAccess() : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching assoc failed, key=\"" + key + "\", value=" + value, e);
        }
    }

    List<AssocModelImpl> queryAssocs(String key, String query) {
        try {
            return filterReadables(db.queryAssocs(key, query));
        } catch (Exception e) {
            throw new RuntimeException("Querying assocs failed, key=\"" + key + "\", query=" + query, e);
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
        return createTypeTopic(model, URI_PREFIX_ROLE_TYPE);
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

    <M extends DMXObjectModelImpl> List<M> query(
            String topicQuery, String topicTypeUri, boolean searchTopicChildren,
            String assocQuery, String assocTypeUri, boolean searchAssocChildren) {
        try {
            logger.fine("Querying related topics fulltext, topicQuery=\"" + topicQuery + "\", topicTypeUri=" +
                topicTypeUri + ", searchTopicChildren=" + searchTopicChildren + ", assocQuery=\"" + assocQuery +
                "\", assocTypeUri=" + assocTypeUri + ", searchAssocChildren=" + searchAssocChildren);
            // topic filter
            List<TopicModelImpl> topics = filterReadables(queryTopics(topicQuery, topicTypeUri, searchTopicChildren));
            if (topics.isEmpty()) {
                boolean topicFilter = !topicQuery.isEmpty() || topicTypeUri != null;
                if (topicFilter) {
                    logger.info("topics: " + topics.size() + ", result: -> empty");
                    return new ArrayList();
                }
            }
            // assoc filter
            List<AssocModelImpl> assocs = filterReadables(queryAssocs(assocQuery, assocTypeUri, searchAssocChildren));
            if (assocs.isEmpty()) {
                boolean assocFilter = !assocQuery.isEmpty() || assocTypeUri != null;
                if (assocFilter) {
                    logger.info("topics: " + topics.size() + ", assocs: " + assocs.size() + ", result: -> empty");
                    return new ArrayList();
                } else {
                    logger.info("topics: " + topics.size() + ", assocs: " + assocs.size() + ", result: -> topics");
                    return (List<M>) topics;
                }
            }
            // combine filters -> return assocs
            return (List<M>) filterAssocsByPlayer(topics, assocs);
        } catch (Exception e) {
            throw new RuntimeException("Querying related topics fulltext failed, topicQuery=\"" + topicQuery +
                "\", topicTypeUri=" + topicTypeUri + ", searchTopicChildren=" + searchTopicChildren +
                ", assocQuery=\"" + assocQuery + "\", assocTypeUri=" + assocTypeUri + ", searchAssocChildren=" +
                searchAssocChildren, e);
        }
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

    private List<TopicModelImpl> queryTopics(String topicQuery, String topicTypeUri, boolean searchTopicChildren) {
        List<TopicModelImpl> topics;
        if (!topicQuery.isEmpty()) {
            if (topicTypeUri != null) {
                if (searchTopicChildren) {
                    topics = parentObjects(topicTypeUri, db.queryTopicsFulltext(null, topicQuery));     // key=null
                } else {
                    topics = db.queryTopicsFulltext(topicTypeUri, topicQuery);
                }
            } else {
                topics = db.queryTopicsFulltext(null, topicQuery);      // key=null
            }
        } else {
            if (topicTypeUri != null) {
                topics = _getTopicsByType(topicTypeUri);
            } else {
                // do nothing
                topics = new ArrayList();
            }
        }
        return topics;
    }

    private List<AssocModelImpl> queryAssocs(String assocQuery, String assocTypeUri, boolean searchAssocChildren) {
        List<AssocModelImpl> assocs;
        if (!assocQuery.isEmpty()) {
            if (assocTypeUri != null) {
                // While children are always topics direct assoc matches are supported as well.
                // So the queryAssocs() logic is little different from queryTopics() above.
                if (!assocQuery.equals("*")) {
                    assocs = db.queryAssocsFulltext(assocTypeUri, assocQuery);
                } else {
                    assocs = _getAssocsByType(assocTypeUri);
                }
                if (searchAssocChildren) {
                    assocs.addAll(parentObjects(assocTypeUri, db.queryTopicsFulltext(null, assocQuery)));    // key=null
                }
            } else {
                if (!assocQuery.equals("*")) {
                    assocs = db.queryAssocsFulltext(null, assocQuery);                                       // key=null
                } else {
                    // TODO: optimization.
                    // If there is a topic result: collect the topic's assocs directly.
                    // If no topic filter is set: possibly do nothing. A plain "all assocs" query might make no sense.
                    assocs = _getAllAssocs();
                }
            }
        } else {
            if (assocTypeUri != null) {
                assocs = _getAssocsByType(assocTypeUri);
            } else {
                // do nothing
                assocs = new ArrayList();
            }
        }
        return assocs;
    }

    // ---

    /**
     * Prerequisites:
     *   - "topics" and "assocs" are permission checked already
     *   - only called with empty "topics" if no topic filter was set
     */
    private List<AssocModelImpl> filterAssocsByPlayer(List<TopicModelImpl> topics, List<AssocModelImpl> assocs) {
        List<AssocModelImpl> result = new ArrayList();
        for (AssocModelImpl assoc : assocs) {
            PlayerModelImpl p1 = assoc.getPlayer1();
            PlayerModelImpl p2 = assoc.getPlayer2();
            if (!(p1 instanceof TopicPlayerModelImpl) || !(p2 instanceof TopicPlayerModelImpl)) {
                continue;
            }
            if (topics.isEmpty()) {
                p1.getDMXObject();      // fetch player object
                p2.getDMXObject();      // fetch player object
                result.add(assoc);
            } else {
                TopicModelImpl topic1 = DMXUtils.findById(p1.getId(), topics);
                TopicModelImpl topic2 = DMXUtils.findById(p2.getId(), topics);
                if (topic1 == null && topic2 == null) {
                    continue;
                }
                initPlayerObject(p1, topic1);
                initPlayerObject(p2, topic2);
                result.add(assoc);
            }
        }
        logger.info("topics: " + topics.size() + ", assocs: " + assocs.size() + ", result: " + result.size());
        return result;
    }

    private void initPlayerObject(PlayerModelImpl player, TopicModelImpl topic) {
        if (topic != null) {
            player.object = topic;
            topic.getChildTopics().set("dmx.core.is_match", true);
        } else {
            player.getDMXObject();      // fetch player object
        }
    }

    // ---

    /**
     * Returns parent topics of the given type as found by child-to-parent traversal starting at the given topics.
     * ### FIXDOC
     */
    private <M extends DMXObjectModelImpl> List<M> parentObjects(String typeUri, List<TopicModelImpl> topics) {
        List<M> result = new ArrayList();
        for (TopicModelImpl topic : topics) {
            for (DMXObjectModelImpl parentObject : _parentObjects(typeUri, topic)) {
                if (!result.contains(parentObject)) {       // TODO: equality and rel-objects?
                    result.add((M) parentObject);
                }
            }
        }
        return result;
    }

    private List<DMXObjectModelImpl> _parentObjects(String typeUri, DMXObjectModelImpl object) {
        List<DMXObjectModelImpl> result = new ArrayList();
        if (object.typeUri.equals(typeUri)) {
            result.add(object);
        } else {
            for (RelatedObjectModel parentObject : object.getRelatedObjects(null, CHILD, PARENT, null)) {
                result.addAll(_parentObjects(typeUri, (DMXObjectModelImpl) parentObject));
            }
        }
        return result;
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

    // DB direct access. No permission check.
    private List<TopicModelImpl> _getTopicsByType(String topicTypeUri) {
        try {
            return _getTopicType(topicTypeUri).getAllInstances();
        } catch (Exception e) {
            throw new RuntimeException("Fetching topics by type failed, topicTypeUri=\"" + topicTypeUri + "\"", e);
        }
    }

    // DB direct access. No permission check.
    private List<AssocModelImpl> _getAssocsByType(String assocTypeUri) {
        try {
            return _getAssocType(assocTypeUri).getAllInstances();
        } catch (Exception e) {
            throw new RuntimeException("Fetching topics by type failed, assocTypeUri=\"" + assocTypeUri + "\"", e);
        }
    }

    // DB direct access. No permission check.
    private List<AssocModelImpl> _getAllAssocs() {
        List<AssocModelImpl> assocs = new ArrayList();
        for (AssocModelImpl assoc : db.fetchAllAssocs()) {
            assocs.add(assoc);
        }
        return assocs;
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
        createTypeTopic(typeTopic, uriPrefix);          // create generic topic
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
