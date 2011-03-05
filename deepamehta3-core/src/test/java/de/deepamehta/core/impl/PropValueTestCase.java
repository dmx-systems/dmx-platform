package de.deepamehta.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.deepamehta.core.model.Properties;
import de.deepamehta.core.model.PropValue;



public class PropValueTestCase {

    @Test
    public void equals() {
        assertEquals(new PropValue("you"), new PropValue("you"));
        assertEquals(new PropValue(123), new PropValue(123));
        assertEquals(new PropValue(101112L), new PropValue(101112L));
        assertEquals(new PropValue(false), new PropValue(false));
        assertEquals(new PropValue(), new PropValue());
    }

    @Test
    public void notEquals() {
        assertFalse(new PropValue("hi").equals(new PropValue("you")));
        assertFalse(new PropValue(123).equals(new PropValue("123")));
        assertFalse(new PropValue(123).equals(123));
        assertFalse(new PropValue(123).equals("123"));
        assertFalse(new PropValue("hi").equals("hi"));
        assertFalse(new PropValue().equals(new PropValue(0)));
        assertFalse(new PropValue().equals(0));
        assertFalse(new PropValue().equals(null));
    }

    @Test
    public void notSame() {
        assertFalse(new PropValue("hi") == new PropValue("hi"));
        assertFalse(new PropValue(123) == new PropValue(123));
        assertFalse(new PropValue() == new PropValue());
    }
}
