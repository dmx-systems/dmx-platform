package systems.dmx.core.impl;

import systems.dmx.core.Association;
import systems.dmx.core.AssociationType;
import systems.dmx.core.DMXObject;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.model.RoleModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.service.accesscontrol.AccessControlException;
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

    Topic getTopic(long topicId) {
        try {
            return checkReadAccessAndInstantiate(fetchTopic(topicId));
        } catch (Exception e) {
            throw new RuntimeException("Fetching topic " + topicId + " failed", e);
        }
    }

    TopicImpl getTopicByUri(String uri) {
        try {
            TopicModelImpl topic = fetchTopicByUri(uri);
            return topic != null ? this.<TopicImpl>checkReadAccessAndInstantiate(topic) : null;
            // Note: inside a conditional operator the type witness is required (at least in Java 6)
        } catch (Exception e) {
            throw new RuntimeException("Fetching topic by URI failed (uri=\"" + uri + "\")", e);
        }
    }

    TopicImpl getTopicByValue(String key, SimpleValue value) {
        try {
            TopicModelImpl topic = fetchTopic(key, value);
            return topic != null ? this.<TopicImpl>checkReadAccessAndInstantiate(topic) : null;
            // Note: inside a conditional operator the type witness is required (at least in Java 6)
        } catch (Exception e) {
            throw new RuntimeException("Fetching topic failed (key=\"" + key + "\", value=\"" + value + "\")", e);
        }
    }

    List<Topic> getTopicsByValue(String key, SimpleValue value) {
        try {
            return checkReadAccessAndInstantiate(fetchTopics(key, value));
        } catch (Exception e) {
            throw new RuntimeException("Fetching topics failed (key=\"" + key + "\", value=\"" + value + "\")", e);
        }
    }

    List<Topic> getTopicsByType(String topicTypeUri) {
        try {
            return checkReadAccessAndInstantiate(_getTopicType(topicTypeUri).getAllInstances());
        } catch (Exception e) {
            throw new RuntimeException("Fetching topics by type failed (topicTypeUri=\"" + topicTypeUri + "\")", e);
        }
    }

    List<Topic> searchTopics(String searchTerm, String fieldUri) {
        try {
            return checkReadAccessAndInstantiate(queryTopics(fieldUri, new SimpleValue(searchTerm)));
        } catch (Exception e) {
            throw new RuntimeException("Searching topics failed (searchTerm=\"" + searchTerm + "\", fieldUri=\"" +
                fieldUri + "\")", e);
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

    Association getAssociation(long assocId) {
        try {
            return checkReadAccessAndInstantiate(fetchAssociation(assocId));
        } catch (Exception e) {
            throw new RuntimeException("Fetching association " + assocId + " failed", e);
        }
    }

    Association getAssociationByValue(String key, SimpleValue value) {
        try {
            AssociationModelImpl assoc = fetchAssociation(key, value);
            return assoc != null ? this.<Association>checkReadAccessAndInstantiate(assoc) : null;
            // Note: inside a conditional operator the type witness is required (at least in Java 6)
        } catch (Exception e) {
            throw new RuntimeException("Fetching association failed (key=\"" + key + "\", value=\"" + value + "\")", e);
        }
    }

    List<Association> getAssociationsByValue(String key, SimpleValue value) {
        try {
            return checkReadAccessAndInstantiate(fetchAssociations(key, value));
        } catch (Exception e) {
            throw new RuntimeException("Fetching associationss failed (key=\"" + key + "\", value=\"" + value + "\")",
                e);
        }
    }

    AssociationImpl getAssociation(String assocTypeUri, long topic1Id, long topic2Id, String roleTypeUri1,
                                                                                      String roleTypeUri2) {
        String info = "assocTypeUri=\"" + assocTypeUri + "\", topic1Id=" + topic1Id + ", topic2Id=" + topic2Id +
            ", roleTypeUri1=\"" + roleTypeUri1 + "\", roleTypeUri2=\"" + roleTypeUri2 + "\"";
        try {
            AssociationModelImpl assoc = fetchAssociation(assocTypeUri, topic1Id, topic2Id, roleTypeUri1, roleTypeUri2);
            return assoc != null ? this.<AssociationImpl>checkReadAccessAndInstantiate(assoc) : null;
            // Note: inside a conditional operator the type witness is required (at least in Java 6)
        } catch (Exception e) {
            throw new RuntimeException("Fetching association failed (" + info + ")", e);
        }
    }

    Association getAssociationBetweenTopicAndAssociation(String assocTypeUri, long topicId, long assocId,
                                                         String topicRoleTypeUri, String assocRoleTypeUri) {
        String info = "assocTypeUri=\"" + assocTypeUri + "\", topicId=" + topicId + ", assocId=" + assocId +
            ", topicRoleTypeUri=\"" + topicRoleTypeUri + "\", assocRoleTypeUri=\"" + assocRoleTypeUri + "\"";
        logger.info(info);
        try {
            AssociationModelImpl assoc = fetchAssociationBetweenTopicAndAssociation(assocTypeUri, topicId, assocId,
                topicRoleTypeUri, assocRoleTypeUri);
            return assoc != null ? this.<Association>checkReadAccessAndInstantiate(assoc) : null;
            // Note: inside a conditional operator the type witness is required (at least in Java 6)
        } catch (Exception e) {
            throw new RuntimeException("Fetching association failed (" + info + ")", e);
        }
    }

    // ---

    List<Association> getAssociationsByType(String assocTypeUri) {
        try {
            return checkReadAccessAndInstantiate(_getAssociationType(assocTypeUri).getAllInstances());
        } catch (Exception e) {
            throw new RuntimeException("Fetching associations by type failed (assocTypeUri=\"" + assocTypeUri + "\")",
                e);
        }
    }

    List<Association> getAssociations(long topic1Id, long topic2Id) {
        return getAssociations(null, topic1Id, topic2Id);   // assocTypeUri=null
    }

    List<Association> getAssociations(String assocTypeUri, long topic1Id, long topic2Id) {
        return getAssociations(assocTypeUri, topic1Id, topic2Id, null, null);   // roleTypeUri1=null, roleTypeUri2=null
    }

    List<Association> getAssociations(String assocTypeUri, long topic1Id, long topic2Id, String roleTypeUri1,
                                                                                         String roleTypeUri2) {
        return instantiate(_getAssociations(assocTypeUri, topic1Id, topic2Id, roleTypeUri1, roleTypeUri2));
    }

    /**
     * Fetches from DB and filters READables. No instantiation.
     *
     * ### TODO: drop this. Use the new traversal methods instead.
     */
    Iterable<AssociationModelImpl> _getAssociations(String assocTypeUri, long topic1Id, long topic2Id,
                                                    String roleTypeUri1, String roleTypeUri2) {
        logger.fine("assocTypeUri=\"" + assocTypeUri + "\", topic1Id=" + topic1Id + ", topic2Id=" + topic2Id +
            ", roleTypeUri1=\"" + roleTypeUri1 + "\", roleTypeUri2=\"" + roleTypeUri2 + "\"");
        try {
            return filterReadables(fetchAssociations(assocTypeUri, topic1Id, topic2Id, roleTypeUri1, roleTypeUri2));
        } catch (Exception e) {
            throw new RuntimeException("Fetching associations between topics " + topic1Id + " and " + topic2Id +
                " failed (assocTypeUri=\"" + assocTypeUri + "\", roleTypeUri1=\"" + roleTypeUri1 +
                "\", roleTypeUri2=\"" + roleTypeUri2 + "\")", e);
        }
    }

    // ---

    Iterable<Association> getAllAssociations() {
        return new AssociationIterable(this);
    }

    List<RoleModel> getRoleModels(long assocId) {
        return fetchRoleModels(assocId);
    }

    // ---

    /**
     * Convenience.
     */
    AssociationImpl createAssociation(String typeUri, RoleModel roleModel1, RoleModel roleModel2) {
        return createAssociation(mf.newAssociationModel(typeUri, roleModel1, roleModel2));
    }

    /**
     * Creates a new association in the DB.
     */
    AssociationImpl createAssociation(AssociationModelImpl model) {
        try {
            em.fireEvent(CoreEvent.PRE_CREATE_ASSOCIATION, model);
            //
            model.preCreate();
            //
            // 1) store in DB
            storeAssociation(model);
            AssociationModelImpl _model = updateValues(model, null);
            createAssociationInstantiation(_model.getId(), _model.getTypeUri());
            // 2) instantiate
            AssociationImpl assoc = _model.instantiate();
            //
            // Note 1: the postCreate() hook is invoked on the update model, *not* on the value integration result
            // (_model). Otherwise the programmatic vs. interactive detection would not work (see postCreate() comment
            // at AssociationDefinitionModelImpl). "model" might be an AssociationDefinitionModel while "_model" is
            // always an AssociationModel.
            // Note 2: postCreate() creates and caches the assoc def based on "model". Cached assoc defs need an
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

    void updateAssociation(AssociationModelImpl updateModel) {
        try {
            AssociationModelImpl model = fetchAssociation(updateModel.getId());
            updateAssociation(model, updateModel);
            //
            // Note: there is no possible POST_UPDATE_ASSOCIATION_REQUEST event to fire here (compare to updateTopic()).
            // It would be equivalent to POST_UPDATE_ASSOCIATION. Per request exactly one association is updated.
            // Its childs are always topics (never associations).
        } catch (Exception e) {
            throw new RuntimeException("Fetching and updating association " + updateModel.getId() + " failed", e);
        }
    }

    void updateAssociation(AssociationModelImpl assoc, AssociationModelImpl updateModel) {
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
    void deleteAssociation(long assocId) {
        try {
            deleteAssociation(fetchAssociation(assocId));
        } catch (IllegalStateException e) {
            // Note: fetchAssociation() might throw IllegalStateException and is no problem.
            // This happens when the association is deleted already. In this case nothing needs to be performed.
            //
            // Compare to DMXObjectModelImpl.delete()
            // TODO: introduce storage-vendor neutral DM exception.
            if (e.getMessage().equals("Node[" + assocId + "] has been deleted in this tx")) {
                logger.info("### Association " + assocId + " has already been deleted in this transaction. " +
                    "This can happen while delete-multi.");
            } else {
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("Fetching and deleting association " + assocId + " failed", e);
        }
    }

    void deleteAssociation(AssociationModelImpl assoc) {
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
            AssociationModelImpl assoc = mf.newAssociationModel("dmx.core.instantiation",
                mf.newTopicRoleModel(topicTypeUri, "dmx.core.type"),
                mf.newTopicRoleModel(topicId, "dmx.core.instance"));
            storeAssociation(assoc);   // direct storage calls used here ### explain
            storeAssociationValue(assoc.id, assoc.value, assoc.typeUri, false);     // isHtml=false
            createAssociationInstantiation(assoc.id, assoc.typeUri);
        } catch (Exception e) {
            throw new RuntimeException("Associating topic " + topicId + " with topic type \"" +
                topicTypeUri + "\" failed", e);
        }
    }

    void createAssociationInstantiation(long assocId, String assocTypeUri) {
        try {
            AssociationModelImpl assoc = mf.newAssociationModel("dmx.core.instantiation",
                mf.newTopicRoleModel(assocTypeUri, "dmx.core.type"),
                mf.newAssociationRoleModel(assocId, "dmx.core.instance"));
            storeAssociation(assoc);   // direct storage calls used here ### explain
            storeAssociationValue(assoc.id, assoc.value, assoc.typeUri, false);     // isHtml=false
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

    AssociationTypeImpl getAssociationType(String uri) {
        return checkReadAccessAndInstantiate(_getAssociationType(uri));
    }

    AssociationTypeImpl getAssociationTypeImplicitly(long assocId) {
        checkAssociationReadAccess(assocId);
        return _getAssociationType(typeUri(assocId)).instantiate();
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

    List<AssociationType> getAllAssociationTypes() {
        try {
            List<AssociationType> assocTypes = new ArrayList();
            for (String uri : getAssociationTypeUris()) {
                assocTypes.add(_getAssociationType(uri).instantiate());
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

    AssociationTypeImpl createAssociationType(AssociationTypeModelImpl model) {
        try {
            em.fireEvent(CoreEvent.PRE_CREATE_ASSOCIATION_TYPE, model);
            //
            // store in DB
            createType(model, URI_PREFIX_ASSOCIATION_TYPE);
            //
            AssociationTypeImpl assocType = model.instantiate();
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

    void updateAssociationType(AssociationTypeModelImpl updateModel) {
        try {
            // Note: type lookup is by ID. The URI might have changed, the ID does not.
            TopicModelImpl topic = fetchTopic(updateModel.getId());
            topic.checkWriteAccess();
            _getAssociationType(topic.getUri()).update(updateModel);
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

    void deleteAssociationType(String assocTypeUri) {
        try {
            TypeModelImpl type = _getAssociationType(assocTypeUri);
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
    AssociationTypeModelImpl _getAssociationType(String uri) {
        return typeStorage.getAssociationType(uri);
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

    RelatedAssociationModelImpl getTopicRelatedAssociation(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                           String othersRoleTypeUri, String othersAssocTypeUri) {
        RelatedAssociationModelImpl assoc = fetchTopicRelatedAssociation(topicId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersAssocTypeUri);
        return assoc != null ? checkReadAccess(assoc) : null;
    }

    List<RelatedAssociationModelImpl> getTopicRelatedAssociations(long topicId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        return filterReadables(fetchTopicRelatedAssociations(topicId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersAssocTypeUri));
    }

    List<AssociationModelImpl> getTopicAssociations(long topicId) {
        return filterReadables(fetchTopicAssociations(topicId));
    }

    // --- Association Source ---

    List<RelatedTopicModelImpl> getAssociationRelatedTopics(long assocId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersTopicTypeUri) {
        return filterReadables(fetchAssociationRelatedTopics(assocId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri));
    }

    RelatedAssociationModelImpl getAssociationRelatedAssociation(long assocId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        RelatedAssociationModelImpl assoc = fetchAssociationRelatedAssociation(assocId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersAssocTypeUri);
        return assoc != null ? checkReadAccess(assoc) : null;
    }

    List<RelatedAssociationModelImpl> getAssociationRelatedAssociations(long assocId, String assocTypeUri,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersAssocTypeUri) {
        return filterReadables(fetchAssociationRelatedAssociations(assocId, assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersAssocTypeUri));
    }

    List<AssociationModelImpl> getAssociationAssociations(long assocId) {
        return filterReadables(fetchAssociationAssociations(assocId));
    }

    // --- Object Source ---

    RelatedTopicModelImpl getRelatedTopic(long objectId, String assocTypeUri, String myRoleTypeUri,
                                          String othersRoleTypeUri, String othersTopicTypeUri) {
        RelatedTopicModelImpl topic = fetchRelatedTopic(objectId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri);
        return topic != null ? checkReadAccess(topic) : null;
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

    List<Association> getAssociationsByProperty(String propUri, Object propValue) {
        return checkReadAccessAndInstantiate(fetchAssociationsByProperty(propUri, propValue));
    }

    List<Association> getAssociationsByPropertyRange(String propUri, Number from, Number to) {
        return checkReadAccessAndInstantiate(fetchAssociationsByPropertyRange(propUri, from, to));
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Access Control / Instantiation ===

    // These methods 1) instantiate objects from models, and 2) check the READ permission for each model.
    // Call these methods when passing objects fetched from the DB to the user.

    // ### TODO: drop this. No instatiations in this class.
    <O> O checkReadAccessAndInstantiate(DMXObjectModelImpl model) {
        return (O) checkReadAccess(model).instantiate();
    }

    // ### TODO: drop this. No instatiations in this class.
    <O> List<O> checkReadAccessAndInstantiate(List<? extends DMXObjectModelImpl> models) {
        return instantiate(filterReadables(models));
    }

    // ---

    private <M extends DMXObjectModelImpl> List<M> filterReadables(List<M> models) {
        Iterator<? extends DMXObjectModelImpl> i = models.iterator();
        while (i.hasNext()) {
            if (!hasReadAccess(i.next())) {
                i.remove();
            }
        }
        return models;
    }

    boolean hasReadAccess(DMXObjectModelImpl model) {
        try {
            checkReadAccess(model);
            return true;
        } catch (AccessControlException e) {
            return false;
        }
    }

    // ---

    // TODO: add return to model's checkReadAccess() and drop this method?
    <M extends DMXObjectModelImpl> M checkReadAccess(M model) {
        model.checkReadAccess();
        return model;
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
    void checkAssociationReadAccess(long assocId) {
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
    void checkAssociationWriteAccess(long assocId) {
        em.fireEvent(CoreEvent.CHECK_ASSOCIATION_WRITE_ACCESS, assocId);
    }



    // === Instantiation ===

    // ### TODO: move to kernel utils
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

    private List<String> getAssociationTypeUris() {
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
        // assoc defs not readable by the current user. But at the time the type topic is stored in the DB its assoc
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
