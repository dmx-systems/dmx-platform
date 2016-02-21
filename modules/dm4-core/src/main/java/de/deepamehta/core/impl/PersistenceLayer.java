package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.ModelFactory;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.storage.spi.DeepaMehtaStorage;

import static java.util.Arrays.asList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



public class PersistenceLayer extends StorageDecorator {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    TypeStorageImpl typeStorage;
    ValueStorage valueStorage;

    EventManager em;
    ModelFactory mf;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public PersistenceLayer(DeepaMehtaStorage storage) {
        super(storage);
        this.typeStorage = new TypeStorageImpl(this);
        this.valueStorage = new ValueStorage(this);
        //
        this.em = new EventManager();
        this.mf = storage.getModelFactory();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    TopicModel createTopic(TopicModel model, String uriPrefix) {
        try {
            em.fireEvent(CoreEvent.PRE_CREATE_TOPIC, model);
            _createTopic(model, uriPrefix);
            em.fireEvent(CoreEvent.POST_CREATE_TOPIC, model);
            return model;
        } catch (Exception e) {
            throw new RuntimeException("Creating topic " + model.getId() + " failed (typeUri=\"" + model.getTypeUri() +
                "\")", e);
        }
    }

    AssociationModel createAssociation(AssociationModel model) {
        try {
            em.fireEvent(CoreEvent.PRE_CREATE_ASSOCIATION, model);
            _createAssociation(model);
            em.fireEvent(CoreEvent.POST_CREATE_ASSOCIATION, model);
            return model;
        } catch (Exception e) {
            throw new RuntimeException("Creating association failed (" + model + ")", e);
        }
    }

    // ---

    /**
     * Convenience method.
     */
    TopicModel createTopic(TopicModel model) {
        return createTopic(model, null);    // uriPrefix=null
    }

    /**
     * Convenience method.
     */
    AssociationModel createAssociation(String typeUri, RoleModel roleModel1, RoleModel roleModel2) {
        return createAssociation(mf.newAssociationModel(typeUri, roleModel1, roleModel2));
    }

    // ---

    /**
     * Factory method: creates a new topic in the DB according to the given model
     * and returns a topic instance. ### FIXDOC
     */
    private void _createTopic(TopicModel model, String uriPrefix) {
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
            long topicId = model.getId();
            String uri = uriPrefix + topicId;
            model.setUri(uri);              // update memory
            storeTopicUri(topicId, uri);    // update DB
        }
    }

    /**
     * Factory method: creates a new association in the DB according to the given model
     * and returns an association instance. ### FIXDOC
     */
    private void _createAssociation(AssociationModel model) {
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
            em.fireEvent(object.getPostDeleteEvent(), object);
        } catch (Exception e) {
            throw new RuntimeException("Deleting " + object.className() + " failed (" + object + ")", e);
        }
    }
}
