package de.deepamehta.core.osgi;

import de.deepamehta.core.service.DeepaMehtaService;

import org.osgi.framework.BundleContext;



public interface PluginContext {

    BundleContext getBundleContext();

    void setCoreService(DeepaMehtaService dms);
}
