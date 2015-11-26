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

    private List<T> items;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public ResultList() {
        this.items = new ArrayList<T>();
    }

    public ResultList(List<T> items) {
        this.items = items;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public T get(int index) {
        return items.get(index);
    }

    public int getSize() {
        return items.size();
    }

    public List<T> getItems() {
        return items;
    }

    // ---

    public void add(T item) {
        items.add(item);
    }

    public void addAll(ResultList<T> result) {
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
            return new JSONObject().put("items", DeepaMehtaUtils.toJSONArray(items));
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }
}
