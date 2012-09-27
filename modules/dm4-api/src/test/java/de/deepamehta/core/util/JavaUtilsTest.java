package de.deepamehta.core.util;

import de.deepamehta.core.util.JavaUtils;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    public void stripDriveLetter() {
        assertEquals("/my/path", JavaUtils.stripDriveLetter("/my/path"));
        assertEquals("/my/path", JavaUtils.stripDriveLetter("A:/my/path"));
        assertEquals("/my/A:path", JavaUtils.stripDriveLetter("/my/A:path"));
    }

    // ---

    @Test
    public void isInRangeIPv4() {
        assertTrue(JavaUtils.isInRange("0.0.0.0",         "0.0.0.0/0"));
        assertTrue(JavaUtils.isInRange("255.255.255.255", "0.0.0.0/0"));
        assertTrue(JavaUtils.isInRange("0.0.0.0",         "127.0.0.1/0"));
        assertTrue(JavaUtils.isInRange("255.255.255.255", "127.0.0.1/0"));
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
    public void inetAddressIPv4() {
        assertEquals(BigInteger.ZERO,             JavaUtils.inetAddress("0.0.0.0"));
        assertEquals(BigInteger.ONE,              JavaUtils.inetAddress("0.0.0.1"));
        assertEquals(bigInt(256),                 JavaUtils.inetAddress("0.0.1.0"));
        assertEquals(bigInt(Math.pow(2, 24) - 1), JavaUtils.inetAddress("0.255.255.255"));
        assertEquals(bigInt(Math.pow(2, 31) - 1), JavaUtils.inetAddress("127.255.255.255"));
        assertEquals(bigInt(Math.pow(2, 31)),     JavaUtils.inetAddress("128.0.0.0"));
        assertEquals(bigInt(Math.pow(2, 32) - 1), JavaUtils.inetAddress("255.255.255.255"));
    }

    @Test
    public void networkMaskIPv4() {
        assertEquals(JavaUtils.inetAddress("0.0.0.0"),         JavaUtils.networkMask(0, 32));
        assertEquals(JavaUtils.inetAddress("128.0.0.0"),       JavaUtils.networkMask(1, 32));
        assertEquals(JavaUtils.inetAddress("255.0.0.0"),       JavaUtils.networkMask(8, 32));
        assertEquals(JavaUtils.inetAddress("255.255.0.0"),     JavaUtils.networkMask(16, 32));
        assertEquals(JavaUtils.inetAddress("255.255.255.0"),   JavaUtils.networkMask(24, 32));
        assertEquals(JavaUtils.inetAddress("255.255.255.192"), JavaUtils.networkMask(26, 32));
        assertEquals(JavaUtils.inetAddress("255.255.255.255"), JavaUtils.networkMask(32, 32));
    }

    // ---

    @Test
    public void isInRangeIPv6() {
        assertTrue(JavaUtils.isInRange("::3afe:7a0:c800", "::3afe:7a0:c800/120"));
        assertTrue(JavaUtils.isInRange("::3afe:7a0:c880", "::3afe:7a0:c800/120"));
        assertTrue(JavaUtils.isInRange("::3afe:7a0:c8ff", "::3afe:7a0:c800/120"));
        assertTrue(JavaUtils.isInRange("::3afe:7a0:c800",  "::3afe:7a0:c800/121"));
        assertTrue(JavaUtils.isInRange("::3afe:7a0:c87f",  "::3afe:7a0:c800/121"));
        assertFalse(JavaUtils.isInRange("::3afe:7a0:c880", "::3afe:7a0:c800/121"));
        assertFalse(JavaUtils.isInRange("::3afe:7a0:c8ff", "::3afe:7a0:c800/121"));
    }

    @Test
    public void inetAddressIPv6() {
        assertEquals(BigInteger.ZERO,             JavaUtils.inetAddress("::"));
        assertEquals(BigInteger.ONE,              JavaUtils.inetAddress("::1"));
        assertEquals(bigInt(256),                 JavaUtils.inetAddress("::100"));
        assertEquals(bigInt(Math.pow(2, 24) - 1), JavaUtils.inetAddress("::ff:ffff"));
        assertEquals(bigInt(Math.pow(2, 31) - 1), JavaUtils.inetAddress("::7fff:ffff"));
        assertEquals(bigInt(Math.pow(2, 31)),     JavaUtils.inetAddress("::8000:0000"));
        assertEquals(bigInt(Math.pow(2, 32) - 1), JavaUtils.inetAddress("::ffff:ffff"));
        assertEquals(bigInt(Math.pow(2, 32)),     JavaUtils.inetAddress("::1:0000:0000"));
        assertEquals(bigInt(Math.pow(2, 63) - 1), JavaUtils.inetAddress("::7fff:ffff:ffff:ffff"));
    }

    @Test
    public void networkMaskIPv6() {
        assertEquals(JavaUtils.inetAddress("::"),          JavaUtils.networkMask(0, 128));
        assertEquals(JavaUtils.inetAddress("8000::"),      JavaUtils.networkMask(1, 128));
        assertEquals(JavaUtils.inetAddress("ff00::"),      JavaUtils.networkMask(8, 128));
        assertEquals(JavaUtils.inetAddress("ffff::"),      JavaUtils.networkMask(16, 128));
        assertEquals(JavaUtils.inetAddress("ffff:ff00::"), JavaUtils.networkMask(24, 128));
        assertEquals(JavaUtils.inetAddress("ffff:ffc0::"), JavaUtils.networkMask(26, 128));
        assertEquals(JavaUtils.inetAddress("ffff:ffff::"), JavaUtils.networkMask(32, 128));
        assertEquals(JavaUtils.inetAddress("ffff:ffff:ffff:ffff::"), JavaUtils.networkMask(64, 128));
        assertEquals(JavaUtils.inetAddress("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"), JavaUtils.networkMask(128, 128));
    }

    // ---

    private BigInteger bigInt(double val) {
        return bigInt((long) val);
    }

    private BigInteger bigInt(long val) {
        return new BigInteger(Long.toString(val));
    }
}
