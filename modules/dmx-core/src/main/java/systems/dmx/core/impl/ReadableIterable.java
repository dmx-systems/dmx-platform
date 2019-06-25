package systems.dmx.core.impl;

import java.util.Iterator;



/**
 * A wrapper iterable that skips unreadables from an underlying DMXObjectModel iterator.
 */
class ReadableIterable<M extends DMXObjectModelImpl> implements Iterable<M> {

    private Iterator<M> objects;    // underlying iterator
    private Iterator<M> i;          // wrapper iterator
    private M next;                 // next readable object; updated by findNext()

    ReadableIterable(Iterator<M> objects) {
        this.objects = objects;
        this.i = new Iterator() {

            @Override
            public boolean hasNext() {
                findNext();
                return next != null;
            }

            @Override
            public M next() {
                return next;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Iterator<M> iterator() {
        return i;       // FIXME: return new Iterator instance
    }

    private void findNext() {
        next = null;
        while (objects.hasNext() && next == null) {
            M model = objects.next();
            if (model.isReadable()) {
                next = model;
            }
        }
    }
}
