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

    /*
        We always create a *subclass* instance.
        The difference is whether the holding variable is declared as of the base type, or the sub type.
        --> Result: always the subclass's method is invoked.
    */

    @Test
    public void methodDispatch1() {
        BaseClass o = new SubClass();
        String res = o.hi();
        assertSame("sub", res);
    }

    @Test
    public void methodDispatch2() {
        SubClass o = new SubClass();
        String res = o.hi();
        assertSame("sub", res);
    }

    /*
        We always create a *subclass* instance and a *parameter subclass* instance.
        The difference is whether the holding variables are declared as of the respective base types, or the sub types.
        --> Result: the subclass's method is only invoked if *both* variables are declared as of the sub types.
    */

    @Test
    public void missingMultipleDispatch1() {
        BC o = new SC();
        PBC p = new PSC();
        String res = o.hi(p);
        assertSame("base", res);
    }

    @Test
    public void missingMultipleDispatch2() {
        BC o = new SC();
        PSC p = new PSC();
        String res = o.hi(p);
        assertSame("base", res);
    }

    @Test
    public void missingMultipleDispatch3() {
        SC o = new SC();
        PBC p = new PSC();
        String res = o.hi(p);
        assertSame("base", res);
    }

    @Test
    public void missingMultipleDispatch4() {
        SC o = new SC();
        PSC p = new PSC();
        String res = o.hi(p);
        assertSame("sub", res);
    }
}

// ---

class BaseClass {

    String hi() {
        return "base";
    }
}

class SubClass extends BaseClass {

    String hi() {
        return "sub";
    }
}

// ---

// base class
class BC {

    String hi(PBC param) {
        return "base";
    }
}

// subclass
class SC extends BC {

    String hi(PSC param) {
        return "sub";
    }
}

// parameter base class
class PBC {
}

// parameter subclass
class PSC extends PBC {
}
