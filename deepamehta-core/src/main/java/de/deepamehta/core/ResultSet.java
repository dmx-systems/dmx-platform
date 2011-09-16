package de.deepamehta.core;

import java.util.Iterator;
import java.util.Set;



public class ResultSet<T> implements Iterable<T> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private int totalCount;
    private Set<T> items;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public ResultSet(int totalCount, Set<T> items) {
        this.totalCount = totalCount;
        this.items = items;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Iterator<T> iterator() {
        return getIterator();
    }

    // ---

    public int getSize() {
        return items.size();
    }

    public int getTotalCount() {
        return totalCount;
    }

    public Set<T> getItems() {
        return items;
    }

    public Iterator<T> getIterator() {
        return items.iterator();
    }
}
