package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.osgi.PluginContext;
import de.deepamehta.core.service.CoreService;
import de.deepamehta.core.service.DMXEvent;
import de.deepamehta.core.service.EventListener;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.ModelFactory;
import de.deepamehta.core.service.Plugin;
import de.deepamehta.core.service.PluginInfo;
import de.deepamehta.core.storage.spi.DMXTransaction;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.util.tracker.ServiceTracker;

import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;



// ### TODO: refactoring? This class does too much.
// ### It lies about its dependencies. It depends on dm4 but dm4 is not passed to constructor.
public class PluginImpl implements Plugin, EventHandler {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String PLUGIN_DEFAULT_PACKAGE = "de.deepamehta.core.osgi";
    private static final String PLUGIN_CONFIG_FILE = "/plugin.properties";
    private static final String PLUGIN_ACTIVATED = "de/deepamehta/core/plugin_activated";   // topic of the OSGi event

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private PluginContext pluginContext;
    private BundleContext bundleContext;

    private Bundle       pluginBundle;
    private String       pluginUri;             // This bundle's symbolic name, e.g. "systems.dmx.webclient"

    private Properties   pluginProperties;      // Read from file "plugin.properties"
    private String       pluginPackage;
    private PluginInfo   pluginInfo;
    private List<String> pluginDependencies;    // plugin URIs as read from "dm4.plugin.activate_after" property
    private Topic        pluginTopic;           // Represents this plugin in DB. Holds plugin migration number.

    // Consumed services (DMX Core and OSGi)
    private CoreServiceImpl dm4;
    private ModelFactory mf;
    private EventAdmin eventService;            // needed to post the PLUGIN_ACTIVATED OSGi event

    // Consumed services (injected)
    //      key: service interface (a class object),
    //      value: an InjectableService
    private Map<Class<?>, InjectableService> injectableServices = new HashMap();

    // Trackers for the consumed services (DMX Core, OSGi, and injected services)
    private List<ServiceTracker> serviceTrackers = new ArrayList();

    // Provided OSGi service
    private String providedServiceInterface;

    // Provided resources
    private StaticResourcesPublication webResources;
    private StaticResourcesPublication fileSystemResources;
    private RestResourcesPublication restResources;

    private Logger logger = Logger.getLogger(getClass().getName());



    // ---------------------------------------------------------------------------------------------------- Constructors

