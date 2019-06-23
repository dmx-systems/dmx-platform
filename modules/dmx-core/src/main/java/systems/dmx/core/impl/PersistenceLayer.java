package systems.dmx.core.impl;

import systems.dmx.core.Assoc;
import systems.dmx.core.AssocType;
import systems.dmx.core.DMXObject;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
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
 * ### TODO: no instatiations here
 * ### TODO: hold storage object in instance variable (instead deriving) to make direct DB access more explicit
 */
public final class PersistenceLayer extends StorageDecorator {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String URI_PREFIX_TOPIC_TYPE       = "domain.project.topic_type_";
    private static final String URI_PREFIX_ASSOCIATION_TYPE = "domain.project.assoc_type_";
    private static final String URI_PREFIX_ROLE_TYPE        = "domain.project.role_type_";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    TypeStorage typeStorage;
    EventManager em;
    ModelFactoryImpl mf;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public PersistenceLayer(DMXStorage storage) {
        super(storage);
        // Note: mf must be initialzed before the type storage is instantiated
        this.em = new EventManager();
        this.mf = (ModelFactoryImpl) storage.getModelFactory();
        this.typeStorage = new TypeStorage(this);
        //
        // Note: this is a constructor side effect. This is a cyclic dependency. This is nasty.
        // ### TODO: explain why we do it.
        mf.pl = this;
        //
        bootstrapTypeCache();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Topics ===

    TopicModelImpl getTopic(long topicId) {
        try {
            return fetchTopic(topicId).checkReadAccess();
        } catch (Exception e) {
            throw new RuntimeException("Fetching topic " + topicId + " failed", e);
        }
    }

    TopicModelImpl getTopicByUri(String uri) {
        try {
            TopicModelImpl topic = fetchTopicByUri(uri);
            return topic != null ? topic.checkReadAccess() : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching topic failed, uri=\"" + uri + "\"", e);
        }
    }

