package de.deepamehta.plugins.webservice;

import de.deepamehta.plugins.webservice.provider.AssociationProvider;
import de.deepamehta.plugins.webservice.provider.AssociationTypeProvider;
import de.deepamehta.plugins.webservice.provider.CommandParamsProvider;
import de.deepamehta.plugins.webservice.provider.CommandResultProvider;
import de.deepamehta.plugins.webservice.provider.DirectivesProvider;
import de.deepamehta.plugins.webservice.provider.JSONEnabledCollectionProvider;
import de.deepamehta.plugins.webservice.provider.JSONEnabledProvider;
import de.deepamehta.plugins.webservice.provider.PluginInfoProvider;
import de.deepamehta.plugins.webservice.provider.RelatedTopicCollectionProvider;
import de.deepamehta.plugins.webservice.provider.StringListProvider;
import de.deepamehta.plugins.webservice.provider.StringSetProvider;
import de.deepamehta.plugins.webservice.provider.TopicProvider;
import de.deepamehta.plugins.webservice.provider.TopicTypeProvider;

import de.deepamehta.core.osgi.Activator;

import java.util.HashSet;
import java.util.Set;



public class Application extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set classes = new HashSet();
        // provider classes
        classes.add(AssociationProvider.class);
        classes.add(AssociationTypeProvider.class);
        classes.add(CommandParamsProvider.class);
        classes.add(CommandResultProvider.class);
        classes.add(DirectivesProvider.class);
        classes.add(JSONEnabledCollectionProvider.class);
        classes.add(JSONEnabledProvider.class);
        classes.add(PluginInfoProvider.class);
        classes.add(RelatedTopicCollectionProvider.class);
        classes.add(StringListProvider.class);
        classes.add(StringSetProvider.class);
        classes.add(TopicProvider.class);
        classes.add(TopicTypeProvider.class);
        return classes;
    }

    @Override
    public Set getSingletons() {
        Set singletons = new HashSet();
        singletons.add(Activator.getService());
        return singletons;
    }
}
