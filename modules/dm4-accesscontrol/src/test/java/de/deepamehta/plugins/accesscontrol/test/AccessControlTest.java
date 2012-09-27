package de.deepamehta.plugins.accesscontrol.test;

import de.deepamehta.plugins.accesscontrol.service.AccessControlService;

import static org.junit.Assert.assertEquals;

import org.junit.Test;



public class AccessControlTest {

    @Test
    public void defaultUsername() {
        assertEquals("admin", AccessControlService.DEFAULT_USERNAME);
    }
}
