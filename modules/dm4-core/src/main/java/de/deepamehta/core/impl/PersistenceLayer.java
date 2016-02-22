package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.storage.spi.DeepaMehtaStorage;

import java.util.logging.Logger;



public class PersistenceLayer extends StorageDecorator {

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

    // ---

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
}
