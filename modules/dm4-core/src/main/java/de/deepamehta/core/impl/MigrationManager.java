package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.service.Migration;
import de.deepamehta.core.service.Plugin;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;

import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.logging.Logger;



class MigrationManager {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String CORE_MIGRATIONS_PACKAGE = "de.deepamehta.core.migrations";
    private static final int REQUIRED_CORE_MIGRATION = 5;

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private EmbeddedService dms;

    private enum MigrationRunMode {
        CLEAN_INSTALL, UPDATE, ALWAYS
    }

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    MigrationManager(EmbeddedService dms) {
        this.dms = dms;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * Determines the migrations to be run for the specified plugin and run them.
     */
    void runPluginMigrations(PluginImpl plugin, boolean isCleanInstall) {
        int migrationNr = plugin.getPluginTopic().getChildTopics().getTopic("dm4.core.plugin_migration_nr")
            .getSimpleValue().intValue();
        int requiredMigrationNr = Integer.parseInt(plugin.getConfigProperty("requiredPluginMigrationNr", "0"));
        int migrationsToRun = requiredMigrationNr - migrationNr;
        //
        if (migrationsToRun == 0) {
            logger.info("Running migrations for " + plugin + " ABORTED -- everything up-to-date (migrationNr=" +
                migrationNr + ")");
            return;
        }
        //
        logger.info("Running " + migrationsToRun + " migrations for " + plugin + " (migrationNr=" + migrationNr +
            ", requiredMigrationNr=" + requiredMigrationNr + ")");
        for (int i = migrationNr + 1; i <= requiredMigrationNr; i++) {
            runPluginMigration(plugin, i, isCleanInstall);
        }
    }

    /**
     * Determines the core migrations to be run and run them.
     */
    void runCoreMigrations(boolean isCleanInstall) {
        int migrationNr = dms.storageDecorator.fetchMigrationNr();
        int requiredMigrationNr = REQUIRED_CORE_MIGRATION;
        int migrationsToRun = requiredMigrationNr - migrationNr;
        //
        if (migrationsToRun == 0) {
            logger.info("Running core migrations ABORTED -- everything up-to-date (migrationNr=" + migrationNr + ")");
            return;
        }
        //
        logger.info("Running " + migrationsToRun + " core migrations (migrationNr=" + migrationNr +
            ", requiredMigrationNr=" + requiredMigrationNr + ")");
        for (int i = migrationNr + 1; i <= requiredMigrationNr; i++) {
            runCoreMigration(i, isCleanInstall);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void runCoreMigration(int migrationNr, boolean isCleanInstall) {
        runMigration(migrationNr, null, isCleanInstall);
        dms.storageDecorator.storeMigrationNr(migrationNr);
    }

    private void runPluginMigration(PluginImpl plugin, int migrationNr, boolean isCleanInstall) {
        runMigration(migrationNr, plugin, isCleanInstall);
        plugin.setMigrationNr(migrationNr);
    }

    // ---

    /**
     * Runs a core migration or a plugin migration.
     *
     * @param   migrationNr     Number of the migration to run.
     * @param   plugin          The plugin that provides the migration to run.
     *                          <code>null</code> for a core migration.
     * @param   isCleanInstall  <code>true</code> if the migration is run as part of a clean install,
     *                          <code>false</code> if the migration is run as part of an update.
     */
    private void runMigration(int migrationNr, PluginImpl plugin, boolean isCleanInstall) {
        MigrationInfo mi = null;
        try {
            mi = new MigrationInfo(migrationNr, plugin);
            if (!mi.success) {
                throw mi.exception;
            }
            // error checks
            if (!mi.isDeclarative && !mi.isImperative) {
                String message = "Neither a migration file (" + mi.migrationFile + ") nor a migration class ";
                if (mi.migrationClassName != null) {
                    throw new RuntimeException(message + "(" + mi.migrationClassName + ") is found");
                } else {
                    throw new RuntimeException(message + "is found. Note: a possible migration class can't be located" +
                        " (plugin package is unknown). Consider setting \"pluginPackage\" in plugin.properties");
                }
            }
            if (mi.isDeclarative && mi.isImperative) {
                throw new RuntimeException("Ambiguity: a migration file (" + mi.migrationFile + ") AND a migration " +
                    "class (" + mi.migrationClassName + ") are found. Consider using two different migration numbers.");
            }
            // run migration
            String runInfo = " (runMode=" + mi.runMode + ", isCleanInstall=" + isCleanInstall + ")";
            if (mi.runMode.equals(MigrationRunMode.CLEAN_INSTALL.name()) == isCleanInstall ||
                mi.runMode.equals(MigrationRunMode.ALWAYS.name())) {
                logger.info("Running " + mi.migrationInfo + runInfo);
                if (mi.isDeclarative) {
                    readMigrationFile(mi.migrationIn, mi.migrationFile);
                } else {
                    Migration migration = (Migration) mi.migrationClass.newInstance();
                    logger.info("Running " + mi.migrationType + " migration class " + mi.migrationClassName);
                    injectServices(migration, mi.migrationInfo, plugin);
                    migration.setCoreService(dms);
                    migration.run();
                }
                logger.info("Completing " + mi.migrationInfo);
            } else {
                logger.info("Running " + mi.migrationInfo + " ABORTED" + runInfo);
            }
            logger.info("Updating migration number (" + migrationNr + ")");
        } catch (Exception e) {
            throw new RuntimeException("Running " + mi.migrationInfo + " failed", e);
        }
    }

    private void injectServices(Migration migration, String migrationInfo, PluginImpl plugin) {
        try {
            for (Field field : PluginImpl.getInjectableFields(migration.getClass())) {
                Class<? extends PluginService> serviceInterface = (Class<? extends PluginService>) field.getType();
                PluginService service;
                // 
                if (serviceInterface.getName().equals(plugin.getProvidedServiceInterface())) {
                    // the migration consumes the plugin's own service
                    service = (PluginService) plugin.getContext();
                } else {
                    service = plugin.getPluginService(serviceInterface);
                }
                //
                logger.info("Injecting service " + serviceInterface.getName() + " into " + migrationInfo);
                field.set(migration, service);  // throws IllegalAccessException
            }
        } catch (Exception e) {
            throw new RuntimeException("Injecting services into " + migrationInfo + " failed", e);
        }
    }

    // ---

    /**
     * Creates types and topics from a JSON formatted input stream.
     *
     * @param   migrationFileName   The origin migration file. Used for logging only.
     */
    private void readMigrationFile(InputStream in, String migrationFileName) {
        try {
            logger.info("Reading migration file \"" + migrationFileName + "\"");
            String fileContent = JavaUtils.readText(in);
            //
            Object value = new JSONTokener(fileContent).nextValue();
            if (value instanceof JSONObject) {
                readEntities((JSONObject) value);
            } else if (value instanceof JSONArray) {
                readEntities((JSONArray) value);
            } else {
                throw new RuntimeException("Invalid file content");
            }
        } catch (Exception e) {
            throw new RuntimeException("Reading migration file \"" + migrationFileName + "\" failed", e);
        }
    }

    private void readEntities(JSONArray entities) throws JSONException {
        for (int i = 0; i < entities.length(); i++) {
            readEntities(entities.getJSONObject(i));
        }
    }

    private void readEntities(JSONObject entities) throws JSONException {
        JSONArray topicTypes = entities.optJSONArray("topic_types");
        if (topicTypes != null) {
            createTopicTypes(topicTypes);
        }
        JSONArray assocTypes = entities.optJSONArray("assoc_types");
        if (assocTypes != null) {
            createAssociationTypes(assocTypes);
        }
        JSONArray topics = entities.optJSONArray("topics");
        if (topics != null) {
            createTopics(topics);
        }
        JSONArray assocs = entities.optJSONArray("associations");
        if (assocs != null) {
            createAssociations(assocs);
        }
    }

    private void createTopicTypes(JSONArray topicTypes) throws JSONException {
        for (int i = 0; i < topicTypes.length(); i++) {
            dms.createTopicType(new TopicTypeModel(topicTypes.getJSONObject(i)));
        }
    }

    private void createAssociationTypes(JSONArray assocTypes) throws JSONException {
        for (int i = 0; i < assocTypes.length(); i++) {
            dms.createAssociationType(new AssociationTypeModel(assocTypes.getJSONObject(i)));
        }
    }

    private void createTopics(JSONArray topics) throws JSONException {
        for (int i = 0; i < topics.length(); i++) {
            dms.createTopic(new TopicModel(topics.getJSONObject(i)));
        }
    }

    private void createAssociations(JSONArray assocs) throws JSONException {
        for (int i = 0; i < assocs.length(); i++) {
            dms.createAssociation(new AssociationModel(assocs.getJSONObject(i)));
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Classes

    /**
     * Collects the info required to run a migration.
     */
    private class MigrationInfo {

        String migrationType;       // "core", "plugin"
        String migrationInfo;       // for logging
        String runMode;             // "CLEAN_INSTALL", "UPDATE", "ALWAYS"
        //
        boolean isDeclarative;
        boolean isImperative;
        //
        String migrationFile;       // for declarative migration
        InputStream migrationIn;    // for declarative migration
        //
        String migrationClassName;  // for imperative migration
        Class migrationClass;       // for imperative migration
        //
        boolean success;            // error occurred?
        Exception exception;        // the error

        MigrationInfo(int migrationNr, PluginImpl plugin) {
            try {
                String configFile = migrationConfigFile(migrationNr);
                InputStream configIn;
                migrationFile = migrationFile(migrationNr);
                migrationType = plugin != null ? "plugin" : "core";
                //
                if (migrationType.equals("core")) {
                    migrationInfo = "core migration " + migrationNr;
                    configIn     = getClass().getResourceAsStream(configFile);
                    migrationIn  = getClass().getResourceAsStream(migrationFile);
                    migrationClassName = coreMigrationClassName(migrationNr);
                    migrationClass = loadClass(migrationClassName);
                } else {
                    migrationInfo = "migration " + migrationNr + " of " + plugin;
                    configIn     = getStaticResourceOrNull(plugin, configFile);
                    migrationIn  = getStaticResourceOrNull(plugin, migrationFile);
                    migrationClassName = plugin.getMigrationClassName(migrationNr);
                    if (migrationClassName != null) {
                        migrationClass = plugin.loadClass(migrationClassName);
                    }
                }
                //
                isDeclarative = migrationIn != null;
                isImperative = migrationClass != null;
                //
                readMigrationConfigFile(configIn, configFile);
                //
                success = true;
            } catch (Exception e) {
                exception = e;
            }
        }

        // ---

        private void readMigrationConfigFile(InputStream in, String configFile) {
            try {
                Properties migrationConfig = new Properties();
                if (in != null) {
                    logger.info("Reading migration config file \"" + configFile + "\"");
                    migrationConfig.load(in);
                } else {
                    logger.info("Reading migration config file \"" + configFile + "\" ABORTED -- file does not exist");
                }
                //
                runMode = migrationConfig.getProperty("migrationRunMode", MigrationRunMode.ALWAYS.name());
                MigrationRunMode.valueOf(runMode);  // check if value is valid
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Reading migration config file \"" + configFile + "\" failed: \"" + runMode +
                    "\" is an invalid value for \"migrationRunMode\"", e);
            } catch (IOException e) {
                throw new RuntimeException("Reading migration config file \"" + configFile + "\" failed", e);
            }
        }

        // ---

        private String migrationFile(int migrationNr) {
            return "/migrations/migration" + migrationNr + ".json";
        }

        private String migrationConfigFile(int migrationNr) {
            return "/migrations/migration" + migrationNr + ".properties";
        }

        private String coreMigrationClassName(int migrationNr) {
            return CORE_MIGRATIONS_PACKAGE + ".Migration" + migrationNr;
        }

        // --- Helper ---

        private InputStream getStaticResourceOrNull(Plugin plugin, String resourceName) {
            if (plugin.hasStaticResource(resourceName)) {
                return plugin.getStaticResource(resourceName);
            } else {
                return null;
            }
        }

        /**
         * Uses the core bundle's class loader to load a class by name.
         *
         * @return  the class, or <code>null</code> if the class is not found.
         */
        private Class loadClass(String className) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
    }
}
