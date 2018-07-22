package de.deepamehta.core.service;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.util.DMXUtils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



/**
 * Note: Directives is not {@link de.deepamehta.core.JSONEnabled} as the underlying structure is a list, not an object.
 * There is a {@link #toJSONArray} method though.
 */
public class Directives implements Iterable<Directives.Entry> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private List<Entry> directives = new ArrayList();

    // ------------------------------------------------------------------------------------------------- Class Variables

    private static Logger logger = Logger.getLogger("de.deepamehta.core.service.Directives");

    private static final ThreadLocal<Directives> threadLocalDirectives = new ThreadLocal() {
        @Override
        protected Directives initialValue() {
            logger.fine("### Creating tread-local directives");
            return new Directives();
        }
    };

    // -------------------------------------------------------------------------------------------------- Public Methods

    public void add(Directive dir, JSONEnabled arg) {
        directives.add(new Entry(dir, arg));
    }

    public JSONArray toJSONArray() {
        return DMXUtils.toJSONArray(directives);
    }

    // ---

    public static Directives get() {
        return threadLocalDirectives.get();
    }

    public static void remove() {
        logger.fine("### Removing tread-local directives");
        threadLocalDirectives.remove();
    }

    // *** Iterable Implementation ***

    @Override
    public Iterator<Entry> iterator() {
        return directives.iterator();
    }

    // -------------------------------------------------------------------------------------------------- Nested Classes

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
                return new JSONObject()
                    .put("type", dir)
                    .put("arg", arg.toJSON());
            } catch (Exception e) {
                throw new RuntimeException("Serialization failed", e);
            }
        }

        @Override
        public String toString() {
            return dir + ": " + arg;
        }
    }
}