    TopicModelImpl getTopicByValue(String key, SimpleValue value) {
        try {
            TopicModelImpl topic = fetchTopic(key, value);
            return topic != null ? topic.checkReadAccess() : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching topic failed, key=\"" + key + "\", value=" + value, e);
        }
    }

    List<TopicModelImpl> getTopicsByValue(String key, SimpleValue value) {
        try {
            return filterReadables(fetchTopics(key, value));
        } catch (Exception e) {
            throw new RuntimeException("Fetching topics failed, key=\"" + key + "\", value=" + value, e);
        }
    }

    List<TopicModelImpl> getTopicsByType(String topicTypeUri) {
        try {
            return filterReadables(_getTopicType(topicTypeUri).getAllInstances());
        } catch (Exception e) {
            throw new RuntimeException("Fetching topics failed, topicTypeUri=\"" + topicTypeUri + "\"", e);
        }
    }

    List<TopicModelImpl> searchTopics(String searchTerm, String fieldUri) {
        try {
            return filterReadables(queryTopics(fieldUri, new SimpleValue(searchTerm)));
        } catch (Exception e) {
            throw new RuntimeException("Searching topics failed, searchTerm=\"" + searchTerm + "\", fieldUri=\"" +
                fieldUri + "\"", e);
        }
    }

    Iterable<Topic> getAllTopics() {
        return new TopicIterable(this);
    }

    // ---

    TopicImpl createTopic(TopicModelImpl model) {
        try {
            return updateValues(model, null).instantiate();
        } catch (Exception e) {
            throw new RuntimeException("Creating topic failed, model=" + model, e);
        }
    }

    // ---

    // ### TODO: drop "firePostCreate" param
    TopicImpl createSingleTopic(TopicModelImpl model, boolean firePostCreate) {
        return createSingleTopic(model, null, firePostCreate);   // uriPrefix=null
    }

    /**
     * Creates a single topic in the DB.
     * No child topics are created.
     *
     * ### TODO: drop "firePostCreate" param
     */
    private TopicImpl createSingleTopic(TopicModelImpl model, String uriPrefix, boolean firePostCreate) {
        try {
            em.fireEvent(CoreEvent.PRE_CREATE_TOPIC, model);
            //
            model.preCreate();
            //
            // 1) store in DB
            storeTopic(model);
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
            model.postCreate();
            //
            TopicImpl topic = model.instantiate();
            if (firePostCreate) {
                em.fireEvent(CoreEvent.POST_CREATE_TOPIC, topic);
            }
            return topic;
        } catch (Exception e) {
            throw new RuntimeException("Creating single topic failed, model=" + model + ", uriPrefix=\"" + uriPrefix +
                "\"", e);
        }
    }

    // ---

    void updateTopic(TopicModelImpl updateModel) {
        try {
            updateTopic(
                fetchTopic(updateModel.getId()),
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
            deleteTopic(fetchTopic(topicId));
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

    Assoc getAssoc(long assocId) {
        try {
            return checkReadAccessAndInstantiate(fetchAssoc(assocId));
        } catch (Exception e) {
            throw new RuntimeException("Fetching association " + assocId + " failed", e);
        }
    }

    Assoc getAssocByValue(String key, SimpleValue value) {
        try {
            AssocModelImpl assoc = fetchAssoc(key, value);
            return assoc != null ? this.checkReadAccessAndInstantiate(assoc) : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching association failed (key=\"" + key + "\", value=\"" + value + "\")", e);
        }
    }

    List<Assoc> getAssocsByValue(String key, SimpleValue value) {
        try {
            return checkReadAccessAndInstantiate(fetchAssocs(key, value));
        } catch (Exception e) {
            throw new RuntimeException("Fetching associations failed (key=\"" + key + "\", value=\"" + value + "\")",
                e);
        }
    }

    AssocImpl getAssoc(String assocTypeUri, long topic1Id, long topic2Id, String roleTypeUri1, String roleTypeUri2) {
        String info = "assocTypeUri=\"" + assocTypeUri + "\", topic1Id=" + topic1Id + ", topic2Id=" + topic2Id +
            ", roleTypeUri1=\"" + roleTypeUri1 + "\", roleTypeUri2=\"" + roleTypeUri2 + "\"";
        try {
            AssocModelImpl assoc = fetchAssoc(assocTypeUri, topic1Id, topic2Id, roleTypeUri1, roleTypeUri2);
            return assoc != null ? this.checkReadAccessAndInstantiate(assoc) : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching association failed (" + info + ")", e);
        }
    }

    Assoc getAssocBetweenTopicAndAssoc(String assocTypeUri, long topicId, long assocId, String topicRoleTypeUri,
                                       String assocRoleTypeUri) {
        String info = "assocTypeUri=\"" + assocTypeUri + "\", topicId=" + topicId + ", assocId=" + assocId +
            ", topicRoleTypeUri=\"" + topicRoleTypeUri + "\", assocRoleTypeUri=\"" + assocRoleTypeUri + "\"";
        logger.info(info);
        try {
            AssocModelImpl assoc = fetchAssocBetweenTopicAndAssoc(assocTypeUri, topicId, assocId,
                topicRoleTypeUri, assocRoleTypeUri);
            return assoc != null ? this.checkReadAccessAndInstantiate(assoc) : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching association failed (" + info + ")", e);
        }
    }

    // ---

    List<Assoc> getAssocsByType(String assocTypeUri) {
        try {
            return checkReadAccessAndInstantiate(_getAssocType(assocTypeUri).getAllInstances());
        } catch (Exception e) {
            throw new RuntimeException("Fetching associations by type failed (assocTypeUri=\"" + assocTypeUri + "\")",
                e);
        }
    }

    List<Assoc> getAssocs(long topic1Id, long topic2Id) {
        return getAssocs(null, topic1Id, topic2Id);   // assocTypeUri=null
    }

    List<Assoc> getAssocs(String assocTypeUri, long topic1Id, long topic2Id) {
        return getAssocs(assocTypeUri, topic1Id, topic2Id, null, null);   // roleTypeUri1=null, roleTypeUri2=null
    }

    List<Assoc> getAssocs(String assocTypeUri, long topic1Id, long topic2Id, String roleTypeUri1, String roleTypeUri2) {
        return instantiate(_getAssocs(assocTypeUri, topic1Id, topic2Id, roleTypeUri1, roleTypeUri2));
    }

    /**
     * Fetches from DB and filters READables. No instantiation.
     *
     * ### TODO: drop this. Use the new traversal methods instead.
     */
    Iterable<AssocModelImpl> _getAssocs(String assocTypeUri, long topic1Id, long topic2Id, String roleTypeUri1,
                                                                                           String roleTypeUri2) {
        logger.fine("assocTypeUri=\"" + assocTypeUri + "\", topic1Id=" + topic1Id + ", topic2Id=" + topic2Id +
            ", roleTypeUri1=\"" + roleTypeUri1 + "\", roleTypeUri2=\"" + roleTypeUri2 + "\"");
        try {
            return filterReadables(fetchAssocs(assocTypeUri, topic1Id, topic2Id, roleTypeUri1, roleTypeUri2));
        } catch (Exception e) {
            throw new RuntimeException("Fetching associations between topics " + topic1Id + " and " + topic2Id +
                " failed (assocTypeUri=\"" + assocTypeUri + "\", roleTypeUri1=\"" + roleTypeUri1 +
                "\", roleTypeUri2=\"" + roleTypeUri2 + "\")", e);
        }
    }

    // ---

    Iterable<Assoc> getAllAssocs() {
        return new AssocIterable(this);
    }

    List<PlayerModel> getPlayerModels(long assocId) {
        return fetchPlayerModels(assocId);
    }

    // ---

    /**
     * Convenience.
     */
    AssocImpl createAssoc(String typeUri, PlayerModel player1, PlayerModel player2) {
        return createAssoc(mf.newAssocModel(typeUri, player1, player2));
    }

    /**
     * Creates a new association in the DB.
     */
    AssocImpl createAssoc(AssocModelImpl model) {
        try {
            em.fireEvent(CoreEvent.PRE_CREATE_ASSOCIATION, model);
            //
            model.preCreate();
            //
            // 1) store in DB
            storeAssoc(model);
            AssocModelImpl _model = updateValues(model, null);
            createAssocInstantiation(_model.getId(), _model.getTypeUri());
            // 2) instantiate
            AssocImpl assoc = _model.instantiate();
            //
            // Note 1: the postCreate() hook is invoked on the update model, *not* on the value integration result
            // (_model). Otherwise the programmatic vs. interactive detection would not work (see postCreate() comment
            // at CompDefModelImpl). "model" might be an CompDefModel while "_model" is always an AssocModel.
            // Note 2: postCreate() creates and caches the comp def based on "model". Cached comp defs need an
            // up-to-date value (as being displayed in webclient's type editor). The value is calculated while
            // value integration. We must transfer that value to "model".
            // TODO: rethink this solution.
            model.value = _model.value;
            model.postCreate();
            //
            em.fireEvent(CoreEvent.POST_CREATE_ASSOCIATION, assoc);
            return assoc;
        } catch (Exception e) {
            throw new RuntimeException("Creating association failed, model=" + model, e);
        }
    }

    // ---

    void updateAssoc(AssocModelImpl updateModel) {
        try {
            AssocModelImpl model = fetchAssoc(updateModel.getId());
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
            deleteAssoc(fetchAssoc(assocId));
        } catch (IllegalStateException e) {
            // Note: fetchAssoc() might throw IllegalStateException and is no problem.
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
            AssocModelImpl assoc = mf.newAssocModel("dmx.core.instantiation",
                mf.newTopicPlayerModel(topicTypeUri, "dmx.core.type"),
                mf.newTopicPlayerModel(topicId, "dmx.core.instance")
            );
            storeAssoc(assoc);   // direct storage calls used here ### explain
            storeAssocValue(assoc.id, assoc.value, assoc.typeUri, false);     // isHtml=false
            createAssocInstantiation(assoc.id, assoc.typeUri);
        } catch (Exception e) {
            throw new RuntimeException("Associating topic " + topicId + " with topic type \"" +
                topicTypeUri + "\" failed", e);
        }
    }

    void createAssocInstantiation(long assocId, String assocTypeUri) {
        try {
            AssocModelImpl assoc = mf.newAssocModel("dmx.core.instantiation",
                mf.newTopicPlayerModel(assocTypeUri, "dmx.core.type"),
                mf.newAssocPlayerModel(assocId, "dmx.core.instance")
            );
            storeAssoc(assoc);   // direct storage calls used here ### explain
            storeAssocValue(assoc.id, assoc.value, assoc.typeUri, false);     // isHtml=false
        } catch (Exception e) {
            throw new RuntimeException("Associating association " + assocId + " with association type \"" +
                assocTypeUri + "\" failed", e);
        }
    }



    // === Types ===

    TopicTypeImpl getTopicType(String uri) {
        return checkReadAccessAndInstantiate(_getTopicType(uri));
    }

    TopicTypeImpl getTopicTypeImplicitly(long topicId) {
        checkTopicReadAccess(topicId);
        return _getTopicType(typeUri(topicId)).instantiate();
    }

    // ---

    AssocTypeImpl getAssocType(String uri) {
        return checkReadAccessAndInstantiate(_getAssocType(uri));
    }

    AssocTypeImpl getAssocTypeImplicitly(long assocId) {
        checkAssocReadAccess(assocId);
        return _getAssocType(typeUri(assocId)).instantiate();
    }

    // ---

    List<TopicType> getAllTopicTypes() {
        try {
            List<TopicType> topicTypes = new ArrayList();
            for (String uri : getTopicTypeUris()) {
                topicTypes.add(_getTopicType(uri).instantiate());
            }
            return topicTypes;
        } catch (Exception e) {
            throw new RuntimeException("Fetching all topic types failed", e);
        }
    }

    List<AssocType> getAllAssocTypes() {
        try {
            List<AssocType> assocTypes = new ArrayList();
            for (String uri : getAssocTypeUris()) {
                assocTypes.add(_getAssocType(uri).instantiate());
            }
            return assocTypes;
        } catch (Exception e) {
            throw new RuntimeException("Fetching all association types failed", e);
        }
    }

    // ---

    TopicTypeImpl createTopicType(TopicTypeModelImpl model) {
        try {
            em.fireEvent(CoreEvent.PRE_CREATE_TOPIC_TYPE, model);
            //
            // store in DB
            createType(model, URI_PREFIX_TOPIC_TYPE);
            //
            TopicTypeImpl topicType = model.instantiate();
            em.fireEvent(CoreEvent.INTRODUCE_TOPIC_TYPE, topicType);
            //
            return topicType;
        } catch (Exception e) {
            throw new RuntimeException("Creating topic type \"" + model.getUri() + "\" failed", e);
        }
    }

    AssocTypeImpl createAssocType(AssocTypeModelImpl model) {
        try {
            em.fireEvent(CoreEvent.PRE_CREATE_ASSOCIATION_TYPE, model);
            //
            // store in DB
            createType(model, URI_PREFIX_ASSOCIATION_TYPE);
            //
            AssocTypeImpl assocType = model.instantiate();
            em.fireEvent(CoreEvent.INTRODUCE_ASSOCIATION_TYPE, assocType);
            //
            return assocType;
        } catch (Exception e) {
            throw new RuntimeException("Creating association type \"" + model.getUri() + "\" failed", e);
        }
    }

    // ---

    void updateTopicType(TopicTypeModelImpl updateModel) {
        try {
            // Note: type lookup is by ID. The URI might have changed, the ID does not.
            TopicModelImpl topic = fetchTopic(updateModel.getId());
            topic.checkWriteAccess();
            _getTopicType(topic.getUri()).update(updateModel);
        } catch (Exception e) {
            throw new RuntimeException("Updating topic type failed, updateModel=" + updateModel, e);
        }
    }

    void updateAssocType(AssocTypeModelImpl updateModel) {
        try {
            // Note: type lookup is by ID. The URI might have changed, the ID does not.
            TopicModelImpl topic = fetchTopic(updateModel.getId());
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

    Topic createRoleType(TopicModelImpl model) {
        // check type URI argument
        String typeUri = model.getTypeUri();
        if (typeUri == null) {
            model.setTypeUri("dmx.core.role_type");
        } else {
            if (!typeUri.equals("dmx.core.role_type")) {
                throw new IllegalArgumentException("A role type is supposed to be of type \"dmx.core.role_type\" " +
                    "(found: \"" + typeUri + "\")");
            }
        }
        //
        return createSingleTopic(model, URI_PREFIX_ROLE_TYPE, true);
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

    DMXObject getObject(long id) {
        return checkReadAccessAndInstantiate(fetchObject(id));
    }



    // === Traversal ===

    // --- Topic Source ---

    List<RelatedTopicModelImpl> getTopicRelatedTopics(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                      String othersRoleTypeUri, String othersTopicTypeUri) {
        return filterReadables(fetchTopicRelatedTopics(topicId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri));
    }

    RelatedAssocModelImpl getTopicRelatedAssoc(long topicId, String assocTypeUri, String myRoleTypeUri,
                                               String othersRoleTypeUri, String othersAssocTypeUri) {
        RelatedAssocModelImpl assoc = fetchTopicRelatedAssoc(topicId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri);
        return assoc != null ? assoc.checkReadAccess() : null;
    }

    List<RelatedAssocModelImpl> getTopicRelatedAssocs(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                      String othersRoleTypeUri, String othersAssocTypeUri) {
        return filterReadables(fetchTopicRelatedAssocs(topicId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri));
    }

    List<AssocModelImpl> getTopicAssocs(long topicId) {
        return filterReadables(fetchTopicAssocs(topicId));
    }

    // --- Assoc Source ---

    List<RelatedTopicModelImpl> getAssocRelatedTopics(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                      String othersRoleTypeUri, String othersTopicTypeUri) {
        return filterReadables(fetchAssocRelatedTopics(assocId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri));
    }

    RelatedAssocModelImpl getAssocRelatedAssoc(long assocId, String assocTypeUri, String myRoleTypeUri,
                                               String othersRoleTypeUri, String othersAssocTypeUri) {
        RelatedAssocModelImpl assoc = fetchAssocRelatedAssoc(assocId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri);
        return assoc != null ? assoc.checkReadAccess() : null;
    }

    List<RelatedAssocModelImpl> getAssocRelatedAssocs(long assocId, String assocTypeUri, String myRoleTypeUri,
                                                      String othersRoleTypeUri, String othersAssocTypeUri) {
        return filterReadables(fetchAssocRelatedAssocs(assocId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri));
    }

    List<AssocModelImpl> getAssocAssocs(long assocId) {
        return filterReadables(fetchAssocAssocs(assocId));
    }

    // --- Object Source ---

    RelatedTopicModelImpl getRelatedTopic(long objectId, String assocTypeUri, String myRoleTypeUri,
                                          String othersRoleTypeUri, String othersTopicTypeUri) {
        RelatedTopicModelImpl topic = fetchRelatedTopic(objectId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri);
        return topic != null ? topic.checkReadAccess() : null;
    }

    List<RelatedTopicModelImpl> getRelatedTopics(long objectId, String assocTypeUri, String myRoleTypeUri,
                                                 String othersRoleTypeUri, String othersTopicTypeUri) {
        return filterReadables(fetchRelatedTopics(objectId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri));
    }



    // === Properties ===

    List<Topic> getTopicsByProperty(String propUri, Object propValue) {
        return checkReadAccessAndInstantiate(fetchTopicsByProperty(propUri, propValue));
    }

    List<Topic> getTopicsByPropertyRange(String propUri, Number from, Number to) {
        return checkReadAccessAndInstantiate(fetchTopicsByPropertyRange(propUri, from, to));
    }

    List<Assoc> getAssocsByProperty(String propUri, Object propValue) {
        return checkReadAccessAndInstantiate(fetchAssocsByProperty(propUri, propValue));
    }

    List<Assoc> getAssocsByPropertyRange(String propUri, Number from, Number to) {
        return checkReadAccessAndInstantiate(fetchAssocsByPropertyRange(propUri, from, to));
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Access Control / Instantiation ===

    // These methods 1) instantiate objects from models, and 2) check the READ permission for each model.
    // Call these methods when passing objects fetched from the DB to the user.

    // ### TODO: drop this. No instatiations in this class.
    <O> O checkReadAccessAndInstantiate(DMXObjectModelImpl model) {
        return (O) model.checkReadAccess().instantiate();
    }

    // ### TODO: drop this. No instatiations in this class.
    <O> List<O> checkReadAccessAndInstantiate(List<? extends DMXObjectModelImpl> models) {
        return instantiate(filterReadables(models));
    }

    // ---

    private <M extends DMXObjectModelImpl> List<M> filterReadables(List<M> models) {
        Iterator<? extends DMXObjectModelImpl> i = models.iterator();
        while (i.hasNext()) {
            if (!i.next().isReadable()) {
                i.remove();
            }
        }
        return models;
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



    // === Instantiation ===

    // ### TODO: move to kernel utils? To be dropped completely? Copy exists in CoreServiceImpl.java
    <O> List<O> instantiate(Iterable<? extends DMXObjectModelImpl> models) {
        List<O> objects = new ArrayList();
        for (DMXObjectModelImpl model : models) {
            objects.add((O) model.instantiate());
        }
        return objects;
    }



    // ===

    private List<String> getTopicTypeUris() {
        try {
            List<String> topicTypeUris = new ArrayList();
            // add meta types
            topicTypeUris.add("dmx.core.topic_type");
            topicTypeUris.add("dmx.core.assoc_type");
            topicTypeUris.add("dmx.core.meta_type");
            // add regular types
            for (TopicModel topicType : filterReadables(fetchTopics("typeUri", new SimpleValue(
                                                                    "dmx.core.topic_type")))) {
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
            for (TopicModel assocType : filterReadables(fetchTopics("typeUri", new SimpleValue(
                                                                    "dmx.core.assoc_type")))) {
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
        createSingleTopic(typeTopic, uriPrefix, true);    // create generic topic
        //
        model.id = typeTopic.id;
        model.uri = typeTopic.uri;
        //
        typeStorage.storeType(model);                     // store type-specific parts
    }

    private String typeUri(long objectId) {
        return (String) fetchProperty(objectId, "typeUri");
    }

    private void bootstrapTypeCache() {
        TopicTypeModelImpl metaMetaType = mf.newTopicTypeModel("dmx.core.meta_meta_type", "Meta Meta Type",
            "dmx.core.text");
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
