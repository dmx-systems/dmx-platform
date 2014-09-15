package de.deepamehta.core.impl;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.ChildTopicsModel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;



public class CompositeTest {

    @Test
    public void composite() {
        ChildTopicsModel person = new ChildTopicsModel()
            .put("dm4.core.name", "Karl Blum")
            .put("dm4.contacts.home_address", new ChildTopicsModel()
                .put("dm4.contacts.postal_code", 13206)
                .put("dm4.contacts.city", "Berlin"))
            .put("dm4.contacts.office_address", new ChildTopicsModel()
                .put("dm4.contacts.postal_code", 14345)
                .put("dm4.contacts.city", "Berlin"));
        //
        assertEquals("Karl Blum", person.getString("dm4.core.name"));
        //
        ChildTopicsModel address = person.getChildTopicsModel("dm4.contacts.home_address");
        assertEquals("Berlin", address.getString("dm4.contacts.city"));
        //
        Object code = address.getObject("dm4.contacts.postal_code");
        assertSame(Integer.class, code.getClass());
        assertEquals(13206, code);  // autoboxing
    }
}
