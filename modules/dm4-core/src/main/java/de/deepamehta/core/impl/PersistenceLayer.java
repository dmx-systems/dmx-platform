package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.service.Directives;
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
    TypeCache typeCache;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public PersistenceLayer(DeepaMehtaStorage storage) {
        super(storage);
        // Note: mf must be initialzed before the type storage is instantiated
        this.em = new EventManager();
        this.mf = (ModelFactoryImpl) storage.getModelFactory();
        this.typeCache = new TypeCache(this);
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

    ResultList<RelatedTopic> getTopics(String topicTypeUri) {
        try {
            return getTopicType(topicTypeUri).getRelatedTopics("dm4.core.instantiation", "dm4.core.type",
                "dm4.core.instance", topicTypeUri);
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

    Topic createTopic(TopicModel model, String uriPrefix) {
        try {
            em.fireEvent(CoreEvent.PRE_CREATE_TOPIC, model);
            _createTopic((TopicModelImpl) model, uriPrefix);
            Topic topic = new TopicImpl(model, this);
            em.fireEvent(CoreEvent.POST_CREATE_TOPIC, topic);
            return topic;
        } catch (Exception e) {
            throw new RuntimeException("Creating topic " + model.getId() + " failed (typeUri=\"" + model.getTypeUri() +
                "\")", e);
        }
    }

    // ---

    void updateTopic(TopicModel model) {
        try {
            getTopic(model.getId()).update(model);
        } catch (Exception e) {
            throw new RuntimeException("Updating topic " + model.getId() + " failed (typeUri=\"" + model.getTypeUri() +
                "\")", e);
        }
    }

    void deleteTopic(long topicId) {
        deleteObject(fetchTopic(topicId));
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
            AssociationModel assoc = fetchAssociation(key, value);
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

    Association getAssociation(String assocTypeUri, long topic1Id, long topic2Id,
                                                           String roleTypeUri1, String roleTypeUri2) {
        String info = "assocTypeUri=\"" + assocTypeUri + "\", topic1Id=" + topic1Id + ", topic2Id=" + topic2Id +
            ", roleTypeUri1=\"" + roleTypeUri1 + "\", roleTypeUri2=\"" + roleTypeUri2 + "\"";
        try {
            AssociationModel assoc = fetchAssociation(assocTypeUri, topic1Id, topic2Id, roleTypeUri1, roleTypeUri2);
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
            AssociationModel assoc = fetchAssociationBetweenTopicAndAssociation(assocTypeUri, topicId, assocId,
                topicRoleTypeUri, assocRoleTypeUri);
            return assoc != null ? instantiateAssociation(assoc) : null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching association failed (" + info + ")", e);
        }
    }

    // ---

    ResultList<RelatedAssociation> getAssociations(String assocTypeUri) {
        try {
            return getAssociationType(assocTypeUri).getRelatedAssociations("dm4.core.instantiation",
                "dm4.core.type", "dm4.core.instance", assocTypeUri);
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

    Association createAssociation(AssociationModel model) {
        try {
            em.fireEvent(CoreEvent.PRE_CREATE_ASSOCIATION, model);
            _createAssociation(model);
            Association assoc = new AssociationImpl(model, this);
            em.fireEvent(CoreEvent.POST_CREATE_ASSOCIATION, assoc);
            return assoc;
        } catch (Exception e) {
            throw new RuntimeException("Creating association failed (" + model + ")", e);
        }
    }

    // ---

    void updateAssociation(AssociationModel model) {
        try {
            getAssociation(model.getId()).update(model);
        } catch (Exception e) {
            throw new RuntimeException("Updating association " + model.getId() + " failed (typeUri=\"" +
                model.getTypeUri() + "\")", e);
        }
    }

    void deleteAssociation(long assocId) {
        deleteObject(fetchAssociation(assocId));
    }



    // ===

    /**
     * Creates a new topic in the DB.
     */
    private void _createTopic(TopicModelImpl model, String uriPrefix) {
        // 1) store in DB
        storeTopic(model);
        valueStorage.storeValue(model);
        createTopicInstantiation(model.getId(), model.getTypeUri());
        //
        // 2) set default URI
        // If no URI is given the topic gets a default URI based on its ID, if requested.
        // Note: this must be done *after* the topic is stored. The ID is not known before.
        // Note: in case no URI was given: once stored a topic's URI is empty (not null).
        if (uriPrefix != null && model.getUri().equals("")) {
            model.updateUri(uriPrefix + model.getId());
        }
    }

    /**
     * Creates a new association in the DB.
     * ### TODO: should be private. Currently called from AccessControlImpl.assignToWorkspace().
     */
    void _createAssociation(AssociationModel model) {
        // 1) store in DB
        storeAssociation(model);
        valueStorage.storeValue(model);
        createAssociationInstantiation(model.getId(), model.getTypeUri());
    }

    // ---

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
        try {
            return typeCache.getTopicType(uri);
        } catch (Exception e) {
            throw new RuntimeException("Fetching topic type \"" + uri + "\" failed", e);
        }
    }

    AssociationType getAssociationType(String uri) {
        try {
            return typeCache.getAssociationType(uri);
        } catch (Exception e) {
            throw new RuntimeException("Fetching association type \"" + uri + "\" failed", e);
        }
    }

    // ---

    TopicType createTopicType(TopicTypeModel model) {
        try {
            // store in DB
            createTopic(model, URI_PREFIX_TOPIC_TYPE);          // create generic topic
            typeStorage.storeType(model);                       // store type-specific parts
            //
            // instantiate
            TopicType topicType = new TopicTypeImpl(model, this);
            typeCache.putTopicType(topicType);
            //
            em.fireEvent(CoreEvent.INTRODUCE_TOPIC_TYPE, topicType);
            return topicType;
        } catch (Exception e) {
            throw new RuntimeException("Creating topic type \"" + model.getUri() + "\" failed (" + model + ")", e);
        }
    }

    AssociationType createAssociationType(AssociationTypeModel model) {
        try {
            // store in DB
            createTopic(model, URI_PREFIX_ASSOCIATION_TYPE);    // create generic topic
            typeStorage.storeType(model);                       // store type-specific parts
            //
            // instantiate
            AssociationType assocType = new AssociationTypeImpl(model, this);
            typeCache.putAssociationType(assocType);
            //
            em.fireEvent(CoreEvent.INTRODUCE_ASSOCIATION_TYPE, assocType);
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



    // ===

    /**
     * Deletes 1) this DeepaMehta object's child topics (recursively) which have an underlying association definition of
     * type "Composition Definition" and 2) deletes all the remaining direct associations of this DeepaMehta object.
     * <p>
     * Note: deletion of the object itself is up to the subclasses. ### FIXDOC
     */
    void deleteObject(DeepaMehtaObjectModelImpl object) {
        try {
            em.fireEvent(object.getPreDeleteEvent(), object.instantiate());
            //
            // delete child topics (recursively)
            for (AssociationDefinitionModel assocDef : object.getType().getAssocDefs()) {
                if (assocDef.getTypeUri().equals("dm4.core.composition_def")) {
                    for (TopicModel childTopic : object.getRelatedTopics(assocDef.getInstanceLevelAssocTypeUri(),
                            "dm4.core.parent", "dm4.core.child", assocDef.getChildTypeUri())) {
                        deleteObject((DeepaMehtaObjectModelImpl) childTopic);
                    }
                }
            }
            // delete direct associations
            for (AssociationModel assoc : object.getAssociations()) {
                deleteObject((DeepaMehtaObjectModelImpl) assoc);
            }
            // delete object itself
            logger.info("Deleting " + object);
            Directives.get().add(object.getDeleteDirective(), object);
            object.delete();
            //
            em.fireEvent(object.getPostDeleteEvent(), object);
        } catch (IllegalStateException e) {
            // Note: getAssociations() might throw IllegalStateException and is no problem.
            // This can happen when this object is an association which is already deleted.
            //
            // Consider this particular situation: let A1 and A2 be associations of this object and let A2 point to A1.
            // If A1 gets deleted first (the association set order is non-deterministic), A2 is implicitely deleted
            // with it (because it is a direct association of A1 as well). Then when the loop comes to A2
            // "IllegalStateException: Node[1327] has been deleted in this tx" is thrown because A2 has been deleted
            // already. (The Node appearing in the exception is the middle node of A2.) If, on the other hand, A2
            // gets deleted first no error would occur.
            //
            // This particular situation exists when e.g. a topicmap is deleted while one of its mapcontext
            // associations is also a part of the topicmap itself. This originates e.g. when the user reveals
            // a topicmap's mapcontext association and then deletes the topicmap.
            //
            if (e.getMessage().equals("Node[" + object.getId() + "] has been deleted in this tx")) {
                logger.info("### Association " + object.getId() + " has already been deleted in this transaction. " +
                    "This can happen while deleting a topic with associations A1 and A2 while A2 points to A1 (" +
                    object + ")");
            } else {
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("Deleting " + object.className() + " " + object.getId() + " failed (" +
                object + ")", e);
        }
    }



    // === Instantiation ===

    // These methods 1) instantiate objects from models, and 2) check the READ permission for each model.
    // Call these methods when passing objects fetched from the DB to the user.
    // ### TODO: make these private?

    Topic instantiateTopic(TopicModel model) {
        checkReadAccess(model);
        return new TopicImpl(model, this);
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
        return new RelatedTopicImpl(model, this);
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

    Association instantiateAssociation(AssociationModel model) {
        checkReadAccess(model);
        return new AssociationImpl(model, this);
    }

    List<Association> instantiateAssociations(List<AssociationModel> models) {
        List<Association> assocs = new ArrayList();
        for (AssociationModel model : models) {
            try {
                assocs.add(instantiateAssociation(model));
            } catch (AccessControlException e) {
                // don't add to result and continue
            }
        }
        return assocs;
    }

    // ---

    RelatedAssociation instantiateRelatedAssociation(RelatedAssociationModel model) {
        checkReadAccess(model);
        return new RelatedAssociationImpl(model, this);
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

    private void checkReadAccess(TopicModel model) {
        em.fireEvent(CoreEvent.PRE_GET_TOPIC, model.getId());          // throws AccessControlException
    }

    private void checkReadAccess(AssociationModel model) {
        em.fireEvent(CoreEvent.PRE_GET_ASSOCIATION, model.getId());    // throws AccessControlException
    }



    // ===

    private void bootstrapTypeCache() {
        TopicTypeModel metaMetaType = mf.newTopicTypeModel("dm4.core.meta_meta_type", "Meta Meta Type",
            "dm4.core.text");
        metaMetaType.setTypeUri("dm4.core.meta_meta_meta_type");
        typeCache.putTopicType(new TopicTypeImpl(metaMetaType, this));
    }
}
