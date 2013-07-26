package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.TopicModel;

import java.util.Iterator;



class TopicIterable implements Iterable<Topic> {

    private Iterator<Topic> topics;

    TopicIterable(EmbeddedService dms) {
        this.topics = new TopicIterator(dms);
    }

    @Override
    public Iterator<Topic> iterator() {
        return topics;
    }
}

class AssociationIterable implements Iterable<Association> {

    private Iterator<Association> assocs;

    AssociationIterable(EmbeddedService dms) {
        this.assocs = new AssociationIterator(dms);
    }

    @Override
    public Iterator<Association> iterator() {
        return assocs;
    }
}



// ===



class TopicIterator extends ObjectIterator<Topic, TopicModel> {

    TopicIterator(EmbeddedService dms) {
        super(dms);
    }

    @Override
    Iterator<TopicModel> fetchObjects() {
        return dms.storage.fetchAllTopics();
    }

    @Override
    Topic instantiateObject(TopicModel model) {
        return dms.instantiateTopic(model, false);          // fetchComnposite=false
    }
}



class AssociationIterator extends ObjectIterator<Association, AssociationModel> {

    AssociationIterator(EmbeddedService dms) {
        super(dms);
    }

    @Override
    Iterator<AssociationModel> fetchObjects() {
        return dms.storage.fetchAllAssociations();
    }

    @Override
    Association instantiateObject(AssociationModel model) {
        return dms.instantiateAssociation(model, false);    // fetchComnposite=false
    }
}



abstract class ObjectIterator<E extends DeepaMehtaObject, M extends DeepaMehtaObjectModel> implements Iterator<E> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected EmbeddedService dms;
    private Iterator<M> objects;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ObjectIterator(EmbeddedService dms) {
        this.dms = dms;
        this.objects = fetchObjects();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public boolean hasNext() {
        return objects.hasNext();
    }

    @Override
    public E next() {
        return instantiateObject(objects.next());
    }

    @Override
    public void remove() {
        objects.remove();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    abstract Iterator<M> fetchObjects();

    abstract E instantiateObject(M model);
}
