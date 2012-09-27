package de.deepamehta.core.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

import de.deepamehta.core.Identifiable;
import de.deepamehta.core.model.TopicModel;

public class DeepaMehtaUtilsTest {

    @Test
    public void castClass() throws Exception {
        Object expected = new TopicModel(17);
        Identifiable actual = DeepaMehtaUtils.cast(expected);
        assertEquals(17, actual.getId());
        assertEquals(expected, actual);
    }

    @Test
    public void castCollection() throws Exception {
        Object expected = new ArrayList<String>(Arrays.asList("check"));
        Collection<String> actual = DeepaMehtaUtils.cast(expected);
        for (String string : actual) {
            assertEquals("check", string);
        }
        assertEquals(expected, actual);
    }
}
