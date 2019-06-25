package systems.dmx.core.impl;

import java.util.Iterator;



/**
 * A wrapper iterable that instantiates models from an underlying DMXObjectModel iterator.
 */
class InstantiationIterable<O extends DMXObjectImpl, M extends DMXObjectModelImpl> implements Iterable<O> {

    private Iterable<M> models;     // underlying iterable

    InstantiationIterable(Iterable<M> models) {
        this.models = models;
    }

    @Override
    public Iterator<O> iterator() {
        Iterator<M> i = models.iterator();
        return new Iterator() {

            @Override
            public boolean hasNext() {
                return i.hasNext();
            }

            @Override
            public O next() {
                return i.next().instantiate();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
