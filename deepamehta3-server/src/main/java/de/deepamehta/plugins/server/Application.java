package de.deepamehta.plugins.server;

import de.deepamehta.plugins.server.provider.CommandParamsProvider;
import de.deepamehta.plugins.server.provider.CommandResultProvider;
import de.deepamehta.plugins.server.provider.PluginInfoProvider;
import de.deepamehta.plugins.server.provider.RelatedTopicListProvider;
import de.deepamehta.plugins.server.provider.AssociationProvider;
import de.deepamehta.plugins.server.provider.AssociationListProvider;
import de.deepamehta.plugins.server.provider.StringListProvider;
import de.deepamehta.plugins.server.provider.StringSetProvider;
import de.deepamehta.plugins.server.provider.TopicProvider;
import de.deepamehta.plugins.server.provider.TopicDataProvider;
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
        classes.add(TopicDataProvider.class);
        classes.add(TopicListProvider.class);
        classes.add(RelatedTopicListProvider.class);
        classes.add(AssociationProvider.class);
        classes.add(AssociationListProvider.class);
        classes.add(TopicTypeProvider.class);
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
