package de.deepamehta.plugins.accesscontrol;

import de.deepamehta.plugins.accesscontrol.provider.PermissionsProvider;
import de.deepamehta.plugins.webservice.provider.JSONEnabledProvider;

import de.deepamehta.core.osgi.Activator;

import java.util.HashSet;
import java.util.Set;



public class Application extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set classes = new HashSet();
        classes.add(PermissionsProvider.class);
        classes.add(JSONEnabledProvider.class);
        return classes;
    }

    @Override
    public Set getSingletons() {
        Set singletons = new HashSet();
        singletons.add(Activator.getService().getPlugin("de.deepamehta.accesscontrol"));
        return singletons;
    }
}
