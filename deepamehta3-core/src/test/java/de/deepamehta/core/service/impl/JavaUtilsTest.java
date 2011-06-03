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
}
