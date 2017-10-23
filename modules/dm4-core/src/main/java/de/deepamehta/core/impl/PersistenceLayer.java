package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.service.accesscontrol.AccessControlException;
import de.deepamehta.core.storage.spi.DeepaMehtaStorage;

import java.util.ArrayList;
import java.util.Collection;
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
    ValueStorage valueStorage;

    EventManager em;
    ModelFactoryImpl mf;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public PersistenceLayer(DeepaMehtaStorage storage) {
        super(storage);
        // Note: mf must be initialzed before the type storage is instantiated
        this.em = new EventManager();
        this.mf = (ModelFactoryImpl) storage.getModelFactory();
        //
        this.typeStorage = new TypeStorage(this);
        this.valueStorage = new ValueStorage(this);
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
        return getTopicByValue("uri", new SimpleValue(uri));
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

    /**
     * Convenience.
     */
    TopicImpl createTopic(TopicModelImpl model) {
        return createTopic(model, null);    // uriPrefix=null
    }

    /**
     * Creates a new topic in the DB.
     */
    TopicImpl createTopic(TopicModelImpl model, String uriPrefix) {
        try {
            em.fireEvent(CoreEvent.PRE_CREATE_TOPIC, model);
            //
            model.preCreate();
            //
            // 1) store in DB
            storeTopic(model);
            valueStorage.storeValue(model);
            createTopicInstantiation(model.getId(), model.getTypeUri());
            // 2) set default URI
            // If no URI is given the topic gets a default URI based on its ID, if requested.
            // Note: this must be done *after* the topic is stored. The ID is not known before.
            // Note: in case no URI was given: once stored a topic's URI is empty (not null).
            if (uriPrefix != null && model.getUri().equals("")) {
                model.updateUri(uriPrefix + model.getId());     // update memory + DB
            }
            // 3) instantiate
            TopicImpl topic = model.instantiate();
            //
            model.postCreate();
            //
            em.fireEvent(CoreEvent.POST_CREATE_TOPIC, topic);
            return topic;
        } catch (Exception e) {
            throw new RuntimeException("Creating topic " + model.getId() + " failed (typeUri=\"" + model.getTypeUri() +
                "\")", e);
        }
    }

    // ---

    void updateTopic(TopicModelImpl updateModel) {
        try {
            TopicModelImpl model = fetchTopic(updateModel.getId());
            updateTopic(model, updateModel);
            //
            // Note: POST_UPDATE_TOPIC_REQUEST is fired only once per update request.
            // On the other hand TopicModel's update() method is called multiple times while updating the child topics
            // (see ChildTopicsModelImpl).
            em.fireEvent(CoreEvent.POST_UPDATE_TOPIC_REQUEST, model.instantiate());
        } catch (Exception e) {
            throw new RuntimeException("Updating topic " + updateModel.getId() + " failed", e);
        }
    }

    void updateTopic(TopicModelImpl topic, TopicModelImpl updateModel) {
        try {
            checkTopicWriteAccess(topic.getId());
            topic.update(updateModel);
        } catch (Exception e) {
            throw new RuntimeException("Updating topic " + topic.getId() + " failed", e);
        }
    }

    // ---

    void deleteTopic(long topicId) {
        deleteTopic(fetchTopic(topicId));
    }

    void deleteTopic(TopicModelImpl topic) {
        try {
            checkTopicWriteAccess(topic.getId());
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

    Association getAssociation(String assocTypeUri, long topic1Id, long topic2Id, String roleTypeUri1,
                                                                                  String roleTypeUri2) {
        String info = "assocTypeUri=\"" + assocTypeUri + "\", topic1Id=" + topic1Id + ", topic2Id=" + topic2Id +
            ", roleTypeUri1=\"" + roleTypeUri1 + "\", roleTypeUri2=\"" + roleTypeUri2 + "\"";
        try {
            AssociationModelImpl assoc = fetchAssociation(assocTypeUri, topic1Id, topic2Id, roleTypeUri1, roleTypeUri2);
            return assoc != null ? this.<Association>checkReadAccessAndInstantiate(assoc) : null;
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

    long[] getPlayerIds(long assocId) {
        return fetchPlayerIds(assocId);
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
            valueStorage.storeValue(model);
            createAssociationInstantiation(model.getId(), model.getTypeUri());
            // 2) instantiate
            AssociationImpl assoc = model.instantiate();
            //
            model.postCreate();
            //
            em.fireEvent(CoreEvent.POST_CREATE_ASSOCIATION, assoc);
            return assoc;
        } catch (Exception e) {
            throw new RuntimeException("Creating association failed (" + model + ")", e);
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
            throw new RuntimeException("Updating association " + updateModel.getId() + " failed", e);
        }
    }

    void updateAssociation(AssociationModelImpl assoc, AssociationModelImpl updateModel) {
        try {
            checkAssociationWriteAccess(assoc.getId());
            assoc.update(updateModel);
        } catch (Exception e) {
            throw new RuntimeException("Updating association " + assoc.getId() + " failed", e);
        }
    }

    // ---

    void deleteAssociation(long assocId) {
        deleteAssociation(fetchAssociation(assocId));
    }

    void deleteAssociation(AssociationModelImpl assoc) {
        try {
            checkAssociationWriteAccess(assoc.getId());
            assoc.delete();
        } catch (Exception e) {
            throw new RuntimeException("Deleting association " + assoc.getId() + " failed", e);
        }
    }



    // ===

    void createTopicInstantiation(long topicId, String topicTypeUri) {
        try {
            AssociationModel assoc = mf.newAssociationModel("dm4.core.instantiation",
                mf.newTopicRoleModel(topicTypeUri, "dm4.core.type"),
                mf.newTopicRoleModel(topicId, "dm4.core.instance"));
            storeAssociation(assoc);   // direct storage calls used here ### explain
            storeAssociationValue(assoc.getId(), assoc.getSimpleValue());
            createAssociationInstantiation(assoc.getId(), assoc.getTypeUri());
        } catch (Exception e) {
            throw new RuntimeException("Associating topic " + topicId +
                " with topic type \"" + topicTypeUri + "\" failed", e);
        }
    }

    void createAssociationInstantiation(long assocId, String assocTypeUri) {
        try {
            AssociationModel assoc = mf.newAssociationModel("dm4.core.instantiation",
                mf.newTopicRoleModel(assocTypeUri, "dm4.core.type"),
                mf.newAssociationRoleModel(assocId, "dm4.core.instance"));
            storeAssociation(assoc);   // direct storage calls used here ### explain
            storeAssociationValue(assoc.getId(), assoc.getSimpleValue());
        } catch (Exception e) {
            throw new RuntimeException("Associating association " + assocId +
                " with association type \"" + assocTypeUri + "\" failed", e);
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
            // ### FIXME: access control
            String topicTypeUri = fetchTopic(updateModel.getId()).getUri();
            _getTopicType(topicTypeUri).update(updateModel);
        } catch (Exception e) {
            throw new RuntimeException("Updating topic type failed (" + updateModel + ")", e);
        }
    }

    void updateAssociationType(AssociationTypeModelImpl updateModel) {
        try {
            // Note: type lookup is by ID. The URI might have changed, the ID does not.
            // ### FIXME: access control
            String assocTypeUri = fetchTopic(updateModel.getId()).getUri();
            _getAssociationType(assocTypeUri).update(updateModel);
        } catch (Exception e) {
            throw new RuntimeException("Updating association type failed (" + updateModel + ")", e);
        }
    }

    // ---

    void deleteTopicType(String topicTypeUri) {
        try {
            _getTopicType(topicTypeUri).delete();           // ### TODO: delete view config topics
        } catch (Exception e) {
            throw new RuntimeException("Deleting topic type \"" + topicTypeUri + "\" failed", e);
        }
    }

    void deleteAssociationType(String assocTypeUri) {
        try {
            _getAssociationType(assocTypeUri).delete();     // ### TODO: delete view config topics
        } catch (Exception e) {
            throw new RuntimeException("Deleting association type \"" + assocTypeUri + "\" failed", e);
        }
    }

    // ---

    Topic createRoleType(TopicModelImpl model) {
        // check type URI argument
        String typeUri = model.getTypeUri();
        if (typeUri == null) {
            model.setTypeUri("dm4.core.role_type");
        } else {
            if (!typeUri.equals("dm4.core.role_type")) {
                throw new IllegalArgumentException("A role type is supposed to be of type \"dm4.core.role_type\" " +
                    "(found: \"" + typeUri + "\")");
            }
        }
        //
        return createTopic(model, URI_PREFIX_ROLE_TYPE);
    }

    // ---

    TopicTypeModelImpl _getTopicType(String uri) {
        return typeStorage.getTopicType(uri);
    }

    AssociationTypeModelImpl _getAssociationType(String uri) {
        return typeStorage.getAssociationType(uri);
    }



    // === Generic Object ===

    DeepaMehtaObject getObject(long id) {
        return checkReadAccessAndInstantiate(fetchObject(id));
    }



    // === Traversal ===

    // --- Topic Source ---

    List<RelatedTopicModelImpl> getTopicRelatedTopics(long topicId, String assocTypeUri, String myRoleTypeUri,
                                                      String othersRoleTypeUri, String othersTopicTypeUri) {
        return filterReadables(fetchTopicRelatedTopics(topicId, assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri));
    }

    List<RelatedTopicModelImpl> getTopicRelatedTopics(long topicId, List<String> assocTypeUris, String myRoleTypeUri,
                                                      String othersRoleTypeUri, String othersTopicTypeUri) {
        return filterReadables(fetchTopicRelatedTopics(topicId, assocTypeUris, myRoleTypeUri, othersRoleTypeUri,
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

    List<RelatedTopicModelImpl> getAssociationRelatedTopics(long assocId, List<String> assocTypeUris,
                                            String myRoleTypeUri, String othersRoleTypeUri, String othersTopicTypeUri) {
        return filterReadables(fetchAssociationRelatedTopics(assocId, assocTypeUris, myRoleTypeUri, othersRoleTypeUri,
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
    <O> O checkReadAccessAndInstantiate(DeepaMehtaObjectModelImpl model) {
        return (O) checkReadAccess(model).instantiate();
    }

    // ### TODO: drop this. No instatiations in this class.
    <O> List<O> checkReadAccessAndInstantiate(List<? extends DeepaMehtaObjectModelImpl> models) {
        return instantiate(filterReadables(models));
    }

    // ---

    private <M extends DeepaMehtaObjectModelImpl> List<M> filterReadables(List<M> models) {
        Iterator<? extends DeepaMehtaObjectModelImpl> i = models.iterator();
        while (i.hasNext()) {
            if (!hasReadAccess(i.next())) {
                i.remove();
            }
        }
        return models;
    }

    boolean hasReadAccess(DeepaMehtaObjectModelImpl model) {
        try {
            checkReadAccess(model);
            return true;
        } catch (AccessControlException e) {
            return false;
        }
    }

    // ---

    <M extends DeepaMehtaObjectModelImpl> M checkReadAccess(M model) {
        model.checkReadAccess();
        return model;
    }

    // ---

    void checkTopicReadAccess(long topicId) {
        em.fireEvent(CoreEvent.CHECK_TOPIC_READ_ACCESS, topicId);
    }

    void checkAssociationReadAccess(long assocId) {
        em.fireEvent(CoreEvent.CHECK_ASSOCIATION_READ_ACCESS, assocId);
    }

    // ---

    private void checkTopicWriteAccess(long topicId) {
        em.fireEvent(CoreEvent.CHECK_TOPIC_WRITE_ACCESS, topicId);
    }

    private void checkAssociationWriteAccess(long assocId) {
        em.fireEvent(CoreEvent.CHECK_ASSOCIATION_WRITE_ACCESS, assocId);
    }



    // === Instantiation ===

    // ### TODO: move to kernel utils
    <O> List<O> instantiate(Iterable<? extends DeepaMehtaObjectModelImpl> models) {
        List<O> objects = new ArrayList();
        for (DeepaMehtaObjectModelImpl model : models) {
            objects.add((O) model.instantiate());
        }
        return objects;
    }



    // ===

    private List<String> getTopicTypeUris() {
        try {
            List<String> topicTypeUris = new ArrayList();
            // add meta types
            topicTypeUris.add("dm4.core.topic_type");
            topicTypeUris.add("dm4.core.assoc_type");
            topicTypeUris.add("dm4.core.meta_type");
            // add regular types
            for (TopicModel topicType : filterReadables(fetchTopics("typeUri", new SimpleValue(
                                                                    "dm4.core.topic_type")))) {
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
                                                                    "dm4.core.assoc_type")))) {
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
        createTopic(typeTopic, uriPrefix);      // create generic topic
        //
        model.id = typeTopic.id;
        model.uri = typeTopic.uri;
        //
        typeStorage.storeType(model);           // store type-specific parts
    }

    private String typeUri(long objectId) {
        return (String) fetchProperty(objectId, "typeUri");
    }

    private void bootstrapTypeCache() {
        TopicTypeModelImpl metaMetaType = mf.newTopicTypeModel("dm4.core.meta_meta_type", "Meta Meta Type",
            "dm4.core.text");
        metaMetaType.setTypeUri("dm4.core.meta_meta_meta_type");
        typeStorage.putInTypeCache(metaMetaType);
    }
}
