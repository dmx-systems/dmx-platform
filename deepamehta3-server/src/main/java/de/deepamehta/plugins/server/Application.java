package de.deepamehta.plugins.server;

import de.deepamehta.plugins.server.provider.CommandParamsProvider;
import de.deepamehta.plugins.server.provider.CommandResultProvider;
import de.deepamehta.plugins.server.provider.DataFieldProvider;
import de.deepamehta.plugins.server.provider.PluginInfoProvider;
import de.deepamehta.plugins.server.provider.RelatedTopicListProvider;
import de.deepamehta.plugins.server.provider.RelationProvider;
import de.deepamehta.plugins.server.provider.RelationListProvider;
import de.deepamehta.plugins.server.provider.StringListProvider;
import de.deepamehta.plugins.server.provider.StringSetProvider;
import de.deepamehta.plugins.server.provider.TopicProvider;
import de.deepamehta.plugins.server.provider.TopicListProvider;
import de.deepamehta.plugins.server.provider.TopicTypeProvider;

import de.deepamehta.core.osgi.Activator;

import java.util.HashSet;
import java.util.Set;



public class Application extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set classes = new HashSet();
        // provider classes
        classes.add(TopicProvider.class);
        classes.add(TopicListProvider.class);
        classes.add(RelatedTopicListProvider.class);
        classes.add(RelationProvider.class);
        classes.add(RelationListProvider.class);
        classes.add(TopicTypeProvider.class);
        classes.add(DataFieldProvider.class);
        classes.add(StringListProvider.class);
        classes.add(StringSetProvider.class);
        classes.add(CommandParamsProvider.class);
        classes.add(CommandResultProvider.class);
        classes.add(PluginInfoProvider.class);
        return classes;
    }

    @Override
    public Set getSingletons() {
        Set singletons = new HashSet();
        singletons.add(Activator.getService());
        return singletons;
    }
}
