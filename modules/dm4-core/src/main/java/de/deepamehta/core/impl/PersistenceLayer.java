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
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.service.accesscontrol.AccessControlException;
import de.deepamehta.core.storage.spi.DeepaMehtaStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;



public class PersistenceLayer extends StorageDecorator {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String URI_PREFIX_TOPIC_TYPE       = "domain.project.topic_type_";
    private static final String URI_PREFIX_ASSOCIATION_TYPE = "domain.project.assoc_type_";
    private static final String URI_PREFIX_ROLE_TYPE        = "domain.project.role_type_";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    TypeStorageImpl typeStorage;
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
        this.typeStorage = new TypeStorageImpl(this);
        this.valueStorage = new ValueStorage(this);
        //
        // Note: this is a constructor side effect. This is a cyclic dependency. This is very nasty.
        // ### TODO: explain why we do it.
        mf.pl = this;
        //
        bootstrapTypeCache();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === Topics ===

    Topic getTopic(long topicId) {
        try {
            return instantiateTopic(fetchTopic(topicId));
        } catch (Exception e) {
            throw new RuntimeException("Fetching topic " + topicId + " failed", e);
        }
    }

    Topic getTopic(String key, SimpleValue value) {
        try {
            TopicModel topic = fetchTopic(key, value);
            return topic != null ? instantiateTopic(topic) : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching topic failed (key=\"" + key + "\", value=\"" + value + "\")", e);
        }
    }

    List<Topic> getTopics(String key, SimpleValue value) {
        try {
            return instantiateTopics(fetchTopics(key, value));
        } catch (Exception e) {
            throw new RuntimeException("Fetching topics failed (key=\"" + key + "\", value=\"" + value + "\")", e);
        }
    }

    List<Topic> getTopics(String topicTypeUri) {
        try {
            return instantiateTopics(typeStorage.getTopicType(topicTypeUri).getAllInstances());
        } catch (Exception e) {
            throw new RuntimeException("Fetching topics by type failed (topicTypeUri=\"" + topicTypeUri + "\")", e);
        }
    }

