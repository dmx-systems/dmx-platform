package de.deepamehta.core;

import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.Iterator;
import java.util.Set;



public class ResultSet<T extends JSONEnabled> implements JSONEnabled, Iterable<T> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private int totalCount;
    private Set<T> items;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public ResultSet(int totalCount, Set<T> items) {
        this.totalCount = totalCount;
        this.items = items;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // === JSONEnabled Implementation ===

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

    // === Iterable Implementation ===

    @Override
    public Iterator<T> iterator() {
        return getIterator();
    }

    // ===

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
