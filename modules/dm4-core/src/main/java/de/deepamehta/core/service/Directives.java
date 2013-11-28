package de.deepamehta.core.service;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.util.DeepaMehtaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



public class Directives implements Iterable<Directives.Entry>, JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private List<Entry> directives = new ArrayList();

    // -------------------------------------------------------------------------------------------------- Public Methods

    public void add(Directive dir, JSONEnabled arg) {
        directives.add(new Entry(dir, arg));
    }

    // *** JSONEnabled Implementation ***

    public JSONObject toJSON() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("directives", DeepaMehtaUtils.objectsToJSON(directives));
            return obj;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // *** Iterable Implementation ***

    @Override
    public Iterator<Entry> iterator() {
        return directives.iterator();
    }

    // --------------------------------------------------------------------------------------------------- Inner Classes

    public class Entry implements JSONEnabled {

        public Directive dir;
        public JSONEnabled arg;

        private Entry(Directive dir, JSONEnabled arg) {
            this.dir = dir;
            this.arg = arg;
        }

        @Override
        public JSONObject toJSON() {
            try {
                JSONObject obj = new JSONObject();
                obj.put("type", dir);
                obj.put("arg", arg.toJSON());
                return obj;
            } catch (Exception e) {
                throw new RuntimeException("Serialization failed (" + this + ")", e);
            }
        }

        @Override
        public String toString() {
            return dir + ": " + arg;
        }
    }
}
