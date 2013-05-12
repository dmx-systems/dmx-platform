package de.deepamehta.core;

import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;



public class ResultSet<T extends JSONEnabled> implements Iterable<T>, JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private int totalCount;
    private Set<T> items;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public ResultSet() {
        this.totalCount = 0;
        this.items = new HashSet<T>();
    }

    public ResultSet(int totalCount, Set<T> items) {
        this.totalCount = totalCount;
        this.items = items;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

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

    // ---

    public void addAll(ResultSet<T> result) {
        totalCount += result.getTotalCount();
        items.addAll(result.getItems());
    }

    // *** Iterable Implementation ***

    @Override
    public Iterator<T> iterator() {
        return getIterator();
    }

    // *** JSONEnabled Implementation ***

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            o.put("total_count", totalCount);
            o.put("items", DeepaMehtaUtils.objectsToJSON(items));
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }
}
