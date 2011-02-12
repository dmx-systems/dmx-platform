package de.deepamehta.plugins.client;

import de.deepamehta.plugins.server.provider.TopicProvider;

import de.deepamehta.core.osgi.Activator;

import java.util.HashSet;
import java.util.Set;



public class Application extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set classes = new HashSet();
        classes.add(TopicProvider.class);
        return classes;
    }

    @Override
    public Set getSingletons() {
        Set singletons = new HashSet();
        singletons.add(Activator.getService().getPlugin("de.deepamehta.3-client"));
        return singletons;
    }
}
