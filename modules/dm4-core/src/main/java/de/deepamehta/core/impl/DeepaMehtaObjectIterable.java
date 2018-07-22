package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.DMXObject;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.TopicModel;

import java.util.Iterator;



/**
 * An iterable over all topics stored in the DB.
 */
class TopicIterable implements Iterable<Topic> {

    private Iterator<Topic> topics;

    TopicIterable(PersistenceLayer pl) {
        this.topics = new TopicIterator(pl);
    }

    @Override
    public Iterator<Topic> iterator() {
        return topics;
    }
}

/**
 * An iterable over all associations stored in the DB.
 */
class AssociationIterable implements Iterable<Association> {

    private Iterator<Association> assocs;

    AssociationIterable(PersistenceLayer pl) {
        this.assocs = new AssociationIterator(pl);
    }

    @Override
    public Iterator<Association> iterator() {
        return assocs;
    }
}



// ===



class TopicIterator extends ObjectIterator<Topic, TopicModelImpl> {

    TopicIterator(PersistenceLayer pl) {
        super(pl);
    }

    @Override
    Iterator<TopicModelImpl> fetchObjects() {
        return pl.fetchAllTopics();
    }

    @Override
    Topic instantiateObject(TopicModelImpl model) {
        return pl.checkReadAccessAndInstantiate(model);
    }
}



class AssociationIterator extends ObjectIterator<Association, AssociationModelImpl> {

    AssociationIterator(PersistenceLayer pl) {
        super(pl);
    }

    @Override
    Iterator<AssociationModelImpl> fetchObjects() {
        return pl.fetchAllAssociations();
    }

    @Override
    Association instantiateObject(AssociationModelImpl model) {
        return pl.checkReadAccessAndInstantiate(model);
    }
}



abstract class ObjectIterator<O extends DMXObject, M extends DeepaMehtaObjectModelImpl> implements Iterator<O> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected PersistenceLayer pl;
    private Iterator<M> objects;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ObjectIterator(PersistenceLayer pl) {
        this.pl = pl;
        this.objects = fetchObjects();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public boolean hasNext() {
        return objects.hasNext();
    }

    @Override
    public O next() {
        return instantiateObject(objects.next());
    }

    @Override
    public void remove() {
        objects.remove();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    abstract Iterator<M> fetchObjects();

    abstract O instantiateObject(M model);
}
