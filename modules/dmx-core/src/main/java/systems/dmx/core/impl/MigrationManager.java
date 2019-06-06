package systems.dmx.core.impl;

import systems.dmx.core.service.Migration;
import systems.dmx.core.service.ModelFactory;
import systems.dmx.core.service.Plugin;
import systems.dmx.core.util.JavaUtils;

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

    private static final String CORE_MIGRATIONS_PACKAGE = "systems.dmx.core.migrations";
    private static final int CORE_MODEL_VERSION = 3;

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private CoreServiceImpl dmx;
    private ModelFactory mf;

    private enum MigrationRunMode {
        CLEAN_INSTALL, UPDATE, ALWAYS
    }

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    MigrationManager(CoreServiceImpl dmx) {
        this.dmx = dmx;
        this.mf = dmx.mf;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * Determines the migrations to be run for the specified plugin, and runs them.
     */
    void runPluginMigrations(PluginImpl plugin, boolean isCleanInstall) {
        int installedModelVersion = plugin.getPluginTopic().getChildTopics().getTopic("dmx.core.plugin_migration_nr")
            .getSimpleValue().intValue();
        int requiredModelVersion = Integer.parseInt(plugin.getConfigProperty("dmx.plugin.model_version", "0"));
        int migrationsToRun = requiredModelVersion - installedModelVersion;
        //
        if (migrationsToRun == 0) {
            logger.info("Running migrations for " + plugin + " SKIPPED -- installed model is up-to-date (version " +
                installedModelVersion + ")");
            return;
        }
        //
        logger.info("Running " + migrationsToRun + " migrations for " + plugin + " (installed model: version " +
            installedModelVersion + ", required model: version " + requiredModelVersion + ")");
        for (int i = installedModelVersion + 1; i <= requiredModelVersion; i++) {
            runMigration(i, plugin, isCleanInstall);
        }
    }

    /**
     * Determines the core migrations to be run, and runs them.
     */
    void runCoreMigrations(boolean isCleanInstall) {
        int installedModelVersion = dmx.pl.fetchMigrationNr();
        int requiredModelVersion = CORE_MODEL_VERSION;
        int migrationsToRun = requiredModelVersion - installedModelVersion;
        //
        if (migrationsToRun == 0) {
            logger.info("Running core migrations SKIPPED -- installed model is up-to-date (version " +
                installedModelVersion + ")");
            return;
        }
        //
        logger.info("Running " + migrationsToRun + " core migrations (installed model: version " +
            installedModelVersion + ", required model: version " + requiredModelVersion + ")");
        for (int i = installedModelVersion + 1; i <= requiredModelVersion; i++) {
            runMigration(i, null, isCleanInstall);      // plugin=null
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Collects the info needed to run a migration, runs it, and then updates the version number in the DB.
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
            // collect info
            mi = new MigrationInfo(migrationNr, plugin);
            if (!mi.success) {
                throw mi.exception;     // is not a runtime exception
            }
            mi.checkValidity();
            //
            _runMigration(mi, isCleanInstall);
            //
            updateVersionNumber(mi);
        } catch (Exception e) {
            // Note: mi is instantiated for sure
            throw new RuntimeException("Running " + mi.migrationInfo + " failed", e);
        }
    }

    /**
     * Runs a migration.
     */
    private void _runMigration(MigrationInfo mi, boolean isCleanInstall) throws Exception {
        String runInfo = " (runMode=" + mi.runMode + ", isCleanInstall=" + isCleanInstall + ")";
        if (mi.runMode.equals(MigrationRunMode.CLEAN_INSTALL.name()) == isCleanInstall ||
            mi.runMode.equals(MigrationRunMode.ALWAYS.name())) {
            logger.info("Running " + mi.migrationInfo + runInfo);
            if (mi.isDeclarative) {
                readMigrationFile(mi.migrationIn, mi.migrationFile);
            } else {
                Migration migration = (Migration) mi.migrationClass.newInstance(); // throws InstantiationException, ...
                injectServices(migration, mi.migrationInfo, mi.plugin);
                migration.setCoreService(dmx);
                logger.info("Running " + mi.migrationType + " migration class " + mi.migrationClassName);
                migration.run();
            }
        } else {
            logger.info("Running " + mi.migrationInfo + " SKIPPED" + runInfo);
        }
    }

    private void updateVersionNumber(MigrationInfo mi) {
        try {
            logger.info("Updating installed model: version " + mi.migrationNr);
            if (mi.migrationType.equals("core")) {
                dmx.pl.storeMigrationNr(mi.migrationNr);
            } else {
                mi.plugin.setMigrationNr(mi.migrationNr);
            }
        } catch (Exception e) {
            throw new RuntimeException("Updating the model version number to " + mi.migrationNr + " failed", e);
        }
    }

    // ---

    private void injectServices(Migration migration, String migrationInfo, PluginImpl plugin) {
        try {
            for (Field field : PluginImpl.getInjectableFields(migration.getClass())) {
                Class<?> serviceInterface = field.getType();
                Object service;
                //
                if (serviceInterface.getName().equals(plugin.getProvidedServiceInterface())) {
                    // the migration consumes the plugin's own service
                    service = plugin.getContext();
                } else {
                    service = plugin.getInjectedService(serviceInterface);
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
            createAssocTypes(assocTypes);
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
            dmx.createTopicType(mf.newTopicTypeModel(topicTypes.getJSONObject(i)));
        }
    }

    private void createAssocTypes(JSONArray assocTypes) throws JSONException {
        for (int i = 0; i < assocTypes.length(); i++) {
            dmx.createAssocType(mf.newAssocTypeModel(assocTypes.getJSONObject(i)));
        }
    }

    private void createTopics(JSONArray topics) throws JSONException {
        for (int i = 0; i < topics.length(); i++) {
            dmx.createTopic(mf.newTopicModel(topics.getJSONObject(i)));
        }
    }

    private void createAssociations(JSONArray assocs) throws JSONException {
        for (int i = 0; i < assocs.length(); i++) {
            dmx.createAssoc(mf.newAssocModel(assocs.getJSONObject(i)));
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Classes

    /**
     * Collects the info required to run a migration.
     */
    private class MigrationInfo {

        int migrationNr;
        PluginImpl plugin;
        //
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
        boolean success;            // error occurred while construction?
        Exception exception;        // the error

        MigrationInfo(int migrationNr, PluginImpl plugin) {
            try {
                this.migrationNr = migrationNr;
                this.plugin = plugin;
                String configFile = migrationConfigFile();
                InputStream configIn;
                migrationFile = migrationFile();
                migrationType = plugin != null ? "plugin" : "core";
                //
                if (migrationType.equals("core")) {
                    migrationInfo = "core migration " + migrationNr;
                    configIn     = getClass().getResourceAsStream(configFile);
                    migrationIn  = getClass().getResourceAsStream(migrationFile);
                    migrationClassName = coreMigrationClassName();
                    migrationClass = loadClass(migrationClassName);
                } else {
                    migrationInfo = "migration " + migrationNr + " of " + plugin;
                    configIn     = getStaticResourceOrNull(configFile);
                    migrationIn  = getStaticResourceOrNull(migrationFile);
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

        private void checkValidity() {
            if (!isDeclarative && !isImperative) {
                String message = "Neither a migration file (" + migrationFile + ") nor a migration class ";
                if (migrationClassName != null) {
                    throw new RuntimeException(message + "(" + migrationClassName + ") is found");
                } else {
                    throw new RuntimeException(message + "is found. Note: a possible migration class can't be located" +
                        " (plugin package is unknown). Consider setting \"dmx.plugin.main_package\" in " +
                        "plugin.properties");
                }
            }
            if (isDeclarative && isImperative) {
                throw new RuntimeException("Ambiguity: a migration file (" + migrationFile + ") AND a migration " +
                    "class (" + migrationClassName + ") are found. Consider using two different migration numbers.");
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
                    logger.info("Reading migration config file \"" + configFile + "\" SKIPPED -- file does not exist");
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

        private String migrationFile() {
            return "/migrations/migration" + migrationNr + ".json";
        }

        private String migrationConfigFile() {
            return "/migrations/migration" + migrationNr + ".properties";
        }

        private String coreMigrationClassName() {
            return CORE_MIGRATIONS_PACKAGE + ".Migration" + migrationNr;
        }

        // --- Helper ---

        private InputStream getStaticResourceOrNull(String resourceName) {
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
