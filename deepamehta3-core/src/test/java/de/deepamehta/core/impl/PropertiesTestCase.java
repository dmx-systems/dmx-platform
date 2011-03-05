package de.deepamehta.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.deepamehta.core.model.Properties;
import de.deepamehta.core.model.PropValue;



public class PropertiesTestCase {

    private Properties p;

    @Before
    public void setup() {
        p = new Properties();
        p.put("string", "hi");
        p.put("int", 123);
        p.put("long", 456L);
        p.put("boolean", true);
        p.put("stringvalue", new PropValue("you"));
        p.put("intvalue", new PropValue(789));
        p.put("longvalue", new PropValue(101112L));
        p.put("booleanvalue", new PropValue(false));
    }

    @Test
    public void get() {
        assertEquals("hi",    p.get("string").toString());
        assertEquals(123,     p.get("int").intValue());
        assertEquals(456L,    p.get("long").longValue());
        assertEquals(true,    p.get("boolean").booleanValue());
        assertEquals("you",   p.get("stringvalue").toString());
        assertEquals(789,     p.get("intvalue").intValue());
        assertEquals(101112L, p.get("longvalue").longValue());
        assertEquals(false,   p.get("booleanvalue").booleanValue());
    }

    @Test
    public void equals() {
        Properties p2 = new Properties();
        p2.put("string", "hi");
        p2.put("int", 123);
        p2.put("long", 456L);
        p2.put("boolean", true);
        p2.put("stringvalue", "you");
        p2.put("intvalue", 789);
        p2.put("longvalue", 101112L);
        p2.put("booleanvalue", false);
        //
        assertEquals(p, p2);
    }
}
