package de.deepamehta.core.impl.service;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.CompositeValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;



public class CompositeTest {

    @Test
    public void composite() {
        CompositeValue person = new CompositeValue("{" +
            "\"dm4.core.name\": \"Karl Blum\"," +
            "\"dm4.contacts.home_address\": {" +
                "\"dm4.contacts.postal_code\": 13206," +
                "\"dm4.contacts.city\": \"Berlin\"" +
            "}," +
            "\"dm4.contacts.office_address\": {" +
                "\"dm4.contacts.postal_code\": 14345," +
                "\"dm4.contacts.city\": \"Berlin\"" +
            "}" +
        "}");
        assertEquals("Karl Blum", person.get("dm4.core.name"));
        //
        CompositeValue address = (CompositeValue) person.get("dm4.contacts.home_address");
        assertEquals("Berlin", address.get("dm4.contacts.city"));
        //
        Object code = address.get("dm4.contacts.postal_code");
        assertSame(Integer.class, code.getClass());
        assertEquals(13206, code);  // autoboxing
    }
}
