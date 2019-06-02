package systems.dmx.webclient;

import systems.dmx.core.Association;
import systems.dmx.core.AssociationType;
import systems.dmx.core.DMXType;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.ViewConfiguration;
import systems.dmx.core.model.AssociationTypeModel;
import systems.dmx.core.model.CompDefModel;
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

    // ---

    /**
     * Once a view config topic is updated we must update the cached view config and inform the webclient.
     */
    @Override
    public void postUpdateTopic(Topic topic, TopicModel updateModel, TopicModel oldTopic) {
        if (topic.getTypeUri().equals("dmx.webclient.view_config")) {
            setDefaultConfigTopicLabel(topic);
            updateTypeCacheAndAddDirective(topic);
        }
    }

    // ---

    @Override
    public void introduceTopicType(TopicType topicType) {
        setViewConfigLabel(topicType);
    }

    @Override
    public void introduceAssociationType(AssociationType assocType) {
        setViewConfigLabel(assocType);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === View Configuration ===

    /**
     * Updates type cache according to the given view config topic, and adds an UPDATE-TYPE directive.
     * Called once a view config topic has been updated.
     *
     * Determines the type and possibly the assoc def the given view config topic belongs to.
     */
    private void updateTypeCacheAndAddDirective(Topic viewConfigTopic) {
        // type to be updated (topic type or assoc type)
        Topic type = viewConfigTopic.getRelatedTopic("dmx.core.composition", "dmx.core.child", "dmx.core.parent", null);
        // ID of the assoc def to be updated. -1 if the update does not target an assoc def (but a type).
        long assocDefId = -1;
        if (type == null) {
            Association assocDef = viewConfigTopic.getRelatedAssociation("dmx.core.composition", "dmx.core.child",
                "dmx.core.parent", "dmx.core.composition_def");
            if (assocDef == null) {
                throw new RuntimeException("Orphaned view config topic: " + viewConfigTopic);
            }
            type = (Topic) assocDef.getPlayer("dmx.core.parent_type");
            assocDefId = assocDef.getId();
        }
        //
        String typeUri = type.getTypeUri();
        if (typeUri.equals("dmx.core.topic_type") || typeUri.equals("dmx.core.meta_type")) {
            _updateTypeCacheAndAddDirective(
                dmx.getTopicType(type.getUri()),
                assocDefId, viewConfigTopic, Directive.UPDATE_TOPIC_TYPE
            );
        } else if (typeUri.equals("dmx.core.assoc_type")) {
            _updateTypeCacheAndAddDirective(
                dmx.getAssociationType(type.getUri()),
                assocDefId, viewConfigTopic, Directive.UPDATE_ASSOCIATION_TYPE
            );
        } else {
            throw new RuntimeException("View config " + viewConfigTopic.getId() + " is associated unexpectedly, type=" +
                type + ", assocDefId=" + assocDefId + ", viewConfigTopic=" + viewConfigTopic);
        }
    }

    private void _updateTypeCacheAndAddDirective(DMXType type, long assocDefId, Topic viewConfigTopic, Directive dir) {
        logger.info("### Updating view config of type \"" + type.getUri() + "\" (assocDefId=" + assocDefId + ")");
        updateTypeCache(type.getModel(), assocDefId, viewConfigTopic.getModel());
        Directives.get().add(dir, type);        // ### TODO: should be implicit
    }

    /**
     * Overrides the cached view config topic for the given type/assoc def with the given view config topic.
     */
    private void updateTypeCache(TypeModel type, long assocDefId, TopicModel viewConfigTopic) {
        ViewConfigurationModel vcm;
        if (assocDefId == -1) {
            vcm = type.getViewConfig();
        } else {
            vcm = getCompDef(type, assocDefId).getViewConfig();
        }
        vcm.updateConfigTopic(viewConfigTopic);
    }

    // --- Label ---

    private void setViewConfigLabel(DMXType type) {
        // type
        setViewConfigLabel(type.getViewConfig());
        // assoc defs
        for (String compDefUri : type) {
            setViewConfigLabel(type.getCompDef(compDefUri).getViewConfig());
        }
    }

    private void setViewConfigLabel(ViewConfiguration viewConfig) {
        for (Topic configTopic : viewConfig.getConfigTopics()) {
            setDefaultConfigTopicLabel(configTopic);
        }
    }

    private void setDefaultConfigTopicLabel(Topic viewConfigTopic) {
        viewConfigTopic.setSimpleValue(VIEW_CONFIG_LABEL);
    }

    // --- Default Config Topic ---

    /**
     * Adds a default view config topic to the given type (and its assoc defs) in case no one is set already.
     * <p>
     * This ensures a programmatically created type (through a migration) will
     * have a view config in any case, for being edited interactively afterwards.
     */
    private void addDefaultViewConfig(TypeModel typeModel) {
        // type
        addDefaultViewConfigTopic(typeModel.getViewConfig());
        // assoc defs
        for (String compDefUri : typeModel) {
            addDefaultViewConfigTopic(typeModel.getCompDef(compDefUri).getViewConfig());
        }
    }

    private void addDefaultViewConfigTopic(ViewConfigurationModel viewConfig) {
        if (viewConfig.getConfigTopic("dmx.webclient.view_config") == null) {
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

    /**
     * Looks up an assoc def by ID.
     */
    private CompDefModel getCompDef(TypeModel type, long assocDefId) {
        for (CompDefModel assocDef : type.getCompDefs()) {
            if (assocDef.getId() == assocDefId) {
                return assocDef;
            }
        }
        throw new RuntimeException("Assoc def " + assocDefId + " not found in type \"" + type.getUri() + "\"");
    }
}
