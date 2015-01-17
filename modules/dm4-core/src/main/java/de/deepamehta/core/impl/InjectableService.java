package de.deepamehta.core.impl;

import de.deepamehta.core.osgi.PluginContext;
import de.deepamehta.core.service.PluginService;

import java.lang.reflect.Field;



class InjectableService {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private PluginContext pluginContext;
    private Class<? extends PluginService> serviceInterface;
    private Field injectableField;

    private PluginService service;

    // ---------------------------------------------------------------------------------------------------- Constructors

    InjectableService(PluginContext pluginContext, Class<? extends PluginService> serviceInterface,
                                                   Field injectableField) {
        this.pluginContext = pluginContext;
        this.serviceInterface = serviceInterface;
        this.injectableField = injectableField;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String toString() {
        return serviceInterface.getName();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    Class<? extends PluginService> getServiceInterface() {
        return serviceInterface;
    }

    PluginService getService() {
        if (!isServiceAvailable()) {
            throw new RuntimeException("Service " + this + " is not available (consumed by " + pluginContext + ")");
        }
        return service;
    }

    boolean isServiceAvailable() {
        return service != null;
    }

    void injectService(PluginService service) {
        this.service = service;
        injectValue(service);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void injectValue(Object value) {
        try {
            injectableField.set(pluginContext, value);  // throws IllegalAccessException
        } catch (Exception e) {
            throw new RuntimeException("Injecting " + (value == null ? "null for " : "") + this + " failed", e);
        }
    }
}
