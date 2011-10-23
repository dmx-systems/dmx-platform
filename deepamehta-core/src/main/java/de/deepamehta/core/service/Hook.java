package de.deepamehta.core.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.TopicModel;



public enum Hook {

    // Note: this hook is triggered only by the plugin itself
    // (see {@link de.deepamehta.core.service.Plugin#initPlugin}).
    // It is declared here for documentation purpose only.
    // ### FIXME: remove hook. Use migration 1 instead.
    POST_INSTALL_PLUGIN("postInstallPluginHook"),
    ALL_PLUGINS_READY("allPluginsReadyHook"),

    // Note: this hook is triggered only by the plugin itself
    // (see {@link de.deepamehta.core.service.Plugin#createServiceTracker}).
    // It is declared here for documentation purpose only.
    SERVICE_ARRIVED("serviceArrived", PluginService.class),
    // Note: this hook is triggered only by the plugin itself
    // (see {@link de.deepamehta.core.service.Plugin#createServiceTracker}).
    // It is declared here for documentation purpose only.
    SERVICE_GONE("serviceGone", PluginService.class),

     PRE_CREATE_TOPIC("preCreateHook",  TopicModel.class, ClientContext.class),
    POST_CREATE_TOPIC("postCreateHook", Topic.class,      ClientContext.class, Directives.class),
     PRE_UPDATE_TOPIC("preUpdateHook",  Topic.class, TopicModel.class, Directives.class),
    POST_UPDATE_TOPIC("postUpdateHook", Topic.class, TopicModel.class, ClientContext.class, Directives.class),

    // ### FIXME: remove hook. Retype is special case of update.
    POST_RETYPE_ASSOCIATION("postRetypeAssociationHook", Association.class, String.class, Directives.class),

     PRE_DELETE_ASSOCIATION("preDeleteAssociationHook",  Association.class, Directives.class),
    POST_DELETE_ASSOCIATION("postDeleteAssociationHook", Association.class, Directives.class),

    PROVIDE_TOPIC_PROPERTIES("providePropertiesHook", Topic.class),
    PROVIDE_RELATION_PROPERTIES("providePropertiesHook", Association.class),

    ENRICH_TOPIC("enrichTopicHook", Topic.class, ClientContext.class),
    ENRICH_TOPIC_TYPE("enrichTopicTypeHook", TopicType.class, ClientContext.class),

    // Note: besides regular triggering (see {@link #createTopicType})
    // this hook is triggered by the plugin itself
    // (see {@link de.deepamehta.core.service.Plugin#introduceTypesToPlugin}).
    MODIFY_TOPIC_TYPE("modifyTopicTypeHook", TopicType.class, ClientContext.class),

    EXECUTE_COMMAND("executeCommandHook", String.class, CommandParams.class, ClientContext.class);

    private final String methodName;
    private final Class[] paramClasses;

    private Hook(String methodName, Class... paramClasses) {
        this.methodName = methodName;
        this.paramClasses = paramClasses;
    }

    public String getMethodName() {
        return methodName;
    }

    public Class[] getParamClasses() {
        return paramClasses;
    }
}