    List<Topic> searchTopics(String searchTerm, String fieldUri) {
        try {
            return instantiateTopics(queryTopics(fieldUri, new SimpleValue(searchTerm)));
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
    Topic createTopic(TopicModel model) {
        return createTopic(model, null);    // uriPrefix=null
    }

    /**
     * Creates a new topic in the DB.
     */
    Topic createTopic(TopicModel model, String uriPrefix) {
        try {
            em.fireEvent(CoreEvent.PRE_CREATE_TOPIC, model);
            //
            // 1) store in DB
            storeTopic(model);
            valueStorage.storeValue((TopicModelImpl) model);
            createTopicInstantiation(model.getId(), model.getTypeUri());
            // 2) set default URI
            // If no URI is given the topic gets a default URI based on its ID, if requested.
            // Note: this must be done *after* the topic is stored. The ID is not known before.
            // Note: in case no URI was given: once stored a topic's URI is empty (not null).
            if (uriPrefix != null && model.getUri().equals("")) {
                ((TopicModelImpl) model).updateUri(uriPrefix + model.getId());
            }
            // 3) instantiate
            Topic topic = new TopicImpl((TopicModelImpl) model, this);
            //
            em.fireEvent(CoreEvent.POST_CREATE_TOPIC, topic);
            return topic;
        } catch (Exception e) {
            throw new RuntimeException("Creating topic " + model.getId() + " failed (typeUri=\"" + model.getTypeUri() +
                "\")", e);
        }
    }

    // ---

    void updateTopic(TopicModel newModel) {
        try {
            TopicModelImpl model = fetchTopic(newModel.getId());
            model.update(newModel);
            //
            // Note: POST_UPDATE_TOPIC_REQUEST is fired only once per update request.
            // On the other hand TopicModel's update() method is called multiple times while updating the child topics
            // (see ChildTopicsModelImpl).
            em.fireEvent(CoreEvent.POST_UPDATE_TOPIC_REQUEST, model.instantiate());
        } catch (Exception e) {
            throw new RuntimeException("Updating topic " + newModel.getId() + " failed", e);
        }
    }

    void deleteTopic(long topicId) {
        fetchTopic(topicId).delete();
    }



    // === Associations ===

    Association getAssociation(long assocId) {
        try {
            return instantiateAssociation(fetchAssociation(assocId));
        } catch (Exception e) {
            throw new RuntimeException("Fetching association " + assocId + " failed", e);
        }
    }

    Association getAssociation(String key, SimpleValue value) {
        try {
            AssociationModelImpl assoc = fetchAssociation(key, value);
            return assoc != null ? instantiateAssociation(assoc) : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching association failed (key=\"" + key + "\", value=\"" + value + "\")", e);
        }
    }

    List<Association> getAssociations(String key, SimpleValue value) {
        try {
            return instantiateAssociations(fetchAssociations(key, value));
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
            return assoc != null ? instantiateAssociation(assoc) : null;
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
            return assoc != null ? instantiateAssociation(assoc) : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching association failed (" + info + ")", e);
        }
    }

    // ---

    List<Association> getAssociations(String assocTypeUri) {
        try {
            return instantiateAssociations(typeStorage.getAssociationType(assocTypeUri).getAllInstances());
        } catch (Exception e) {
            throw new RuntimeException("Fetching associations by type failed (assocTypeUri=\"" + assocTypeUri + "\")",
                e);
        }
    }

    List<Association> getAssociations(long topic1Id, long topic2Id) {
        return getAssociations(topic1Id, topic2Id, null);
    }

    List<Association> getAssociations(long topic1Id, long topic2Id, String assocTypeUri) {
        logger.info("topic1Id=" + topic1Id + ", topic2Id=" + topic2Id + ", assocTypeUri=\"" + assocTypeUri + "\"");
        try {
            return instantiateAssociations(fetchAssociations(assocTypeUri, topic1Id, topic2Id, null, null));
                                                                    // roleTypeUri1=null, roleTypeUri2=null
        } catch (Exception e) {
            throw new RuntimeException("Fetching associations between topics " + topic1Id + " and " + topic2Id +
                " failed (assocTypeUri=\"" + assocTypeUri + "\")", e);
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
    Association createAssociation(String typeUri, RoleModel roleModel1, RoleModel roleModel2) {
        return createAssociation(mf.newAssociationModel(typeUri, roleModel1, roleModel2));
    }

    /**
     * Creates a new association in the DB.
     */
    Association createAssociation(AssociationModelImpl model) {
        try {
            em.fireEvent(CoreEvent.PRE_CREATE_ASSOCIATION, model);
            //
            // 1) store in DB
            storeAssociation(model);
            valueStorage.storeValue(model);
            createAssociationInstantiation(model.getId(), model.getTypeUri());
            // 2) instantiate
            Association assoc = new AssociationImpl(model, this);
            //
            em.fireEvent(CoreEvent.POST_CREATE_ASSOCIATION, assoc);
            return assoc;
        } catch (Exception e) {
            throw new RuntimeException("Creating association failed (" + model + ")", e);
        }
    }

    // ---

    void updateAssociation(AssociationModel newModel) {
        try {
            AssociationModelImpl model = fetchAssociation(newModel.getId());
            model.update(newModel);
            //
            // Note: there is no possible POST_UPDATE_ASSOCIATION_REQUEST event to fire here (compare to updateTopic()).
            // It would be equivalent to POST_UPDATE_ASSOCIATION. Per request exactly one association is updated.
            // Its childs are always topics (never associations).
        } catch (Exception e) {
            throw new RuntimeException("Updating association " + newModel.getId() + " failed", e);
        }
    }

    void deleteAssociation(long assocId) {
        fetchAssociation(assocId).delete();
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

    TopicType getTopicType(String uri) {
        return typeStorage.getTopicType(uri).instantiate();
    }

    AssociationType getAssociationType(String uri) {
        return typeStorage.getAssociationType(uri).instantiate();
    }

    // ---

    TopicType createTopicType(TopicTypeModelImpl model) {
        try {
            // store in DB
            createTopic(model, URI_PREFIX_TOPIC_TYPE);          // create generic topic
            typeStorage.storeType(model);                       // store type-specific parts
            //
            TopicType topicType = model.instantiate();
            em.fireEvent(CoreEvent.INTRODUCE_TOPIC_TYPE, topicType);
            //
            return topicType;
        } catch (Exception e) {
            throw new RuntimeException("Creating topic type \"" + model.getUri() + "\" failed (" + model + ")", e);
        }
    }

    AssociationType createAssociationType(AssociationTypeModelImpl model) {
        try {
            // store in DB
            createTopic(model, URI_PREFIX_ASSOCIATION_TYPE);    // create generic topic
            typeStorage.storeType(model);                       // store type-specific parts
            //
            AssociationType assocType = model.instantiate();
            em.fireEvent(CoreEvent.INTRODUCE_ASSOCIATION_TYPE, assocType);
            //
            return assocType;
        } catch (Exception e) {
            throw new RuntimeException("Creating association type \"" + model.getUri() + "\" failed (" + model + ")",
                e);
        }
    }

    // ---

    Topic createRoleType(TopicModel model) {
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



    // === Generic Object ===

    DeepaMehtaObject getObject(long id) {
        DeepaMehtaObjectModelImpl model = fetchObject(id);
        checkReadAccess(model);
        return model.instantiate();
    }



    // === Properties ===

    List<Topic> getTopicsByProperty(String propUri, Object propValue) {
        return instantiateTopics(fetchTopicsByProperty(propUri, propValue));
    }

    List<Topic> getTopicsByPropertyRange(String propUri, Number from, Number to) {
        return instantiateTopics(fetchTopicsByPropertyRange(propUri, from, to));
    }

    List<Association> getAssociationsByProperty(String propUri, Object propValue) {
        return instantiateAssociations(fetchAssociationsByProperty(propUri, propValue));
    }

    List<Association> getAssociationsByPropertyRange(String propUri, Number from, Number to) {
        return instantiateAssociations(fetchAssociationsByPropertyRange(propUri, from, to));
    }



    // === Instantiation ===

    // These methods 1) instantiate objects from models, and 2) check the READ permission for each model.
    // Call these methods when passing objects fetched from the DB to the user.
    // ### TODO: make these private?

    Topic instantiateTopic(TopicModel model) {
        checkReadAccess(model);
        return new TopicImpl((TopicModelImpl) model, this);
    }

    List<Topic> instantiateTopics(List<TopicModel> models) {
        List<Topic> topics = new ArrayList();
        for (TopicModel model : models) {
            try {
                topics.add(instantiateTopic(model));
            } catch (AccessControlException e) {
                // don't add to result and continue
            }
        }
        return topics;
    }

    // ---

    RelatedTopic instantiateRelatedTopic(RelatedTopicModel model) {
        checkReadAccess(model);
        return new RelatedTopicImpl((RelatedTopicModelImpl) model, this);
    }

    ResultList<RelatedTopic> instantiateRelatedTopics(ResultList<RelatedTopicModel> models) {
        List<RelatedTopic> relTopics = new ArrayList();
        for (RelatedTopicModel model : models) {
            try {
                relTopics.add(instantiateRelatedTopic(model));
            } catch (AccessControlException e) {
                // don't add to result and continue
            }
        }
        return new ResultList<RelatedTopic>(relTopics);
    }

    // ---

    Association instantiateAssociation(AssociationModelImpl model) {
        checkReadAccess(model);
        return new AssociationImpl(model, this);
    }

    List<Association> instantiateAssociations(List<AssociationModel> models) {
        List<Association> assocs = new ArrayList();
        for (AssociationModel model : models) {
            try {
                assocs.add(instantiateAssociation((AssociationModelImpl) model));
            } catch (AccessControlException e) {
                // don't add to result and continue
            }
        }
        return assocs;
    }

    // ---

    RelatedAssociation instantiateRelatedAssociation(RelatedAssociationModel model) {
        checkReadAccess(model);
        return new RelatedAssociationImpl((RelatedAssociationModelImpl) model, this);
    }

    ResultList<RelatedAssociation> instantiateRelatedAssociations(Iterable<RelatedAssociationModel> models) {
        ResultList<RelatedAssociation> relAssocs = new ResultList();
        for (RelatedAssociationModel model : models) {
            try {
                relAssocs.add(instantiateRelatedAssociation(model));
            } catch (AccessControlException e) {
                // don't add to result and continue
            }
        }
        return relAssocs;
    }



    // === Access Control ===

    /**
     * @throws  AccessControlException
     */
    private void checkReadAccess(DeepaMehtaObjectModel model) {
        em.fireEvent(((DeepaMehtaObjectModelImpl) model).getPreGetEvent(), model.getId());
    }



    // ===

    private void bootstrapTypeCache() {
        TopicTypeModelImpl metaMetaType = mf.newTopicTypeModel("dm4.core.meta_meta_type", "Meta Meta Type",
            "dm4.core.text");
        metaMetaType.setTypeUri("dm4.core.meta_meta_meta_type");
        typeStorage.putInTypeCache(metaMetaType);
    }
}
