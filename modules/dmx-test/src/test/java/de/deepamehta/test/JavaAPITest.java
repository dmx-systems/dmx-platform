package de.deepamehta.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    public void stringEquality() {
        String a = "Hallo";
        String b = "Hallo";
        assertTrue(a == b);
        String c = a;
        assertTrue(a == c);
        String d = "Hal" + "lo";
        assertTrue(a == d);
        String e = "Hall" + ((char) 111);
        assertTrue(a == e);
        int o = 111;
        String f = "Hall" + ((char) o);
        assertFalse(a == f);
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
        TestClass has a overloaded method, one with a parameter baseclass, one with a parameter subclass.
        In both test cases we create a parameter object of the *subclass*.
        The difference is whether the holding variable is declared as of the base type, or the sub type.
        --> Result: if the parameter object is declared as base-type the base method is invoked!
        --> Method overloading involves NO dynamic dispatch! The method to be invoked is determined at **compile time**.
    */

    class TestClass {

        String hi(PBC param) {
            return "base";
        }

        String hi(PSC param) {
            return "sub";
        }
    }

    @Test
    public void methodOverloading1() {
        TestClass t = new TestClass();
        PBC p = new PSC();
        String res = t.hi(p);
        assertSame("base", res);
    }

    @Test
    public void methodOverloading2() {
        TestClass t = new TestClass();
        PSC p = new PSC();
        String res = t.hi(p);
        assertSame("sub", res);
    }

    /*
        SubClass overrides a method from the BaseClass.
        In both test cases we create a *subclass* instance.
        The difference is whether the holding variable is declared as of the base type, or the sub type.
        --> Result: always the subclass's method is invoked.
        --> Method overriding involves dynamic dispatch. The method to be invoked is determined at **runtime**.
    */

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

    @Test
    public void methodOverriding1() {
        BaseClass o = new SubClass();
        String res = o.hi();
        assertSame("sub", res);
    }

    @Test
    public void methodOverriding2() {
        SubClass o = new SubClass();
        String res = o.hi();
        assertSame("sub", res);
    }

    /*
        The subclass (SC) overrides a method from the baseclass (BC) with an subclassed parameter.
        In all 4 test cases we create a *subclass* instance and a *parameter subclass* instance.
        The difference is whether the holding variables are declared as of the respective base types, or the sub types.
        --> Result: the subclass's method is only invoked if *both* variables are declared as of the sub types.
    */

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

// parameter base class
class PBC {
}

// parameter subclass
class PSC extends PBC {
}
