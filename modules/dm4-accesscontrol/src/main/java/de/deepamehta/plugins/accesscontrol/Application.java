package de.deepamehta.plugins.accesscontrol;

import de.deepamehta.plugins.accesscontrol.provider.PermissionsProvider;
import de.deepamehta.plugins.webservice.provider.JSONEnabledProvider;

import de.deepamehta.core.osgi.Activator;

import java.util.HashSet;
import java.util.Set;



public class Application extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(PermissionsProvider.class);
        classes.add(JSONEnabledProvider.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<Object>();
        singletons.add(Activator.getService().getPlugin("de.deepamehta.accesscontrol"));
        return singletons;
    }
}
