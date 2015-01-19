package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.osgi.PluginContext;
import de.deepamehta.core.service.DeepaMehtaEvent;
import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.service.EventListener;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Plugin;
import de.deepamehta.core.service.PluginInfo;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.SecurityHandler;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
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



public class PluginImpl implements Plugin, EventHandler {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String PLUGIN_DEFAULT_PACKAGE = "de.deepamehta.core.osgi";
    private static final String PLUGIN_CONFIG_FILE = "/plugin.properties";
    private static final String PLUGIN_ACTIVATED = "de/deepamehta/core/plugin_activated";   // topic of the OSGi event

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private PluginContext pluginContext;
    private BundleContext bundleContext;

    private Bundle       pluginBundle;
    private String       pluginUri;             // This bundle's symbolic name, e.g. "de.deepamehta.webclient"

    private Properties   pluginProperties;      // Read from file "plugin.properties"
    private String       pluginPackage;
    private PluginInfo   pluginInfo;
    private List<String> pluginDependencies;    // plugin URIs as read from "importModels" property
    private Topic        pluginTopic;           // Represents this plugin in DB. Holds plugin migration number.

    // Consumed services (DeepaMehta Core and OSGi)
    private EmbeddedService dms;
    private WebPublishingService webPublishingService;
    private EventAdmin eventService;            // needed to post the PLUGIN_ACTIVATED OSGi event

    // Consumed plugin services
    //      key: service interface (a class object),
    //      value: service object. Is null if the service is not yet available. ### FIXDOC
    private Map<Class<? extends PluginService>, InjectableService> consumedPluginServices = new HashMap();

    // Trackers for the consumed services (DeepaMehta Core, OSGi, and plugin services)
    private List<ServiceTracker> serviceTrackers = new ArrayList();

    // Provided OSGi service
    private String providedServiceInterface;
    private ServiceRegistration registration;

