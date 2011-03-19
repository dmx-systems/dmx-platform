package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicData;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.model.TopicTypeDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;



public class CompositeTest {

    @Test
    public void composite() {
        Composite person = new Composite("{" +
            "\"dm3.core.name\": \"Karl Blum\"," +
            "\"dm3.contacts.home_address\": {" +
                "\"dm3.contacts.postal_code\": 13206," +
                "\"dm3.contacts.city\": \"Berlin\"" +
            "}," +
            "\"dm3.contacts.office_address\": {" +
                "\"dm3.contacts.postal_code\": 14345," +
                "\"dm3.contacts.city\": \"Berlin\"" +
            "}" +
        "}");
        assertEquals("Karl Blum", person.get("dm3.core.name"));
        //
        Composite address = (Composite) person.get("dm3.contacts.home_address");
        assertEquals("Berlin", address.get("dm3.contacts.city"));
        //
        Object code = address.get("dm3.contacts.postal_code");
        assertSame(Integer.class, code.getClass());
        assertEquals(13206, code);  // autoboxing
    }
}