    public PluginImpl(PluginContext pluginContext) {
        this.pluginContext = pluginContext;
        this.bundleContext = pluginContext.getBundleContext();
        //
        this.pluginBundle = bundleContext.getBundle();
        this.pluginUri = pluginBundle.getSymbolicName();
        //
        this.pluginProperties = readConfigFile();
        this.pluginPackage = getConfigProperty("dm4.plugin.main_package", pluginContext.getClass().getPackage()
            .getName());
        this.pluginInfo = new PluginInfoImpl(pluginUri, pluginBundle);
        this.pluginDependencies = pluginDependencies();
        //
        this.providedServiceInterface = providedServiceInterface();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public void start() {
        if (pluginDependencies.size() > 0) {
            registerPluginActivatedEventListener();
        }
        //
        createCoreServiceTrackers();
        createInjectedServiceTrackers();
        //
        openServiceTrackers();
    }

    public void stop() {
        // Note: plugins often use the shutdown() hook to unregister things at certain services.
        // So the shutdown() hook must be invoked _before_ the service trackers are closed.
        invokeShutdownHook();
        closeServiceTrackers();
    }

    // ---

    /**
     * Publishes a directory of the server's file system.
     *
     * @param   path    An absolute path to the directory to be published.
     */
    public void publishFileSystem(String uriNamespace, String path) {
        try {
            logger.info("### Publishing file system \"" + path + "\" at URI namespace \"" + uriNamespace + "\"");
            //
            if (fileSystemResources != null) {
                throw new RuntimeException(this + " has already published file system resources; " +
                    "only one directory per plugin is supported");
            }
            //
            fileSystemResources = dm4.wpService.publishFileSystem(uriNamespace, path);
        } catch (Exception e) {
            throw new RuntimeException("Publishing file system \"" + path + "\" at URI namespace \"" + uriNamespace +
                "\" failed", e);
        }
    }

    // ---

    public String getUri() {
        return pluginUri;
    }

    // --- Plugin Implementation ---

    @Override
    public InputStream getStaticResource(String name) {
        try {
            // We always use the plugin bundle's class loader to access the resource.
            // getClass().getResource() would fail for generic plugins (plugin bundles not containing a plugin
            // subclass) because the core bundle's class loader would be used and it has no access.
            URL url = pluginBundle.getResource(name);
            //
            if (url == null) {
                throw new RuntimeException("Resource \"" + name + "\" not found");
            }
            //
            return url.openStream();    // throws IOException
        } catch (Exception e) {
            throw new RuntimeException("Accessing a static resource of " + this + " failed", e);
        }
    }

    @Override
    public boolean hasStaticResource(String name) {
        return getBundleEntry(name) != null;
    }

    // ---

    @Override
    public String toString() {
        return pluginContext.toString();
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    PluginInfo getInfo() {
        return pluginInfo;
    }

    PluginContext getContext() {
        return pluginContext;
    }

    Topic getPluginTopic() {
        return pluginTopic;
    }

    String getProvidedServiceInterface() {
        return providedServiceInterface;
    }

    // ---

    /**
     * Returns a plugin configuration property (as read from file "plugin.properties")
     * or <code>null</code> if no such property exists.
     */
    String getConfigProperty(String key) {
        return getConfigProperty(key, null);
    }

    String getConfigProperty(String key, String defaultValue) {
        return pluginProperties.getProperty(key, defaultValue);
    }

    // ---

    /**
     * Returns the migration class name for the given migration number.
     *
     * @return  the fully qualified migration class name, or <code>null</code> if the migration package is unknown.
     *          This is the case if the plugin bundle contains no Plugin subclass and the "dm4.plugin.main_package"
     *          config property is not set.
     */
    String getMigrationClassName(int migrationNr) {
        if (pluginPackage.equals(PLUGIN_DEFAULT_PACKAGE)) {
            return null;    // migration package is unknown
        }
        //
        return pluginPackage + ".migrations.Migration" + migrationNr;
    }

    void setMigrationNr(int migrationNr) {
        pluginTopic.getChildTopics().set("dm4.core.plugin_migration_nr", migrationNr);
    }

    // ---

    /**
     * Uses the plugin bundle's class loader to load a class by name.
     *
     * @return  the class, or <code>null</code> if the class is not found.
     */
    Class loadClass(String className) {
        try {
            return pluginBundle.loadClass(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Config Properties ===

    private Properties readConfigFile() {
        try {
            Properties properties = new Properties();
            //
            if (!hasStaticResource(PLUGIN_CONFIG_FILE)) {
                logger.info("Reading config file \"" + PLUGIN_CONFIG_FILE + "\" for " + this + " SKIPPED " +
                    "-- file does not exist");
                return properties;
            }
            //
            logger.info("Reading config file \"" + PLUGIN_CONFIG_FILE + "\" for " + this);
            properties.load(getStaticResource(PLUGIN_CONFIG_FILE));
            return properties;
        } catch (Exception e) {
            throw new RuntimeException("Reading config file \"" + PLUGIN_CONFIG_FILE + "\" for " + this + " failed", e);
        }
    }



    // === Service Tracking ===

    private void createCoreServiceTrackers() {
        serviceTrackers.add(createServiceTracker(CoreService.class));
        serviceTrackers.add(createServiceTracker(EventAdmin.class));
    }

    private void createInjectedServiceTrackers() {
        List<InjectableService> injectableServices = createInjectableServices();
        //
        if (injectableServices.isEmpty()) {
            logger.info("Tracking services for " + this + " SKIPPED -- no services consumed");
            return;
        }
        //
        logger.info("Tracking " + injectableServices.size() + " services for " + this + " " + injectableServices);
        for (InjectableService injectableService : injectableServices) {
            Class<?> serviceInterface = injectableService.getServiceInterface();
            this.injectableServices.put(serviceInterface, injectableService);
            serviceTrackers.add(createServiceTracker(serviceInterface));
        }
    }

    // ---

    private List<InjectableService> createInjectableServices() {
        List<InjectableService> injectableServices = new ArrayList();
        //
        for (Field field : getInjectableFields(pluginContext.getClass())) {
            Class<?> serviceInterface = field.getType();
            injectableServices.add(new InjectableService(pluginContext, serviceInterface, field));
        }
        return injectableServices;
    }

    private boolean injectedServicesAvailable() {
        for (InjectableService injectableService : injectableServices.values()) {
            if (!injectableService.isServiceAvailable()) {
                return false;
            }
        }
        return true;
    }

    // ---

    // called also from MigrationManager
    static List<Field> getInjectableFields(Class<?> clazz) {
        List<Field> injectableFields = new ArrayList();
        // Note: we use getDeclaredFields() (instead of getFields()) to *not* search the super classes
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                field.setAccessible(true);  // allow injection into private fields
                injectableFields.add(field);
            }
        }
        return injectableFields;
    }

    // called from MigrationManager
    Object getInjectedService(Class<?> serviceInterface) {
        InjectableService injectableService = injectableServices.get(serviceInterface);
        if (injectableService == null) {
            throw new RuntimeException("Service " + serviceInterface.getName() + " is not tracked by " + this);
        }
        return injectableService.getService();
    }

    // ---

    private ServiceTracker createServiceTracker(final Class serviceInterface) {
        //
        return new ServiceTracker(bundleContext, serviceInterface.getName(), null) {

            @Override
            public Object addingService(ServiceReference serviceRef) {
                Object service = null;
                try {
                    service = super.addingService(serviceRef);
                    addService(service, serviceInterface);
                } catch (Throwable e) {
                    logger.log(Level.SEVERE, "An error occurred while adding service " + serviceInterface.getName() +
                        " to " + pluginContext + ":", e);
                    // Note: here we catch anything, also errors (like NoClassDefFoundError).
                    // If thrown through the OSGi container it would not print out the stacktrace.
                }
                return service;
            }

            @Override
            public void removedService(ServiceReference ref, Object service) {
                try {
                    removeService(service, serviceInterface);
                    super.removedService(ref, service);
                } catch (Throwable e) {
                    logger.log(Level.SEVERE, "An error occurred while removing service " + serviceInterface.getName() +
                        " from " + pluginContext + ":", e);
                    // Note: here we catch anything, also errors (like NoClassDefFoundError).
                    // If thrown through the OSGi container it would not print out the stacktrace.
                }
            }
        };
    }

    // ---

    private void openServiceTrackers() {
        for (ServiceTracker serviceTracker : serviceTrackers) {
            serviceTracker.open();
        }
    }

    private void closeServiceTrackers() {
        for (ServiceTracker serviceTracker : serviceTrackers) {
            serviceTracker.close();
        }
    }

    // ---

    private void addService(Object service, Class serviceInterface) {
        if (service instanceof CoreService) {
            logger.info("Adding DMX core service to " + this);
            setCoreService((CoreServiceImpl) service);
            publishWebResources();
            publishRestResources();
            checkRequirementsForActivation();
        } else if (service instanceof EventAdmin) {
            logger.info("Adding Event Admin service to " + this);
            eventService = (EventAdmin) service;
            checkRequirementsForActivation();
        } else {
            logger.info("Adding " + serviceInterface.getName() + " to " + this);
            injectableServices.get(serviceInterface).injectService(service);
            pluginContext.serviceArrived(service);
            checkRequirementsForActivation();
        }
    }

    private void removeService(Object service, Class serviceInterface) {
        if (service == dm4) {
            logger.info("Removing DMX core service from " + this);
            unpublishRestResources();
            unpublishWebResources();
            unpublishFileSystem();
            dm4.pluginManager.deactivatePlugin(this);   // use plugin manager before core service is removed
            setCoreService(null);
        } else if (service == eventService) {
            logger.info("Removing Event Admin service from " + this);
            eventService = null;
        } else {
            logger.info("Removing " + serviceInterface.getName() + " from " + this);
            pluginContext.serviceGone(service);
            injectableServices.get(serviceInterface).injectService(null);
        }
    }

    // ---

    private void setCoreService(CoreServiceImpl dm4) {
        this.dm4 = dm4;
        this.mf = dm4 != null ? dm4.mf : null;
        pluginContext.setCoreService(dm4);
    }

    // ---

    /**
     * Checks if this plugin's requirements are met, and if so, activates this plugin.
     *
     * The requirements:
     *   - the 2 core services are available (CoreService, EventAdmin).
     *   - the injected services (according to the "Inject" annotation) are available.
     *   - the plugin dependencies (according to the "dm4.plugin.activate_after" config property) are active.
     */
    private void checkRequirementsForActivation() {
        if (dm4 != null && eventService != null && injectedServicesAvailable() && dependenciesAvailable()) {
            dm4.pluginManager.activatePlugin(this);
        }
    }



    // === Activation ===

    /**
     * Activates this plugin and then posts the PLUGIN_ACTIVATED OSGi event.
     *
     * Activation comprises:
     *   - invoke the plugin's preInstall() hook
     *   - install the plugin in the database (includes plugin topic, migrations, type introduction)
     *   - register the plugin's event listeners
     *   - register the plugin's OSGi service
     *   - invoke the plugin's init() hook
     */
    void activate() {
        try {
            logger.info("----- Activating " + this + " -----");
            //
            invokePreInstallHook();
            installPluginInDB();
            registerListeners();
            registerProvidedService();
            invokeInitHook();
            // Note: the event listeners must be registered *after* the plugin is installed in the database (see
            // installPluginInDB() below).
            // Consider the Access Control plugin: it can't set a topic's creator before the "admin" user is created.
            //
            logger.info("----- Activation of " + this + " complete -----");
            //
            postPluginActivatedEvent();
            //
        } catch (Throwable e) {
            throw new RuntimeException("Activating " + this + " failed", e);
        }
    }

    void deactivate() {
        unregisterListeners();
    }



    // === Installation ===

    /**
     * Installs the plugin in the database. This comprises:
     *   1) create "Plugin" topic
     *   2) run migrations
     *   3) type introduction (fires the {@link CoreEvent.INTRODUCE_TOPIC_TYPE} and
     *                                   {@link CoreEvent.INTRODUCE_ASSOCIATION_TYPE} events)
     */
    private void installPluginInDB() {
        DMXTransaction tx = dm4.beginTx();
        try {
            // 1) create "Plugin" topic
            boolean isCleanInstall = createPluginTopicIfNotExists();
            // 2) run migrations
            dm4.migrationManager.runPluginMigrations(this, isCleanInstall);
            // 3) type introduction
            if (isCleanInstall) {
                introduceTopicTypesToPlugin();
                introduceAssociationTypesToPlugin();
            }
            //
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK! (" + this + ")");
            throw new RuntimeException("Installing " + this + " in the database failed", e);
        } finally {
            tx.finish();
        }
    }

    private boolean createPluginTopicIfNotExists() {
        pluginTopic = fetchPluginTopic();
        //
        if (pluginTopic != null) {
            logger.info("Installing " + this + " in the database SKIPPED -- already installed");
            return false;
        }
        //
        logger.info("Installing " + this + " in the database");
        pluginTopic = createPluginTopic();
        return true;
    }

    /**
     * Creates a Plugin topic in the DB.
     * <p>
     * A Plugin topic represents an installed plugin and is used to track its version.
     */
    private Topic createPluginTopic() {
        return dm4.createTopic(mf.newTopicModel(pluginUri, "dm4.core.plugin", mf.newChildTopicsModel()
            .put("dm4.core.plugin_name", pluginName())
            .put("dm4.core.plugin_symbolic_name", pluginUri)
            .put("dm4.core.plugin_migration_nr", 0)
        ));
    }

    private Topic fetchPluginTopic() {
        return dm4.getTopicByUri(pluginUri);
    }

    // ---

    // ### TODO: move to PersistenceLayer?
    private void introduceTopicTypesToPlugin() {
        try {
            for (TopicType topicType : dm4.getAllTopicTypes()) {
                dispatchEvent(CoreEvent.INTRODUCE_TOPIC_TYPE, topicType);
            }
        } catch (Exception e) {
            throw new RuntimeException("Introducing topic types to " + this + " failed", e);
        }
    }

    // ### TODO: move to PersistenceLayer?
    private void introduceAssociationTypesToPlugin() {
        try {
            for (AssociationType assocType : dm4.getAllAssociationTypes()) {
                dispatchEvent(CoreEvent.INTRODUCE_ASSOCIATION_TYPE, assocType);
            }
        } catch (Exception e) {
            throw new RuntimeException("Introducing association types to " + this + " failed", e);
        }
    }



    // === Life Cycle ===

    private void invokePreInstallHook() {
        pluginContext.preInstall();
    }

    private void invokeInitHook() {
        pluginContext.init();
    }

    private void invokeShutdownHook() {
        try {
            pluginContext.shutdown();
        } catch (Throwable e) {
            // Note: we don't throw here. Stopping the plugin must proceed.
            logger.log(Level.SEVERE, "An error occurred in the shutdown() hook of " + this + ":", e);
        }
    }



    // === Events ===

    private void registerListeners() {
        try {
            List<DMXEvent> events = getEvents();
            //
            if (events.size() == 0) {
                logger.info("Registering event listeners of " + this + " SKIPPED -- no event listeners implemented");
                return;
            }
            //
            logger.info("Registering " + events.size() + " event listeners of " + this);
            for (DMXEvent event : events) {
                dm4.em.addListener(event, (EventListener) pluginContext);
            }
        } catch (Exception e) {
            throw new RuntimeException("Registering event listeners of " + this + " failed", e);
        }
    }

    private void unregisterListeners() {
        List<DMXEvent> events = getEvents();
        if (events.size() == 0) {
            return;
        }
        //
        logger.info("Unregistering event listeners of " + this);
        for (DMXEvent event : events) {
            dm4.em.removeListener(event, (EventListener) pluginContext);
        }
    }

    // ---

    /**
     * Returns the events this plugin is listening to.
     */
    private List<DMXEvent> getEvents() {
        List<DMXEvent> events = new ArrayList();
        for (Class interfaze : pluginContext.getClass().getInterfaces()) {
            if (isListenerInterface(interfaze)) {
                DMXEvent event = DMXEvent.getEvent(interfaze);
                logger.fine("### EventListener Interface: " + interfaze + ", event=" + event);
                events.add(event);
            }
        }
        return events;
    }

    /**
     * Checks weather this plugin is a listener for the given event, and if so, dispatches the event to this plugin.
     * Otherwise nothing is performed.
     * <p>
     * Called internally to dispatch the INTRODUCE_TOPIC_TYPE and INTRODUCE_ASSOCIATION_TYPE events.
     */
    private void dispatchEvent(DMXEvent event, Object... params) {
        dm4.em.dispatchEvent(this, event, params);
    }

    /**
     * Returns true if the specified interface is an event listener interface.
     * A event listener interface is a sub-interface of {@link EventListener}.
     */
    private boolean isListenerInterface(Class interfaze) {
        return EventListener.class.isAssignableFrom(interfaze);
    }



    // === Provided Service ===

    /**
     * Registers the provided service at the OSGi framework.
     * If this plugin doesn't provide a service nothing is performed.
     */
    private void registerProvidedService() {
        // Note: "providedServiceInterface" is initialized in constructor. Initializing it here would be
        // too late as the MigrationManager accesses it. In activate() the MigrationManager is called
        // *before* registerProvidedService().
        try {
            if (providedServiceInterface == null) {
                logger.info("Registering OSGi service of " + this + " SKIPPED -- no OSGi service provided");
                return;
            }
            //
            logger.info("Registering service \"" + providedServiceInterface + "\" at OSGi framework");
            bundleContext.registerService(providedServiceInterface, pluginContext, null);
        } catch (Exception e) {
            throw new RuntimeException("Registering service of " + this + " at OSGi framework failed", e);
        }
    }

    private String providedServiceInterface() {
        List<Class<?>> serviceInterfaces = getInterfaces("Service");
        switch (serviceInterfaces.size()) {
        case 0:
            return null;
        case 1:
            return serviceInterfaces.get(0).getName();
        default:
            throw new RuntimeException("Only one service interface per plugin is supported");
        }
    }



    // === Web Resources ===

    /**
     * Publishes this plugin's web resources (via WebPublishingService).
     * If the plugin doesn't provide web resources nothing is performed.
     */
    private void publishWebResources() {
        String uriNamespace = null;
        try {
            uriNamespace = getWebResourcesNamespace();
            if (uriNamespace == null) {
                logger.info("Publishing web resources of " + this + " SKIPPED -- no web resources provided");
                return;
            }
            //
            logger.info("Publishing web resources of " + this + " at URI namespace \"" + uriNamespace + "\"");
            webResources = dm4.wpService.publishWebResources(uriNamespace, pluginBundle);
        } catch (Exception e) {
            throw new RuntimeException("Publishing web resources of " + this + " failed " +
                "(URI namespace=\"" + uriNamespace + "\")", e);
        }
    }

    private void unpublishWebResources() {
        if (webResources != null) {
            logger.info("Unpublishing web resources of " + this);
            webResources.unpublish();
        }
    }

    // ---

    private String getWebResourcesNamespace() {
        return getBundleEntry("/web") != null ? "/" + pluginUri : null;
    }

    private URL getBundleEntry(String path) {
        return pluginBundle.getEntry(path);
    }



    // === File System Resources ===

    // Note: publishing is performed by public method publishFileSystem()

    private void unpublishFileSystem() {
        if (fileSystemResources != null) {
            logger.info("Unpublishing file system resources of " + this);
            fileSystemResources.unpublish();
        }
    }



    // === REST Resources ===

    /**
     * Publishes this plugin's REST resources (via WebPublishingService).
     * If the plugin doesn't provide REST resources nothing is performed.
     */
    private void publishRestResources() {
        try {
            // root resources
            List<Object> rootResources = getRootResources();
            if (rootResources.size() != 0) {
                String uriNamespace = dm4.wpService.getUriNamespace(pluginContext);
                logger.info("Publishing REST resources of " + this + " at URI namespace \"" + uriNamespace + "\"");
            } else {
                logger.info("Publishing REST resources of " + this + " SKIPPED -- no REST resources provided");
            }
            // provider classes
            List<Class<?>> providerClasses = getProviderClasses();
            if (providerClasses.size() != 0) {
                logger.info("Registering " + providerClasses.size() + " provider classes of " + this);
            } else {
                logger.info("Registering provider classes of " + this + " SKIPPED -- no provider classes found");
            }
            // register
            if (rootResources.size() != 0 || providerClasses.size() != 0) {
                restResources = dm4.wpService.publishRestResources(rootResources, providerClasses);
            }
        } catch (Exception e) {
            unpublishWebResources();
            throw new RuntimeException("Publishing REST resources (including provider classes) of " + this +
                " failed", e);
        }
    }

    private void unpublishRestResources() {
        if (restResources != null) {
            logger.info("Unpublishing REST resources (including provider classes) of " + this);
            restResources.unpublish();
        }
    }

    // ---

    private List<Object> getRootResources() {
        List<Object> rootResources = new ArrayList();
        if (dm4.wpService.isRootResource(pluginContext)) {
            rootResources.add(pluginContext);
        }
        return rootResources;
    }

    private List<Class<?>> getProviderClasses() throws IOException {
        List<Class<?>> providerClasses = new ArrayList();
        for (String className : scanPackage("/provider")) {
            Class clazz = loadClass(className);
            if (clazz == null) {
                throw new RuntimeException("Loading provider class \"" + className + "\" failed");
            } else if (!dm4.wpService.isProviderClass(clazz)) {
                // Note: scanPackage() also returns nested classes, so we check explicitly.
                continue;
            }
            //
            providerClasses.add(clazz);
        }
        return providerClasses;
    }



    // === Plugin Dependencies ===

    private List<String> pluginDependencies() {
        List<String> pluginDependencies = new ArrayList();
        String activateAfter = getConfigProperty("dm4.plugin.activate_after");
        if (activateAfter != null) {
            String[] pluginUris = activateAfter.split(", *");
            for (int i = 0; i < pluginUris.length; i++) {
                pluginDependencies.add(pluginUris[i]);
            }
        }
        //
        if (!pluginDependencies.isEmpty()) {
            logger.info("Tracking " + pluginDependencies.size() + " plugins for " + this + " " + pluginDependencies);
        } else {
            logger.info("Tracking plugins for " + this + " SKIPPED -- no plugin dependencies declared");
        }
        //
        return pluginDependencies;
    }

    private boolean hasDependency(String pluginUri) {
        return pluginDependencies.contains(pluginUri);
    }

    private boolean dependenciesAvailable() {
        for (String pluginUri : pluginDependencies) {
            if (!isPluginActivated(pluginUri)) {
                return false;
            }
        }
        return true;
    }

    private boolean isPluginActivated(String pluginUri) {
        return dm4.pluginManager.isPluginActivated(pluginUri);
    }

    // Note: PLUGIN_ACTIVATED is defined as an OSGi event and not as a DMXEvent.
    // PLUGIN_ACTIVATED is not supposed to be listened by plugins.
    // It is a solely used internally (to track plugin availability).

    private void registerPluginActivatedEventListener() {
        String[] topics = new String[] {PLUGIN_ACTIVATED};
        Hashtable properties = new Hashtable();
        properties.put(EventConstants.EVENT_TOPIC, topics);
        bundleContext.registerService(EventHandler.class.getName(), this, properties);
    }

    private void postPluginActivatedEvent() {
        Map<String, String> properties = new HashMap();
        properties.put(EventConstants.BUNDLE_SYMBOLICNAME, pluginUri);
        eventService.postEvent(new Event(PLUGIN_ACTIVATED, properties));
    }

    // --- EventHandler Implementation ---

    @Override
    public void handleEvent(Event event) {
        String pluginUri = null;
        try {
            if (!event.getTopic().equals(PLUGIN_ACTIVATED)) {
                throw new RuntimeException("Unexpected event: " + event);
            }
            //
            pluginUri = (String) event.getProperty(EventConstants.BUNDLE_SYMBOLICNAME);
            if (!hasDependency(pluginUri)) {
                return;
            }
            //
            logger.info("Handling PLUGIN_ACTIVATED event from \"" + pluginUri + "\" for " + this);
            checkRequirementsForActivation();
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "An error occurred while handling PLUGIN_ACTIVATED event from \"" + pluginUri +
                "\" for " + this + ":", e);
            // Note: here we catch anything, also errors (like NoClassDefFoundError).
            // If thrown through the OSGi container it would not print out the stacktrace.
        }
    }



    // === Helper ===

    private String pluginName() {
        return pluginContext.getPluginName();
    }

    private List<Class<?>> getInterfaces(String suffix) {
        List<Class<?>> interfaces = new ArrayList();
        for (Class<?> interfaze : pluginContext.getClass().getInterfaces()) {
            if (interfaze.getName().endsWith(suffix)) {
                interfaces.add(interfaze);
            }
        }
        return interfaces;
    }

    // ---

    private List<String> scanPackage(String relativePath) {
        List<String> classNames = new ArrayList();
        Enumeration<String> e = getPluginPaths(relativePath);
        if (e != null) {
            while (e.hasMoreElements()) {
                String entryPath = e.nextElement();
                String className = entryPathToClassName(entryPath);
                logger.fine("  # Found class: " + className);
                classNames.add(className);
            }
        }
        return classNames;
    }

    private Enumeration<String> getPluginPaths(String relativePath) {
        String path = "/" + pluginPackage.replace('.', '/') + relativePath;
        logger.fine("### Scanning path \"" + path + "\"");
        return pluginBundle.getEntryPaths(path);
    }

    private String entryPathToClassName(String entryPath) {
        entryPath = entryPath.substring(0, entryPath.length() - 6);     // strip ".class"
        return entryPath.replace('/', '.');
    }
}
