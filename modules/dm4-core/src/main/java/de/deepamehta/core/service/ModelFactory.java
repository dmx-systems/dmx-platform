package de.deepamehta.core.service;

import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;



public interface ModelFactory {

    TopicModel topicModel(long id, String uri, String typeUri, SimpleValue value, ChildTopicsModel childTopics);
}
