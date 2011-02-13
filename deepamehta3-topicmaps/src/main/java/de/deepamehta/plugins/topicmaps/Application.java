package de.deepamehta.plugins.topicmaps;

import de.deepamehta.plugins.topicmaps.provider.TopicmapProvider;
import de.deepamehta.plugins.topicmaps.provider.RefIdProvider;

import de.deepamehta.core.osgi.Activator;

import java.util.HashSet;
import java.util.Set;



public class Application extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set classes = new HashSet();
        classes.add(TopicmapProvider.class);
        classes.add(RefIdProvider.class);
        return classes;
    }

    @Override
    public Set getSingletons() {
        Set singletons = new HashSet();
        singletons.add(Activator.getService().getPlugin("de.deepamehta.3-topicmaps"));
        return singletons;
    }
}
