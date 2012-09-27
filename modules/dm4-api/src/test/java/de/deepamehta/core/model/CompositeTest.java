package de.deepamehta.core.model;

import de.deepamehta.core.model.CompositeValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;



public class CompositeTest {

    @Test
    public void composite() {
        CompositeValue person = new CompositeValue()
            .put("dm4.core.name", "Karl Blum")
            .put("dm4.contacts.home_address", new CompositeValue()
                .put("dm4.contacts.postal_code", 13206)
                .put("dm4.contacts.city", "Berlin"))
            .put("dm4.contacts.office_address", new CompositeValue()
                .put("dm4.contacts.postal_code", 14345)
                .put("dm4.contacts.city", "Berlin"));
        //
        assertEquals("Karl Blum", person.getString("dm4.core.name"));
        //
        CompositeValue address = person.getComposite("dm4.contacts.home_address");
        assertEquals("Berlin", address.getString("dm4.contacts.city"));
        //
        Object code = address.get("dm4.contacts.postal_code");
        assertSame(Integer.class, code.getClass());
        assertEquals(13206, code);  // autoboxing
    }
}
