package systems.dmx.webclient;

import systems.dmx.core.Association;
import systems.dmx.core.AssociationType;
import systems.dmx.core.DMXType;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.ViewConfiguration;
import systems.dmx.core.model.AssociationDefinitionModel;
import systems.dmx.core.model.AssociationTypeModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicTypeModel;
import systems.dmx.core.model.TypeModel;
import systems.dmx.core.model.ViewConfigurationModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.Directive;
import systems.dmx.core.service.Directives;
import systems.dmx.core.service.event.AllPluginsActiveListener;
import systems.dmx.core.service.event.IntroduceTopicTypeListener;
import systems.dmx.core.service.event.IntroduceAssociationTypeListener;
import systems.dmx.core.service.event.PostUpdateTopicListener;
import systems.dmx.core.service.event.PreCreateTopicTypeListener;
import systems.dmx.core.service.event.PreCreateAssociationTypeListener;

import java.awt.Desktop;
import java.net.URI;
import java.util.logging.Logger;



public class WebclientPlugin extends PluginActivator implements AllPluginsActiveListener,
                                                                IntroduceTopicTypeListener,
                                                                IntroduceAssociationTypeListener,
                                                                PreCreateTopicTypeListener,
                                                                PreCreateAssociationTypeListener,
                                                                PostUpdateTopicListener {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String VIEW_CONFIG_LABEL = "View Configuration";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private boolean hasWebclientLaunched = false;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void allPluginsActive() {
        String webclientUrl = getWebclientUrl();
        //
        if (hasWebclientLaunched == true) {
            logger.info("### Launching webclient (url=\"" + webclientUrl + "\") SKIPPED -- already launched");
            return;
        }
        //
        try {
            logger.info("### Launching webclient (url=\"" + webclientUrl + "\")");
            Desktop.getDesktop().browse(new URI(webclientUrl));
            hasWebclientLaunched = true;
        } catch (Exception e) {
            logger.warning("### Launching webclient failed (" + e + ")");
            logger.warning("### To launch it manually: " + webclientUrl);
        }
    }

    /**
     * Add a default view config to the type in case no one is set.
     * <p>
     * Note: the default view config needs a workspace assignment. The default view config must be added *before* the
     * assignment can take place. Workspace assignment for a type (including its components like the view config) is
     * performed by the type-introduction hook of the Workspaces module. Here we use the pre-create-type hook (instead
     * of type-introduction too) as the pre-create-type hook is guaranteed to be invoked *before* type-introduction.
     * On the other hand the order of type-introduction invocations is not deterministic accross plugins.
     */
    @Override
    public void preCreateTopicType(TopicTypeModel model) {
        addDefaultViewConfig(model);
    }

    /**
     * Add a default view config to the type in case no one is set.
     * <p>
     * Note: the default view config needs a workspace assignment. The default view config must be added *before* the
     * assignment can take place. Workspace assignment for a type (including its components like the view config) is
     * performed by the type-introduction hook of the Workspaces module. Here we use the pre-create-type hook (instead
     * of type-introduction too) as the pre-create-type hook is guaranteed to be invoked *before* type-introduction.
     * On the other hand the order of type-introduction invocations is not deterministic accross plugins.
     */
    @Override
    public void preCreateAssociationType(AssociationTypeModel model) {
        addDefaultViewConfig(model);
    }

    /**
     * Once a view configuration is updated in the DB we must update the cached view configuration model.
     */
    @Override
    public void postUpdateTopic(Topic topic, TopicModel updateModel, TopicModel oldTopic) {
        if (topic.getTypeUri().equals("dmx.webclient.view_config")) {
            updateType(topic);
            setConfigTopicLabel(topic);
        }
    }

    // ---

    @Override
    public void introduceTopicType(TopicType topicType) {
        setViewConfigLabel(topicType.getViewConfig());
    }

    @Override
    public void introduceAssociationType(AssociationType assocType) {
        setViewConfigLabel(assocType.getViewConfig());
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === View Configuration ===

    /**
     * Updates type cache once a view config topic has been updated, and adds an UPDATE-TYPE directive.
     */
    private void updateType(Topic viewConfig) {
        // type to be updated
        Topic type = viewConfig.getRelatedTopic("dmx.core.composition", "dmx.core.view_config", "dmx.core.type", null);
        // ID of the assoc def to be updated.
        // -1 if the update does not target an assoc def (but a type).
        long assocDefId = -1;
        if (type == null) {
            Association assocDef = viewConfig.getRelatedAssociation("dmx.core.composition", "dmx.core.view_config",
                "dmx.core.assoc_def", null);
            if (assocDef == null) {
                throw new RuntimeException("Orphaned view config: " + viewConfig);
            }
            type = (Topic) assocDef.getPlayer("dmx.core.parent_type");
            assocDefId = assocDef.getId();
        }
        //
        String typeUri = type.getTypeUri();
        if (typeUri.equals("dmx.core.topic_type") || typeUri.equals("dmx.core.meta_type")) {
            updateType(
                dmx.getTopicType(type.getUri()),
                assocDefId, viewConfig, Directive.UPDATE_TOPIC_TYPE
            );
        } else if (typeUri.equals("dmx.core.assoc_type")) {
            updateType(
                dmx.getAssociationType(type.getUri()),
                assocDefId, viewConfig, Directive.UPDATE_ASSOCIATION_TYPE
            );
        } else {
            throw new RuntimeException("View config " + viewConfig.getId() + " is associated unexpectedly, type=" +
                type + ", assocDefId=" + assocDefId + ", viewConfig=" + viewConfig);
        }
    }

    private void updateType(DMXType type, long assocDefId, Topic viewConfig, Directive dir) {
        logger.info("### Updating view config of type \"" + type.getUri() + "\" (assocDefId=" + assocDefId + ")");
        updateViewConfig(type, assocDefId, viewConfig);
        Directives.get().add(dir, type);        // ### TODO: should be implicit
    }

    private void updateViewConfig(DMXType type, long assocDefId, Topic viewConfig) {
        ViewConfigurationModel vcm;
        if (assocDefId == -1) {
            vcm = type.getModel().getViewConfig();
        } else {
            vcm = getAssocDef(type.getModel(), assocDefId).getViewConfig();
        }
        vcm.updateConfigTopic(viewConfig.getModel());
    }

    // --- Label ---

    private void setViewConfigLabel(ViewConfiguration viewConfig) {
        for (Topic configTopic : viewConfig.getConfigTopics()) {
            setConfigTopicLabel(configTopic);
        }
    }

    private void setConfigTopicLabel(Topic viewConfig) {
        viewConfig.setSimpleValue(VIEW_CONFIG_LABEL);
    }

    // --- Default Value ---

    /**
     * Add a default view config topic to the given type model in case no one is set already.
     * <p>
     * This ensures a programmatically created type (through a migration) will
     * have a view config in any case, for being edited interactively afterwards.
     */
    private void addDefaultViewConfig(TypeModel typeModel) {
        ViewConfigurationModel viewConfig = typeModel.getViewConfig();
        TopicModel configTopic = viewConfig.getConfigTopic("dmx.webclient.view_config");
        if (configTopic == null) {
            viewConfig.addConfigTopic(mf.newTopicModel("dmx.webclient.view_config"));
        }
    }



    // === Webclient Start ===

    private String getWebclientUrl() {
        boolean isHttpsEnabled = Boolean.getBoolean("org.apache.felix.https.enable");
        String protocol, port;
        if (isHttpsEnabled) {
            // Note: if both protocols are enabled HTTPS takes precedence
            protocol = "https";
            port = System.getProperty("org.osgi.service.http.port.secure");
        } else {
            protocol = "http";
            port = System.getProperty("org.osgi.service.http.port");
        }
        return protocol + "://localhost:" + port + "/systems.dmx.webclient/";
    }



    // === Misc ===

    private AssociationDefinitionModel getAssocDef(TypeModel type, long assocDefId) {
        for (AssociationDefinitionModel assocDef : type.getAssocDefs()) {
            if (assocDef.getId() == assocDefId) {
                return assocDef;
            }
        }
        throw new RuntimeException("Assoc def " + assocDefId + " not found in type \"" + type.getUri() + "\"");
    }
}
