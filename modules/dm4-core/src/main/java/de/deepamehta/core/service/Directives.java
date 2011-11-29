package de.deepamehta.core.service;

import de.deepamehta.core.JSONEnabled;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.List;



public class Directives {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private List<Dir> directives = new ArrayList();

    // -------------------------------------------------------------------------------------------------- Public Methods

    public void add(Directive type, JSONEnabled arg) {
        directives.add(new Dir(type, arg));
    }

    public JSONArray toJSON() {
        try {
            JSONArray array = new JSONArray();
            for (Dir directive : directives) {
                JSONObject dir = new JSONObject();
                dir.put("type", directive.type);
                dir.put("arg",  directive.arg.toJSON());
                array.put(dir);
            }
            return array;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // --------------------------------------------------------------------------------------------------- Inner Classes

    private class Dir {

        private Directive type;
        private JSONEnabled arg;

        private Dir(Directive type, JSONEnabled arg) {
            this.type = type;
            this.arg = arg;
        }

        @Override
        public String toString() {
            return type + ": " + arg;
        }
    }
}
