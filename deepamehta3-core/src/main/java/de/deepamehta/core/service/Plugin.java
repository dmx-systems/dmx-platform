package de.deepamehta.core.service;

import de.deepamehta.core.model.ClientContext;
import de.deepamehta.core.model.Relation;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.storage.Transaction;
import de.deepamehta.core.util.JavaUtils;

import com.sun.jersey.spi.container.servlet.ServletContainer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;

import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import java.io.InputStream;
import java.io.IOException;

import java.net.URL;



/**
 * Base class for plugin developers to derive their plugins from.
 */
public class Plugin implements BundleActivator {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String PLUGIN_CONFIG_FILE = "/plugin.properties";
    private static final String STANDARD_PROVIDER_PACKAGE = "de.deepamehta.plugins.server.provider";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String pluginId;                    // This bundle's symbolic name.
    private String pluginName;                  // This bundle's name = POM project name.
    private String pluginClass;
    private String pluginPackage;
    private Bundle pluginBundle;
    private Topic  pluginTopic;                 // Represents this plugin in DB. Holds plugin migration number.

    private boolean isActivated;

    private Properties configProperties;        // Read from file "plugin.properties"

    protected CoreService dms;
    private HttpService httpService;

    private List<ServiceTracker> serviceTrackers = new ArrayList();

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    public String getId() {
        return pluginId;
    }

    public String getName() {
        return pluginName;
    }

    public Topic getPluginTopic() {
        return pluginTopic;
    }

    public String getConfigProperty(String key) {
        return getConfigProperty(key, null);
    }

