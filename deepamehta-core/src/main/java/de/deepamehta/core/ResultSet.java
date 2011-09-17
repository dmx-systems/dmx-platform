package de.deepamehta.core;

import de.deepamehta.core.model.DeepaMehtaObjectModel;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;



public class ResultSet<T> implements JSONEnabled, Iterable<T> {

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
            o.put("items", DeepaMehtaObjectModel.objectsToJSON((Collection<JSONEnabled>) items));
            return o;
        } catch (JSONException e) {
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
