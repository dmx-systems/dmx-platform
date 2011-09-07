package de.deepamehta.plugins.files;

import de.deepamehta.core.osgi.Activator;

import java.util.HashSet;
import java.util.Set;



public class Application extends javax.ws.rs.core.Application {

    @Override
    public Set getSingletons() {
        Set singletons = new HashSet();
        singletons.add(Activator.getService().getPlugin("de.deepamehta.files"));
        return singletons;
    }
}
