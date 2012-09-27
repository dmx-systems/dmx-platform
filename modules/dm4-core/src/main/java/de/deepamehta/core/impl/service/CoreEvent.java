package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Listener;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.event.*;

import java.util.HashMap;
import java.util.Map;



/**
 * Events fired by the DeepaMehta core.
 * Plugins can listen to these events by implementing the respective listener interfaces.
 *
 * There are 2 types of events:
 *   - regular events: are fired (usually) by the core and then delivered to all registered listeners (plugins).
 *   - internal plugin events: are fired by a plugin and then delivered only to itself. There are 5 internal events:
 *     - POST_INSTALL_PLUGIN
 *     - INTRODUCE_TOPIC_TYPE (has a double nature)
 *     - INITIALIZE_PLUGIN
 *     - PLUGIN_SERVICE_ARRIVED
 *     - PLUGIN_SERVICE_GONE
 *
 * @see de.deepamehta.core.service.event
 */
enum CoreEvent {

    PRE_CREATE_TOPIC(PreCreateTopicListener.class,
        "preCreateTopic", TopicModel.class, ClientState.class),
    PRE_CREATE_ASSOCIATION(PreCreateAssociationListener.class,
        "preCreateAssociation", AssociationModel.class, ClientState.class),

    POST_CREATE_TOPIC(PostCreateTopicListener.class,
        "postCreateTopic", Topic.class, ClientState.class, Directives.class),
    POST_CREATE_ASSOCIATION(PostCreateAssociationListener.class,
        "postCreateAssociation", Association.class, ClientState.class, Directives.class),

    PRE_UPDATE_TOPIC(PreUpdateTopicListener.class,
        "preUpdateTopic", Topic.class, TopicModel.class, Directives.class),
    POST_UPDATE_TOPIC(PostUpdateTopicListener.class,
        "postUpdateTopic", Topic.class, TopicModel.class, TopicModel.class, ClientState.class, Directives.class),

    PRE_DELETE_ASSOCIATION(PreDeleteAssociationListener.class,
        "preDeleteAssociation", Association.class, Directives.class),
    POST_DELETE_ASSOCIATION(PostDeleteAssociationListener.class,
        "postDeleteAssociation", Association.class, Directives.class),

    // ### TODO: remove this event. Retype is special case of update
    POST_RETYPE_ASSOCIATION(PostRetypeAssociationListener.class,
        "postRetypeAssociation", Association.class, String.class, Directives.class),

    PRE_SEND_TOPIC(PreSendTopicListener.class,
        "preSendTopic", Topic.class, ClientState.class),
    PRE_SEND_ASSOCIATION(PreSendAssociationListener.class,
        "preSendAssociation", Association.class, ClientState.class),
    PRE_SEND_TOPIC_TYPE(PreSendTopicTypeListener.class,
        "preSendTopicType", TopicType.class, ClientState.class),

    ALL_PLUGINS_ACTIVE(AllPluginsActiveListener.class,
        "allPluginsActive"),

    // === Internal plugin events ===

    // ### TODO: remove this event. Use migration 1 instead.
    POST_INSTALL_PLUGIN(PostInstallPluginListener.class,
        "postInstallPlugin"),

    INTRODUCE_TOPIC_TYPE(IntroduceTopicTypeListener.class,
        "introduceTopicType", TopicType.class, ClientState.class),

    INITIALIZE_PLUGIN(InitializePluginListener.class,
        "initializePlugin"),

    PLUGIN_SERVICE_ARRIVED(PluginServiceArrivedListener.class,
        "pluginServiceArrived", PluginService.class),
    PLUGIN_SERVICE_GONE(PluginServiceGoneListener.class,
        "pluginServiceGone", PluginService.class);

    // ------------------------------------------------------------------------------------------------- Class Variables

    private static final Map<String, CoreEvent> events = new HashMap<String, CoreEvent>();

    static {
        for (CoreEvent event : CoreEvent.values()) {
            events.put(event.listenerInterface.getSimpleName(), event);
        }
    }

    // ---------------------------------------------------------------------------------------------- Instance Variables

    final Class<? extends Listener> listenerInterface;
    final String handlerMethodName;
    final Class<?>[] paramClasses;

    // ---------------------------------------------------------------------------------------------------- Constructors

    private CoreEvent(Class<? extends Listener> listenerInterface, String handlerMethodName, Class<?>... paramClasses) {
        this.listenerInterface = listenerInterface;
        this.handlerMethodName = handlerMethodName;
        this.paramClasses = paramClasses;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    static CoreEvent fromListenerInterface(Class<Listener> listenerInterface) {
        return events.get(listenerInterface.getSimpleName());
    }

}