    /**
     * Uses the plugin bundle's class loader to load a class by name.
     *
     * @return  the class, or <code>null</code> if the class is not found.
     */
    public Class loadClass(String className) {
        try {
            return pluginBundle.loadClass(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Returns the migration class name for the given migration number.
     *
     * @return  the fully qualified migration class name, or <code>null</code> if the migration package is unknown.
     *          This is the case if the plugin bundle contains no Plugin subclass and the "pluginPackage" config
     *          property is not set.
     */
    public String getMigrationClassName(int migrationNr) {
        if (pluginPackage.equals("de.deepamehta.core.service")) {
            return null;    // migration package is unknown
        }
        //
        return pluginPackage + ".migrations.Migration" + migrationNr;
    }

    /**
     * Uses the plugin bundle's class loader to find a resource.
     *
     * @return  A InputStream object or null if no resource with this name is found.
     */
    public InputStream getResourceAsStream(String name) throws IOException {
        // We always use the plugin bundle's class loader to access the resource.
        // getClass().getResource() would fail for generic plugins (plugin bundles not containing a plugin
        // subclass) because the core bundle's class loader would be used and it has no access.
        URL url = pluginBundle.getResource(name);
        if (url != null) {
            return url.openStream();
        } else {
            return null;
        }
    }

    // FIXME: drop method and make dms protected instead?
    public CoreService getService() {
        // CoreService dms = (CoreService) deepamehtaServiceTracker.getService();
        if (dms == null) {
            throw new RuntimeException("DeepaMehta core service is currently not available " +
                "for plugin \"" + pluginName + "\"");
        }
        return dms;
    }



    // **************************************
    // *** BundleActivator Implementation ***
    // **************************************



    @Override
    public void start(BundleContext context) {
        try {
            pluginBundle = context.getBundle();
            pluginId = pluginBundle.getSymbolicName();
            pluginName = (String) pluginBundle.getHeaders().get("Bundle-Name");
            pluginClass = (String) pluginBundle.getHeaders().get("Bundle-Activator");
            //
            logger.info("========== Starting DeepaMehta plugin \"" + pluginName + "\" ==========");
            //
            configProperties = readConfigFile();
            pluginPackage = getConfigProperty("pluginPackage", getClass().getPackage().getName());
            //
            createServiceTracker(CoreService.class.getName(), context);
            createServiceTracker(HttpService.class.getName(), context);
            createServiceTrackers(context);
        } catch (RuntimeException e) {
            logger.severe("Plugin \"" + pluginName + "\" can't be activated. Reason:");
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void stop(BundleContext context) {
        logger.info("========== Stopping DeepaMehta plugin \"" + pluginName + "\" ==========");
        //
        for (ServiceTracker serviceTracker : serviceTrackers) {
            serviceTracker.close();
        }
    }



    // *************
    // *** Hooks ***
    // *************



    public void postInstallPluginHook() {
    }

    public void allPluginsReadyHook() {
    }

    // ---

    public void serviceArrived(PluginService service) {
    }

    public void serviceGone(PluginService service) {
    }

    // ---

    public void preCreateHook(Topic topic, ClientContext clientContext) {
    }

    public void postCreateHook(Topic topic, ClientContext clientContext) {
    }

    public void preUpdateHook(Topic topic, Map<String, Object> newProperties) {
    }

    public void postUpdateHook(Topic topic, Map<String, Object> oldProperties) {
    }

    // ---

    public void preDeleteRelationHook(long relationId) {
    }

    public void postDeleteRelationHook(long relationId) {
    }

    // ---

    public void providePropertiesHook(Topic topic) {
    }

    public void providePropertiesHook(Relation relation) {
    }

    // ---

    public void enrichTopicHook(Topic topic, ClientContext clientContext) {
    }

    public void enrichTopicTypeHook(TopicType topicType, ClientContext clientContext) {
    }

    // ---

    /**
     * Allows a plugin to modify type definitions -- exisisting ones <i>and</i> future ones.
     * Plugins get a opportunity to visit (and modify) each type definition extacly once.
     * <p>
     * This hook is triggered in 2 situations:
     * <ul>
     *  <li>for each type that <i>exists</i> already while a plugin clean install.
     *  <li>for types created (interactively by the user, or programmatically by a migration) <i>after</i>
     *      the plugin has been installed.
     * </ul>
     * This hook is typically used by plugins which provide cross-cutting concerns by affecting <i>all</i>
     * type definitions of a DeepaMehta installation. Typically such a plugin adds new data fields to types
     * or relates types with specific topics.
     * <p>
     * Examples of plugins which use this hook:
     * <ul>
     *  <li>The "DeepaMehta 3 Workspaces" plugin adds a "Workspaces" field to all types.
     *  <li>The "DeepaMehta 3 Time" plugin adds timestamp fields to all types.
     *  <li>The "DeepaMehta 3 Access Control" plugin adds a "Creator" field to all types and relates them to a user.
     * </ul>
     *
     * @param   topicType   the type to be modified. The passed object is actually an instance of a {@link TopicType}
     *                      subclass that is backed by the database. That is, modifications by e.g.
     *                      {@link TopicType#addDataField} are persistent.
     *                      <p>
     *                      Note: at the time the hook is triggered the type exists already in the database, in
     *                      particular the underlying type topic has an ID already. That is, the type is ready for
     *                      e.g. being related to other topics.
     */
    public void modifyTopicTypeHook(TopicType topicType, ClientContext clientContext) {
    }

    // ---

    public JSONObject executeCommandHook(String command, Map params, ClientContext clientContext) {
        return null;
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private void createServiceTrackers(BundleContext context) {
        String consumedServiceInterfaces = getConfigProperty("consumedServiceInterfaces");
        if (consumedServiceInterfaces != null) {
            String[] serviceInterfaces = consumedServiceInterfaces.split(", *");
        	for (int i = 0; i < serviceInterfaces.length; i++) {
        	    createServiceTracker(serviceInterfaces[i], context);
        	}
        }
    }

    private void createServiceTracker(final String serviceInterface, BundleContext context) {
        ServiceTracker serviceTracker = new ServiceTracker(context, serviceInterface, null) {

            @Override
            public Object addingService(ServiceReference serviceRef) {
                Object service = super.addingService(serviceRef);
                if (service instanceof CoreService) {
                    logger.info("Adding DeepaMehta core service to plugin \"" + pluginName + "\"");
                    dms = (CoreService) service;
                    initPlugin(context);
                } else if (service instanceof HttpService) {
                    logger.info("Adding HTTP service to plugin \"" + pluginName + "\"");
                    httpService = (HttpService) service;
                    registerWebResources();
                    registerRestResources();
                } else {
                    logger.info("### Adding plugin service \"" + serviceInterface + "\" to plugin \"" +
                        pluginName + "\"");
                    // trigger hook
                    serviceArrived((PluginService) service);
                }
                //
                return service;
            }

            @Override
            public void removedService(ServiceReference ref, Object service) {
                if (service == dms) {
                    logger.info("Removing DeepaMehta core service from plugin \"" + pluginName + "\"");
                    unregisterPlugin();
                    dms = null;
                } else if (service == httpService) {
                    logger.info("Removing HTTP service from plugin \"" + pluginName + "\"");
                    unregisterWebResources();
                    unregisterRestResources();
                    httpService = null;
                } else if (service instanceof PluginService) {
                    logger.info("### Removing plugin service \"" + serviceInterface + "\" from plugin \"" +
                        pluginName + "\"");
                    // trigger hook
                    serviceGone((PluginService) service);
                }
                super.removedService(ref, service);
            }
        };
        serviceTrackers.add(serviceTracker);
        serviceTracker.open();
    }

    // ---

    private void registerPluginService(BundleContext context) {
        String serviceInterface = getConfigProperty("providedServiceInterface");
        if (serviceInterface != null) {
            logger.info("### Registering service \"" + serviceInterface +
                "\" of plugin \"" + pluginName + "\" at OSGi framework");
            context.registerService(serviceInterface, this, null);
        }
    }

    // ---

    private void registerPlugin() {
        logger.info("Registering plugin \"" + pluginName + "\" at DeepaMehta core service");
        dms.registerPlugin(this);
        isActivated = true;
    }

    private void unregisterPlugin() {
        if (isActivated) {
            logger.info("Unregistering plugin \"" + pluginName + "\" at DeepaMehta core service");
            dms.unregisterPlugin(pluginId);
        }
    }

    // ---

    private void registerWebResources() {
        String namespace = "/" + pluginId;
        try {
            logger.info("Registering web resources of plugin \"" + pluginName + "\" at namespace " + namespace);
            httpService.registerResources(namespace, "/web", null);
        } catch (NamespaceException e) {
            throw new RuntimeException("Web resources of plugin \"" + pluginName + "\" can't be registered " +
                "at namespace " + namespace, e);
        }
    }

    private void unregisterWebResources() {
        String namespace = "/" + pluginId;
        logger.info("Unregistering web resources of plugin \"" + pluginName + "\"");
        httpService.unregister(namespace);
    }

    // ---

    private void registerRestResources() {
        String namespace = getConfigProperty("restResourcesNamespace");
        try {
            if (namespace != null) {
                logger.info("Registering REST resources of plugin \"" + pluginName + "\" at namespace " + namespace);
                // Generic plugins (plugin bundles not containing a Plugin subclass) which provide resource classes
                // must set the "pluginPackage" config property. Otherwise the resource classes can't be located.
                if (pluginPackage.equals("de.deepamehta.core.service")) {
                    throw new RuntimeException("Resource classes can't be located (plugin package is unknown). " +
                        "You must implement a Plugin subclass OR configure \"pluginPackage\" in plugin.properties");
                }
                //
                Dictionary initParams = new Hashtable();
                if (loadClass(pluginPackage + ".Application") != null) {
                    initParams.put("javax.ws.rs.Application", pluginPackage + ".Application");
                } else {
                    initParams.put("com.sun.jersey.config.property.packages", packagesToScan());
                }
                //
                httpService.registerServlet(namespace, new ServletContainer(), initParams, null);
            }
        } catch (Exception e) {
            throw new RuntimeException("REST resources of plugin \"" + pluginName + "\" can't be registered " +
                "at namespace " + namespace, e);
        }
    }

    private void unregisterRestResources() {
        String namespace = getConfigProperty("restResourcesNamespace");
        if (namespace != null) {
            logger.info("Unregistering REST resources of plugin \"" + pluginName + "\"");
            httpService.unregister(namespace);
        }
    }

    /**
     * Returns the packages Jersey have to scan (for root resource and provider classes) for this plugin.
     * These comprise:
     * 1) The plugin's "resource" package.
     * 2) The plugin's "provider" package.
     * 3) The deepamehta3-server's "provider" package.
     *    This contains providers for DeepaMehta's core model classes, e.g. "Topic".
     */
    private String packagesToScan() {
        StringBuilder packages = new StringBuilder(pluginPackage + ".resources;");
        //
        String pluginProviderPackage = pluginPackage + ".provider";
        if (!pluginProviderPackage.equals(STANDARD_PROVIDER_PACKAGE)) {
            packages.append(pluginProviderPackage + ";");
        }
        // The standard provider classes of the deepamehta3-server module are available to every plugin
        packages.append(STANDARD_PROVIDER_PACKAGE);
        //
        return packages.toString();
    }

    // --- Config Properties ---

    private Properties readConfigFile() {
        try {
            Properties properties = new Properties();
            InputStream in = getResourceAsStream(PLUGIN_CONFIG_FILE);
            if (in != null) {
                logger.info("Reading plugin config file \"" + PLUGIN_CONFIG_FILE + "\"");
                properties.load(in);
            } else {
                logger.info("No plugin config file found (tried \"" + PLUGIN_CONFIG_FILE + "\")" +
                    " -- using default configuration");
            }
            return properties;
        } catch (IOException e) {
            throw new RuntimeException("Plugin config file can't be read", e);
        }
    }

    private String getConfigProperty(String key, String defaultValue) {
        return configProperties.getProperty(key, defaultValue);
    }

    // ---

    private void initPlugin(BundleContext context) {
        Transaction tx = dms.beginTx();
        try {
            logger.info("----- Initializing plugin \"" + pluginName + "\" -----");
            boolean isCleanInstall = initPluginTopic();
            runPluginMigrations(isCleanInstall);
            if (isCleanInstall) {
                postInstallPluginHook();  // trigger hook
                introduceTypesToPlugin();
            }
            registerPluginService(context);
            registerPlugin();
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Plugin \"" + pluginName + "\" can't be initialzed", e);
        } finally {
            tx.finish();
        }
    }

    /**
     * Creates a Plugin topic in the DB, if not already exists.
     * <p>
     * A Plugin topic represents an installed plugin and is used to track its version.
     *
     * @return  <code>true</code> if a Plugin topic has been created (means: this is a plugin clean install),
     *          <code>false</code> otherwise.
     */
    private boolean initPluginTopic() {
        pluginTopic = findPluginTopic();
        if (pluginTopic != null) {
            logger.info("Do NOT create topic for plugin \"" + pluginName + "\" -- already exists");
            return false;
        } else {
            logger.info("Creating topic for plugin \"" + pluginName + "\" -- this is a plugin clean install");
            Map properties = new HashMap();
            properties.put("de/deepamehta/core/property/PluginID", pluginId);
            properties.put("de/deepamehta/core/property/PluginMigrationNr", 0);
            // FIXME: clientContext=null
            pluginTopic = dms.createTopic("de/deepamehta/core/topictype/Plugin", properties, null);
            return true;
        }
    }

    private Topic findPluginTopic() {
        return dms.getTopic("de/deepamehta/core/property/PluginID", pluginId);
    }

    /**
     * Determines the migrations to be run for this plugin and run them.
     */
    private void runPluginMigrations(boolean isCleanInstall) {
        int migrationNr = (Integer) pluginTopic.getProperty("de/deepamehta/core/property/PluginMigrationNr");
        int requiredMigrationNr = Integer.parseInt(getConfigProperty("requiredPluginMigrationNr", "0"));
        int migrationsToRun = requiredMigrationNr - migrationNr;
        logger.info("migrationNr=" + migrationNr + ", requiredMigrationNr=" + requiredMigrationNr +
            " -- running " + migrationsToRun + " plugin migrations");
        for (int i = migrationNr + 1; i <= requiredMigrationNr; i++) {
            dms.runPluginMigration(this, i, isCleanInstall);
        }
    }

    private void introduceTypesToPlugin() {
        for (String typeUri : dms.getTopicTypeUris()) {
            // trigger hook
            modifyTopicTypeHook(dms.getTopicType(typeUri, null), null);   // clientContext=null
        }
    }
}
