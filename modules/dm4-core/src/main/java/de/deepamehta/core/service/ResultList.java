package de.deepamehta.core.service;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



public class ResultList<T extends JSONEnabled> implements Iterable<T>, JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private int totalCount;
    private List<T> items;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public ResultList() {
        this.totalCount = 0;
        this.items = new ArrayList<T>();
    }

    public ResultList(int totalCount, List<T> items) {
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

    public List<T> getItems() {
        return items;
    }

    // ---

    public void addAll(ResultList<T> result) {
        totalCount += result.getTotalCount();
        items.addAll(result.getItems());
    }

    // ---

    public ResultList<T> loadChildTopics() {
        for (T item : this) {
            // Note: we store also models in a result list. So we need a cast here.
            ((DeepaMehtaObject) item).loadChildTopics();
        }
        return this;
    }

    // *** Iterable Implementation ***

    @Override
    public Iterator<T> iterator() {
        return items.iterator();
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
