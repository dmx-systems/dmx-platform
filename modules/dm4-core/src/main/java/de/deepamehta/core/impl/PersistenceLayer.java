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
            em.fireEvent(object.getPreDeleteEvent(), object);
            //
            // 1) delete child topics (recursively)
            for (AssociationDefinitionModel assocDef : object.getType().getAssocDefs()) {
                if (assocDef.getTypeUri().equals("dm4.core.composition_def")) {
                    for (TopicModel childTopic : object.getRelatedTopics(assocDef.getInstanceLevelAssocTypeUri(),
                            "dm4.core.parent", "dm4.core.child", assocDef.getChildTypeUri())) {
                        deleteObject((DeepaMehtaObjectModelImpl) childTopic);
                    }
                }
            }
            // 2) delete direct associations
            for (AssociationModel assoc : object.getAssociations()) {
                deleteObject((DeepaMehtaObjectModelImpl) assoc);
            }
            // delete topic itself
            logger.info("Deleting " + object);
            Directives.get().add(object.getDeleteDirective(), object);
            object.delete();
            //
            em.fireEvent(object.getPostDeleteEvent(), object);  // ### FIXME: adapt listener
        } catch (Exception e) {
            throw new RuntimeException("Deleting " + object.className() + " failed (" + object + ")", e);
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
