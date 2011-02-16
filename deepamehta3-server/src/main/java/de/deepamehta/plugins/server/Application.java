package de.deepamehta.plugins.server;

import de.deepamehta.plugins.server.provider.PropertiesProvider;
import de.deepamehta.plugins.server.provider.TopicProvider;
import de.deepamehta.plugins.server.provider.TopicListProvider;
import de.deepamehta.plugins.server.provider.RelatedTopicListProvider;
import de.deepamehta.plugins.server.resources.RelationResource;
import de.deepamehta.plugins.server.resources.TopicTypeResource;
import de.deepamehta.plugins.server.resources.PluginResource;
import de.deepamehta.plugins.server.resources.CommandResource;

import de.deepamehta.core.osgi.Activator;

import java.util.HashSet;
import java.util.Set;



public class Application extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set classes = new HashSet();
        // root resource classes
        classes.add(RelationResource.class);
        classes.add(TopicTypeResource.class);
        classes.add(PluginResource.class);
        classes.add(CommandResource.class);
        // provider classes
        classes.add(TopicProvider.class);
        classes.add(TopicListProvider.class);
        classes.add(RelatedTopicListProvider.class);
        classes.add(PropertiesProvider.class);
        return classes;
    }

    @Override
    public Set getSingletons() {
        Set singletons = new HashSet();
        singletons.add(Activator.getService());
        return singletons;
    }
}
