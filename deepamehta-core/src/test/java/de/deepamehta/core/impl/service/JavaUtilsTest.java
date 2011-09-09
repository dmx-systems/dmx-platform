package de.deepamehta.core.impl.service;

import de.deepamehta.core.util.JavaUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;



public class JavaUtilsTest {

    @Test
    public void stripHTML() {
        String html = "<body><p><i>Hi</i> there!</p><p style=\"margin-top: 10px\">2. paragraph</p></body>";
        assertEquals("Hi there!2. paragraph", JavaUtils.stripHTML(html));
    }

    @Test
    public void stripHTMLwithLinebreaks() {
        String html = "<p>abc 123</p>\n<p>def</p>\n<p>ghi</p>";
        assertEquals("abc 123\ndef\nghi", JavaUtils.stripHTML(html));
    }

    // ---

    @Test
    public void isInRange() {
        assertTrue(JavaUtils.isInRange("172.68.8.0",   "172.68.8.0/24"));
        assertTrue(JavaUtils.isInRange("172.68.8.12",  "172.68.8.0/24"));
        assertTrue(JavaUtils.isInRange("172.68.8.255", "172.68.8.0/24"));
        assertFalse(JavaUtils.isInRange("172.68.9.0",   "172.68.8.0/24"));
        assertFalse(JavaUtils.isInRange("172.68.9.12",  "172.68.8.0/24"));
        assertFalse(JavaUtils.isInRange("172.68.9.255", "172.68.8.0/24"));
        assertTrue(JavaUtils.isInRange( "100.68.8.113", "100.68.8.113/32"));
        assertFalse(JavaUtils.isInRange("100.68.8.112", "100.68.8.113/32"));
        assertFalse(JavaUtils.isInRange("100.68.8.114", "100.68.8.113/32"));
    }

    @Test
    public void inetAddress() {
        assertEquals(  0, JavaUtils.inetAddress("0.0.0.0"));
        assertEquals(  1, JavaUtils.inetAddress("0.0.0.1"));
        assertEquals(256, JavaUtils.inetAddress("0.0.1.0"));
        assertEquals((int)  Math.pow(2, 24) - 1,  JavaUtils.inetAddress("0.255.255.255"));
        assertEquals((int) (Math.pow(2, 31) - 1), JavaUtils.inetAddress("127.255.255.255"));
        assertEquals((int) -Math.pow(2, 31),      JavaUtils.inetAddress("128.0.0.0"));
        assertEquals(-1, JavaUtils.inetAddress("255.255.255.255"));
    }

    @Test
    public void networkMask() {
        assertEquals(JavaUtils.inetAddress("255.255.255.255"), JavaUtils.networkMask(0));
        assertEquals(JavaUtils.inetAddress("128.0.0.0"),       JavaUtils.networkMask(1));
        assertEquals(JavaUtils.inetAddress("255.0.0.0"),       JavaUtils.networkMask(8));
        assertEquals(JavaUtils.inetAddress("255.255.0.0"),     JavaUtils.networkMask(16));
        assertEquals(JavaUtils.inetAddress("255.255.255.0"),   JavaUtils.networkMask(24));
        assertEquals(JavaUtils.inetAddress("255.255.255.192"), JavaUtils.networkMask(26));
        assertEquals(JavaUtils.inetAddress("255.255.255.255"), JavaUtils.networkMask(32));
    }
}
