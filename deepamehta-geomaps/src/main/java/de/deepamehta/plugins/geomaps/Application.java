package de.deepamehta.plugins.geomaps;

import de.deepamehta.plugins.webservice.provider.JSONEnabledProvider;
// import de.deepamehta.plugins.topicmaps.provider.RefIdProvider;

import de.deepamehta.core.osgi.Activator;

import java.util.HashSet;
import java.util.Set;



public class Application extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set classes = new HashSet();
        classes.add(JSONEnabledProvider.class);
        // classes.add(RefIdProvider.class);
        return classes;
    }

    @Override
    public Set getSingletons() {
        Set singletons = new HashSet();
        singletons.add(Activator.getService().getPlugin("de.deepamehta.geomaps"));
        return singletons;
    }
}