    // Provided resources
    private StaticResources staticResources;
    private StaticResources directoryResource;
    private RestResources restResources;

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
        this.pluginPackage = getConfigProperty("pluginPackage", pluginContext.getClass().getPackage().getName());
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
        createPluginServiceTrackers();
        //
        openServiceTrackers();
    }

    public void stop() {
        pluginContext.shutdown();
        closeServiceTrackers();
    }

    // ---

    public void publishDirectory(String directoryPath, String uriNamespace, SecurityHandler securityHandler) {
        try {
            logger.info("### Publishing directory \"" + directoryPath + "\" at URI namespace \"" + uriNamespace + "\"");
            //
            if (directoryResource != null) {
                throw new RuntimeException(this + " has already published a directory; " +
                    "only one per plugin is supported");
            }
            //
            directoryResource = webPublishingService.publishStaticResources(directoryPath, uriNamespace,
                securityHandler);
        } catch (Exception e) {
            throw new RuntimeException("Publishing directory \"" + directoryPath + "\" at URI namespace \"" +
                uriNamespace + "\" failed", e);
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
     *          This is the case if the plugin bundle contains no Plugin subclass and the "pluginPackage" config
     *          property is not set.
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
                logger.info("Reading config file \"" + PLUGIN_CONFIG_FILE + "\" for " + this + " ABORTED " +
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
        serviceTrackers.add(createServiceTracker(DeepaMehtaService.class));
        serviceTrackers.add(createServiceTracker(WebPublishingService.class));
        serviceTrackers.add(createServiceTracker(EventAdmin.class));
    }

    private void createPluginServiceTrackers() {
        List<InjectableService> injectableServices = createInjectableServices();
        //
        if (injectableServices.isEmpty()) {
            logger.info("Tracking plugin services for " + this + " ABORTED -- no services consumed");
            return;
        }
        //
        logger.info("Tracking " + injectableServices.size() + " plugin services for " + this + " " +
            injectableServices);
        for (InjectableService injectableService : injectableServices) {
            Class<? extends PluginService> serviceInterface = injectableService.getServiceInterface();
            consumedPluginServices.put(serviceInterface, injectableService);
            serviceTrackers.add(createServiceTracker(serviceInterface));
        }
    }

    // ---

    private List<InjectableService> createInjectableServices() {
        List<InjectableService> injectableServices = new ArrayList();
        //
        for (Field field : getInjectableFields(pluginContext.getClass())) {
            Class<? extends PluginService> serviceInterface = (Class<? extends PluginService>) field.getType();
            injectableServices.add(new InjectableService(pluginContext, serviceInterface, field));
        }
        return injectableServices;
    }

    private boolean pluginServicesAvailable() {
        for (InjectableService injectableService : consumedPluginServices.values()) {
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
        //
        // Note: we use getDeclaredFields() (instead of getFields()) to *not* search the super classes
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                Class<?> fieldType = field.getType();
                //
                if (!PluginService.class.isAssignableFrom(fieldType)) {
                    throw new RuntimeException("@Inject annotated field \"" + field.getName() +
                        "\" has unsupported type (" + fieldType.getName() + "). Use @Inject " +
                        "only for injecting plugin services.");
                }
                //
                field.setAccessible(true);  // allow injection into private fields
                injectableFields.add(field);
            }
        }
        return injectableFields;
    }

    // called from MigrationManager
    PluginService getPluginService(Class<? extends PluginService> serviceInterface) {
        InjectableService injectableService = consumedPluginServices.get(serviceInterface);
        if (injectableService == null) {
            throw new RuntimeException("Service " + serviceInterface.getName() + " is not consumed by " + this);
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
                    logger.log(Level.SEVERE, "Adding service " + serviceInterface.getName() + " to " +
                        pluginContext + " failed", e);
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
                    logger.log(Level.SEVERE, "Removing service " + serviceInterface.getName() + " from " +
                        pluginContext + " failed", e);
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
        if (service instanceof DeepaMehtaService) {
            logger.info("Adding DeepaMehta 4 core service to " + this);
            setCoreService((EmbeddedService) service);
            checkRequirementsForActivation();
        } else if (service instanceof WebPublishingService) {
            logger.info("Adding Web Publishing service to " + this);
            webPublishingService = (WebPublishingService) service;
            publishStaticResources();
            publishRestResources();
            checkRequirementsForActivation();
        } else if (service instanceof EventAdmin) {
            logger.info("Adding Event Admin service to " + this);
            eventService = (EventAdmin) service;
            checkRequirementsForActivation();
        } else if (service instanceof PluginService) {
            logger.info("Adding service " + serviceInterface.getName() + " to " + this);
            consumedPluginServices.get(serviceInterface).injectService((PluginService) service);
            pluginContext.serviceArrived((PluginService) service);
            checkRequirementsForActivation();
        }
    }

    private void removeService(Object service, Class serviceInterface) {
        if (service == dms) {
            logger.info("Removing DeepaMehta 4 core service from " + this);
            dms.pluginManager.deactivatePlugin(this);   // use plugin manager before core service is removed
            setCoreService(null);
        } else if (service == webPublishingService) {
            logger.info("Removing Web Publishing service from " + this);
            unpublishRestResources();
            unpublishStaticResources();
            unpublishDirectoryResource();
            webPublishingService = null;
        } else if (service == eventService) {
            logger.info("Removing Event Admin service from " + this);
            eventService = null;
        } else if (service instanceof PluginService) {
            logger.info("Removing service " + serviceInterface.getName() + " from " + this);
            pluginContext.serviceGone((PluginService) service);
            consumedPluginServices.get(serviceInterface).injectService(null);
        }
    }

    // ---

    private void setCoreService(EmbeddedService dms) {
        this.dms = dms;
        pluginContext.setCoreService(dms);
    }

    // ---

    /**
     * Checks if this plugin's requirements are met, and if so, activates this plugin.
     *
     * The requirements:
     *   - the 3 core services are available (DeepaMehtaService, WebPublishingService, EventAdmin).
     *   - the plugin services (according to the "ConsumesService" annotation ### FIXDOC) are available.
     *   - the plugin dependencies (according to the "importModels" config property) are active.
     *
     * Note: The Web Publishing service is not strictly required for activation, but we must ensure
     * ALL_PLUGINS_ACTIVE is not fired before the Web Publishing service becomes available.
     */
    private void checkRequirementsForActivation() {
        if (dms != null && webPublishingService != null && eventService != null && pluginServicesAvailable()
                                                                                && dependenciesAvailable()) {
            dms.pluginManager.activatePlugin(this);
        }
    }



    // === Activation ===

    /**
     * Activates this plugin and then posts the PLUGIN_ACTIVATED OSGi event.
     *
     * Activation comprises:
     *   - install the plugin in the database (includes migrations, post-install hook, type introduction)
     *   - initialize the plugin
     *   - register the plugin's event listeners
     *   - register the plugin's OSGi service
     */
    void activate() {
        try {
            logger.info("----- Activating " + this + " -----");
            //
            installPluginInDB();
            initializePlugin();
            registerListeners();
            registerPluginService();
            // Note: the event listeners must be registered *after* the plugin is installed in the database (see
            // installPluginInDB() below).
            // Consider the Access Control plugin: it can't set a topic's creator before the "admin" user is created.
            //
            logger.info("----- Activation of " + this + " complete -----");
            //
            postPluginActivatedEvent();
            //
        } catch (Exception e) {
            throw new RuntimeException("Activation of " + this + " failed", e);
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
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            // 1) create "Plugin" topic
            boolean isCleanInstall = createPluginTopicIfNotExists();
            // 2) run migrations
            dms.migrationManager.runPluginMigrations(this, isCleanInstall);
            //
            if (isCleanInstall) {
                // 3) type introduction
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
            logger.info("Installing " + this + " in the database ABORTED -- already installed");
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
        return dms.createTopic(new TopicModel(pluginUri, "dm4.core.plugin", new ChildTopicsModel()
            .put("dm4.core.plugin_name", pluginName())
            .put("dm4.core.plugin_symbolic_name", pluginUri)
            .put("dm4.core.plugin_migration_nr", 0)
        ));
    }

    private Topic fetchPluginTopic() {
        return dms.getTopic("uri", new SimpleValue(pluginUri));
    }

    // ---

    private void introduceTopicTypesToPlugin() {
        try {
            for (String topicTypeUri : dms.getTopicTypeUris()) {
                // ### TODO: explain
                if (topicTypeUri.equals("dm4.core.meta_meta_type")) {
                    continue;
                }
                //
                TopicType topicType = dms.getTopicType(topicTypeUri);
                deliverEvent(CoreEvent.INTRODUCE_TOPIC_TYPE, topicType);
            }
        } catch (Exception e) {
            throw new RuntimeException("Introducing topic types to " + this + " failed", e);
        }
    }

    private void introduceAssociationTypesToPlugin() {
        try {
            for (String assocTypeUri : dms.getAssociationTypeUris()) {
                AssociationType assocType = dms.getAssociationType(assocTypeUri);
                deliverEvent(CoreEvent.INTRODUCE_ASSOCIATION_TYPE, assocType);
            }
        } catch (Exception e) {
            throw new RuntimeException("Introducing association types to " + this + " failed", e);
        }
    }



    // === Initialization ===

    private void initializePlugin() {
        pluginContext.init();
    }



    // === Events ===

    private void registerListeners() {
        List<DeepaMehtaEvent> events = getEvents();
        //
        if (events.size() == 0) {
            logger.info("Registering event listeners of " + this + " ABORTED -- no event listeners implemented");
            return;
        }
        //
        logger.info("Registering " + events.size() + " event listeners of " + this);
        for (DeepaMehtaEvent event : events) {
            dms.eventManager.addListener(event, (EventListener) pluginContext);
        }
    }

    private void unregisterListeners() {
        List<DeepaMehtaEvent> events = getEvents();
        if (events.size() == 0) {
            return;
        }
        //
        logger.info("Unregistering event listeners of " + this);
        for (DeepaMehtaEvent event : events) {
            dms.eventManager.removeListener(event, (EventListener) pluginContext);
        }
    }

    // ---

    /**
     * Returns the events this plugin is listening to.
     */
    private List<DeepaMehtaEvent> getEvents() {
        List<DeepaMehtaEvent> events = new ArrayList();
        for (Class interfaze : pluginContext.getClass().getInterfaces()) {
            if (isListenerInterface(interfaze)) {
                DeepaMehtaEvent event = DeepaMehtaEvent.getEvent(interfaze);
                logger.fine("### EventListener Interface: " + interfaze + ", event=" + event);
                events.add(event);
            }
        }
        return events;
    }

    /**
     * Checks weather this plugin is a listener for the given event, and if so, delivers the event to this plugin.
     * Otherwise nothing is performed.
     * <p>
     * Called internally to deliver the INTRODUCE_TOPIC_TYPE and INTRODUCE_ASSOCIATION_TYPE events.
     */
    private void deliverEvent(DeepaMehtaEvent event, Object... params) {
        dms.eventManager.deliverEvent(this, event, params);
    }

    /**
     * Returns true if the specified interface is an event listener interface.
     * A event listener interface is a sub-interface of {@link EventListener}.
     */
    private boolean isListenerInterface(Class interfaze) {
        return EventListener.class.isAssignableFrom(interfaze);
    }



    // === Plugin Service ===

    /**
     * Registers this plugin's OSGi service at the OSGi framework.
     * If the plugin doesn't provide an OSGi service nothing is performed.
     */
    private void registerPluginService() {
        try {
            if (providedServiceInterface == null) {
                logger.info("Registering OSGi service of " + this + " ABORTED -- no OSGi service provided");
                return;
            }
            //
            logger.info("Registering service \"" + providedServiceInterface + "\" at OSGi framework");
            registration = bundleContext.registerService(providedServiceInterface, pluginContext, null);
        } catch (Exception e) {
            throw new RuntimeException("Registering service of " + this + " at OSGi framework failed", e);
        }
    }

    private String providedServiceInterface() {
        List<String> serviceInterfaces = scanPackage("/service");
        switch (serviceInterfaces.size()) {
        case 0:
            return null;
        case 1:
            return serviceInterfaces.get(0);
        default:
            throw new RuntimeException("Only one service interface per plugin is supported");
        }
    }



    // === Static Resources ===

    /**
     * Publishes this plugin's static resources (via Web Publishing service).
     * If the plugin doesn't provide static resources nothing is performed.
     */
    private void publishStaticResources() {
        String uriNamespace = null;
        try {
            uriNamespace = getStaticResourcesNamespace();
            if (uriNamespace == null) {
                logger.info("Publishing static resources of " + this + " ABORTED -- no static resources provided");
                return;
            }
            //
            logger.info("Publishing static resources of " + this + " at URI namespace \"" + uriNamespace + "\"");
            staticResources = webPublishingService.publishStaticResources(pluginBundle, uriNamespace);
        } catch (Exception e) {
            throw new RuntimeException("Publishing static resources of " + this + " failed " +
                "(uriNamespace=\"" + uriNamespace + "\")", e);
        }
    }

    private void unpublishStaticResources() {
        if (staticResources != null) {
            logger.info("Unpublishing static resources of " + this);
            webPublishingService.unpublishStaticResources(staticResources);
        }
    }

    // ---

    private String getStaticResourcesNamespace() {
        return getBundleEntry("/web") != null ? "/" + pluginUri : null;
    }

    private URL getBundleEntry(String path) {
        return pluginBundle.getEntry(path);
    }



    // === Directory Resources ===

    // Note: registration is performed by public method publishDirectory()

    private void unpublishDirectoryResource() {
        if (directoryResource != null) {
            logger.info("Unpublishing directory resource of " + this);
            webPublishingService.unpublishStaticResources(directoryResource);
        }
    }



    // === REST Resources ===

    /**
     * Publishes this plugin's REST resources (via Web Publishing service).
     * If the plugin doesn't provide REST resources nothing is performed.
     */
    private void publishRestResources() {
        try {
            // root resources
            List<Object> rootResources = getRootResources();
            if (rootResources.size() != 0) {
                String uriNamespace = webPublishingService.getUriNamespace(pluginContext);
                logger.info("Publishing REST resources of " + this + " at URI namespace \"" + uriNamespace + "\"");
            } else {
                logger.info("Publishing REST resources of " + this + " ABORTED -- no REST resources provided");
            }
            // provider classes
            List<Class<?>> providerClasses = getProviderClasses();
            if (providerClasses.size() != 0) {
                logger.info("Registering " + providerClasses.size() + " provider classes of " + this);
            } else {
                logger.info("Registering provider classes of " + this + " ABORTED -- no provider classes provided");
            }
            // register
            if (rootResources.size() != 0 || providerClasses.size() != 0) {
                restResources = webPublishingService.publishRestResources(rootResources, providerClasses);
            }
        } catch (Exception e) {
            unpublishStaticResources();
            throw new RuntimeException("Publishing REST resources (including provider classes) of " + this +
                " failed", e);
        }
    }

    private void unpublishRestResources() {
        if (restResources != null) {
            logger.info("Unpublishing REST resources (including provider classes) of " + this);
            webPublishingService.unpublishRestResources(restResources);
        }
    }

    // ---

    private List<Object> getRootResources() {
        List<Object> rootResources = new ArrayList();
        if (webPublishingService.isRootResource(pluginContext)) {
            rootResources.add(pluginContext);
        }
        return rootResources;
    }

    private List<Class<?>> getProviderClasses() throws IOException {
        List<Class<?>> providerClasses = new ArrayList();
        for (String className : scanPackage("/provider")) {
            Class providerClass = loadClass(className);
            if (providerClass == null) {
                throw new RuntimeException("Loading provider class \"" + className + "\" failed");
            }
            providerClasses.add(providerClass);
        }
        return providerClasses;
    }



    // === Plugin Dependencies ===

    private List<String> pluginDependencies() {
        List<String> pluginDependencies = new ArrayList();
        String importModels = getConfigProperty("importModels");
        if (importModels != null) {
            String[] pluginUris = importModels.split(", *");
            for (int i = 0; i < pluginUris.length; i++) {
                pluginDependencies.add(pluginUris[i]);
            }
        }
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
        return dms.pluginManager.isPluginActivated(pluginUri);
    }

    // Note: PLUGIN_ACTIVATED is defined as an OSGi event and not as a DeepaMehtaEvent.
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
            logger.log(Level.SEVERE, "Handling PLUGIN_ACTIVATED event from \"" + pluginUri + "\" for " + this +
                " failed", e);
            // Note: here we catch anything, also errors (like NoClassDefFoundError).
            // If thrown through the OSGi container it would not print out the stacktrace.
        }
    }



    // === Helper ===

    private String pluginName() {
        return pluginContext.getPluginName();
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
