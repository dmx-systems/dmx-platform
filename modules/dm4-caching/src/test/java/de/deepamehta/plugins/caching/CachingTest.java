package de.deepamehta.plugins.caching;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.logging.Logger;



public class CachingTest {

    private Logger logger = Logger.getLogger(getClass().getName());

    @Test
    public void regex() {
        String CACHABLE_PATH = "core/(topic|association)/(\\d+)";
        assertTrue("core/topic/2695".matches(CACHABLE_PATH));
        assertFalse("/core/topic/2695".matches(CACHABLE_PATH));
        assertFalse("base/core/topic/2695".matches(CACHABLE_PATH));
        assertFalse("core/topic/2695?".matches(CACHABLE_PATH));
        assertFalse("core/topic/2695?fetch_composite=false".matches(CACHABLE_PATH));
    }

    @Test
    public void date() {
        logger.info("### Date(0)=" + new Date(0) + "\n          " +
                        "Date(-1)=" + new Date(-1));
    }
}
