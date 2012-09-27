package de.deepamehta.core.util;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.Arrays;



public class JavaAPITest {

    @Test
    public void asListWithNullArgument() {
        try {
            Arrays.asList((Object[])null);
            fail();
        } catch (NullPointerException e) {
            // => asList can't take a null argument
        }
    }

    @Test
    public void asListWithNoArguments() {
        assertSame(0, Arrays.asList().size());
    }
}
