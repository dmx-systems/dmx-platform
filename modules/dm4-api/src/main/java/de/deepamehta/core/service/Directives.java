package de.deepamehta.core.service;

import de.deepamehta.core.JSONEnabled;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



public class Directives implements Iterable<Directives.Entry> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private List<Entry> directives = new ArrayList<Entry>();

    // -------------------------------------------------------------------------------------------------- Public Methods

    public void add(Directive dir, JSONEnabled arg) {
        directives.add(new Entry(dir, arg));
    }

    public JSONArray toJSON() {
        try {
            JSONArray array = new JSONArray();
            for (Entry directive : directives) {
                JSONObject dir = new JSONObject();
                dir.put("type", directive.dir);
                dir.put("arg",  directive.arg.toJSON());
                array.put(dir);
            }
            return array;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    @Override
    public Iterator<Entry> iterator() {
        return directives.iterator();
    }

    // --------------------------------------------------------------------------------------------------- Inner Classes

    public class Entry {

        public Directive dir;
        public JSONEnabled arg;

        private Entry(Directive dir, JSONEnabled arg) {
            this.dir = dir;
            this.arg = arg;
        }

        @Override
        public String toString() {
            return dir + ": " + arg;
        }
    }
}
