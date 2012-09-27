package de.deepamehta.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



public class JSONTest {

    // --- Serialization ---

    @Test
    public void put() throws JSONException {
        JSONObject o = new JSONObject();
        JSONObject ret = o.put("value", "hello");
        //
        assertSame(o, ret);
    }

    @Test
    public void string() throws JSONException {
        String str = "hi";
        //
        JSONObject o = new JSONObject();
        o.put("id", 123);
        o.put("value", str);
        //
        assertEquals("{\"id\":123,\"value\":\"hi\"}", o.toString());
    }

    @Test
    public void nullObject() throws JSONException {
        String str = null;
        //
        JSONObject o = new JSONObject();
        o.put("id", 123);
        o.put("value", str);
        // null values are not put into the JSONObject
        assertEquals("{\"id\":123}", o.toString());
    }

    @Test
    public void classWithToStringMethod() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", 123);
        o.put("value", new ClassWithToStringMethod());
        // Every type of object can be but. For serialization the object's toString() method is consulted.
        assertEquals("{\"id\":123,\"value\":\"456\"}", o.toString());
    }

    @Test
    public void classWithNullReturningToStringMethod() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", 123);
        o.put("value", new ClassWithNullReturningToStringMethod());
        // If the object's toString() returns null it is serialized as "" (!)
        assertEquals("{\"id\":123,\"value\":\"\"}", o.toString());
    }

    @Test
    public void classWithoutToStringMethod() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", 123);
        o.put("value", new ClassWithoutToStringMethod());
        // if toString() is not overridden:
        assertTrue(o.toString().startsWith(
            "{\"id\":123,\"value\":\"" + ClassWithoutToStringMethod.class.getName()));
    }

    // --- Get ---

    @Test
    public void getClassWithToStringMethod() throws JSONException {
        Object val = new ClassWithToStringMethod();
        //
        JSONObject o = new JSONObject();
        o.put("id", 123);
        o.put("value", val);
        assertEquals("{\"id\":123,\"value\":\"456\"}", o.toString());
        //
        Object obj = o.get("value");
        assertEquals(ClassWithToStringMethod.class.getName(), obj.getClass().getName());
        assertSame(val, obj);   // the very object is stored in the JSONObject
        //
        String s = o.getString("value");
        assertEquals("456", s); // getting the object as string returns its toString() result
    }

    @Test
    public void getClassWithNullReturningToStringMethod() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", 123);
        o.put("value", new ClassWithNullReturningToStringMethod());
        assertEquals("{\"id\":123,\"value\":\"\"}", o.toString());
        //
        String s = o.getString("value");
        assertNull(s);  // getting the object as string returns its toString() result -- null in this case
    }

    // --- Default Values ---

    @Test
    public void defaultNumber() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", 123);
        assertEquals(456, o.optLong("value", 456));
        assertEquals(-1, o.optLong("value", -1));
        assertEquals(0, o.optLong("value"));
    }

    @Test
    public void defaultString() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", 123);
        assertEquals("test", o.optString("value", "test"));
        assertEquals("", o.optString("value"));
        assertNull(o.optString("value", null));
    }

    @Test
    public void defaultObject() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", 123);
        assertNull(o.optJSONObject("id"));      // the "id" value is not a JSONObject -> null is returned
        assertNull(o.optJSONObject("value"));
    }

    // --- Iteration ---

    @Test
    public void iteration() throws JSONException {
        List<String> keys = new ArrayList<String>();
        JSONObject o = new JSONObject("{d: 78, c: 90, a: 56, e: 12, b: 34}");
        Iterator<?> i = (Iterator<?>) o.keys();
        while (i.hasNext()) {
            String key = (String) i.next();
            keys.add(key);
        }
        assertEquals(Arrays.asList("d", "c", "a", "e", "b"), keys);
        // the jettison JSONObject implementation preserves the natural key order (uses LinkedHashMap)
    }

    @Test
    public void names() throws JSONException {
        JSONObject o = new JSONObject("{d: 78, c: 90, a: 56, e: 12, b: 34}");
        JSONArray keys = o.names();
        assertEquals("[\"d\",\"c\",\"a\",\"e\",\"b\"]", keys.toString());
        // the jettison JSONObject implementation preserves the natural key order (uses LinkedHashMap)
    }

    // --- Comments ---

    @Test
    public void comment() throws JSONException {
        JSONObject o = new JSONObject("{abc: 123, /* comment */ def: 456}");
        assertEquals(123, o.getInt("abc"));
        assertEquals(456, o.getInt("def"));
        // the jettison JSON parser allows comments!
        // That's surprising as comments are not covered by the JSON specification.
    }

    @Test
    public void commentToLineEnd() throws JSONException {
        JSONObject o = new JSONObject("{abc: 123, // comment\ndef: 456}");
        assertEquals(123, o.getInt("abc"));
        assertEquals(456, o.getInt("def"));
        // the jettison JSON parser allows comments!
        // That's surprising as comments are not covered by the JSON specification.
    }

    @Test
    public void commentHash() throws JSONException {
        JSONObject o = new JSONObject("{abc: 123, # comment\ndef: 456}");
        assertEquals(123, o.getInt("abc"));
        assertEquals(456, o.getInt("def"));
        // the jettison JSON parser allows comments!
        // That's surprising as comments are not covered by the JSON specification.
    }

    // --- Helper Classes ---

    class ClassWithToStringMethod {
        public String toString() {
            return "456";
        }
    }

    class ClassWithNullReturningToStringMethod {
        public String toString() {
            return null;
        }
    }

    class ClassWithoutToStringMethod {
    }
}
