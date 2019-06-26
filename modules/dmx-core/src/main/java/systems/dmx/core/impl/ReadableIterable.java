package systems.dmx.core.impl;

import java.util.Iterator;



/**
 * A wrapper iterable that skips unreadables from an underlying DMXObjectModel iterable.
 */
class ReadableIterable<M extends DMXObjectModelImpl> implements Iterable<M> {

    private Iterable<M> models;     // underlying iterable

    ReadableIterable(Iterable<M> models) {
        this.models = models;
    }

    @Override
    public Iterator<M> iterator() {
        return new ReadableIterator();
    }

    // ---------------------------------------------------------------------------------------------------- Nested Class

    private class ReadableIterator implements Iterator<M> {

        private Iterator<M> i = models.iterator();
        private M next;             // next readable object; updated by findNext()

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

        private void findNext() {
            next = null;
            while (i.hasNext() && next == null) {
                M model = i.next();
                if (model.isReadable()) {
                    next = model;
                }
            }
        }
    }
}
