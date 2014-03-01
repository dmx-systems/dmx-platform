package de.deepamehta.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



public class JavaAPITest {

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---

    @Test
    public void asListWithNullArgument() {
        try {
            List l = Arrays.asList(null);
            fail();
        } catch (NullPointerException e) {
        }
        // => asList can't take a null argument
    }

    @Test
    public void asListWithNoArguments() {
        List l = Arrays.asList();
        assertSame(0, l.size());
    }

    // ---

    @Test
    public void split() {
        String[] a = "abc=123".split("=");
        assertSame(2, a.length);
    }

    @Test
    public void splitWithTrailingEmpty() {
        String[] a = "abc=".split("=");
        assertSame(1, a.length);
    }

    @Test
    public void splitWithTrailingEmptyAndLimit() {
        String[] a = "abc=".split("=", 2);
        assertSame(2, a.length);
        assertEquals("", a[1]);
    }
}
