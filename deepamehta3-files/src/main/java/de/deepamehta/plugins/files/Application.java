package de.deepamehta.plugins.files;

import de.deepamehta.plugins.files.provider.DirectoryListingProvider;
import de.deepamehta.plugins.files.provider.ResourceProvider;
import de.deepamehta.plugins.files.provider.ResourceInfoProvider;

import de.deepamehta.core.osgi.Activator;

import java.util.HashSet;
import java.util.Set;



public class Application extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set classes = new HashSet();
        classes.add(DirectoryListingProvider.class);
        classes.add(ResourceProvider.class);
        classes.add(ResourceInfoProvider.class);
        return classes;
    }

    @Override
    public Set getSingletons() {
        Set singletons = new HashSet();
        singletons.add(Activator.getService().getPlugin("de.deepamehta.3-files"));
        return singletons;
    }
}
