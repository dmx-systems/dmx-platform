package de.deepamehta.core.impl;

import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ModelFactory;



class ModelFactoryImpl implements ModelFactory {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private StorageDecorator storageDecorator;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ModelFactoryImpl(StorageDecorator storageDecorator) {
        this.storageDecorator = storageDecorator;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    public TopicModel topicModel(long id, String uri, String typeUri, SimpleValue value, ChildTopicsModel childTopics) {
        return new TopicModelImpl(id, uri, typeUri, value, childTopics);
    }
}
