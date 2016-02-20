package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
import de.deepamehta.core.storage.spi.DeepaMehtaStorage;

import static java.util.Arrays.asList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



public class PersistenceLayer extends StorageDecorator {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    TypeStorageImpl typeStorage;
    ValueStorage valueStorage;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public PersistenceLayer(DeepaMehtaStorage storage) {
        super(storage);
        this.typeStorage = new TypeStorageImpl(this);
        this.valueStorage = new ValueStorage(this);
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * Deletes 1) this DeepaMehta object's child topics (recursively) which have an underlying association definition of
     * type "Composition Definition" and 2) deletes all the remaining direct associations of this DeepaMehta object.
     * <p>
     * Note: deletion of the object itself is up to the subclasses. ### FIXDOC
     */
    void deleteObject(DeepaMehtaObjectModelImpl object) {
        try {
            dms.fireEvent(object.getPreDeleteEvent(), object);
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
            dms.fireEvent(object.getPostDeleteEvent(), object);
        } catch (Exception e) {
            throw new RuntimeException("Deleting " + object.className() + " failed (" + object + ")", e);
        }
    }
}
