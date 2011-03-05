package de.deepamehta.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;



public class JSONTest {

    // --- Testing Serialization ---

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
            "{\"id\":123,\"value\":\"de.deepamehta.test.JSONTest$ClassWithoutToStringMethod@"));
    }

    // --- Testing Get ---

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
        assertEquals("de.deepamehta.test.JSONTest$ClassWithToStringMethod", obj.getClass().getName());
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
