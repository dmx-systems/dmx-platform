package de.deepamehta.core.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.listener.*;

import java.util.HashMap;
import java.util.Map;



public enum CoreEvent {

    PRE_CREATE_TOPIC(PreCreateTopicListener.class,
        "preCreateTopic", TopicModel.class, ClientState.class),
    POST_CREATE_TOPIC(PostCreateTopicListener.class,
        "postCreateTopic", Topic.class, ClientState.class, Directives.class),

    PRE_UPDATE_TOPIC(PreUpdateTopicListener.class,
        "preUpdateTopic", Topic.class, TopicModel.class, Directives.class),
    POST_UPDATE_TOPIC(PostUpdateTopicListener.class,
        "postUpdateTopic", Topic.class, TopicModel.class, TopicModel.class, ClientState.class, Directives.class),

    PRE_DELETE_ASSOCIATION(PreDeleteAssociationListener.class,
        "preDeleteAssociation", Association.class, Directives.class),
    POST_DELETE_ASSOCIATION(PostDeleteAssociationListener.class,
        "postDeleteAssociation", Association.class, Directives.class),

    POST_RETYPE_ASSOCIATION(PostRetypeAssociationListener.class,
        "postRetypeAssociation", Association.class, String.class, Directives.class),
    // ### TODO: remove this hook. Retype is special case of update.

    PRE_SEND_TOPIC(PreSendTopicListener.class,
        "preSendTopic", Topic.class, ClientState.class),
    PRE_SEND_TOPIC_TYPE(PreSendTopicTypeListener.class,
        "preSendTopicType", TopicType.class, ClientState.class),

    POST_INSTALL_PLUGIN(PostInstallPluginListener.class,
        "postInstallPlugin"),
    // Note: this is an internal plugin event. It is fired for a specific plugin only.
    // (see {@link de.deepamehta.core.service.Plugin#initializePlugin}).
    // ### TODO: remove this event. Use migration 1 instead.

    ALL_PLUGINS_READY(AllPluginsReadyListener.class,
        "allPluginsReady"),

    INTRODUCE_TOPIC_TYPE(IntroduceTopicTypeListener.class,
        "introduceTopicType", TopicType.class, ClientState.class),
    // Note: besides regular triggering (see {@link #createTopicType})
    // this hook is triggered by the plugin itself
    // (see {@link de.deepamehta.core.service.Plugin#introduceTypesToPlugin}).

    SERVICE_ARRIVED(ServiceArrivedListener.class,
        "serviceArrived", PluginService.class),
    // Note: this hook is triggered only by the plugin itself
    // (see {@link de.deepamehta.core.service.Plugin#createServiceTracker}).
    SERVICE_GONE(ServiceGoneListener.class,
        "serviceGone", PluginService.class);
    // Note: this hook is triggered only by the plugin itself
    // (see {@link de.deepamehta.core.service.Plugin#createServiceTracker}).

    // ### TODO: transform the other hooks into events

    public final Class listenerInterface;
    public final String handlerMethodName;
    public final Class[] paramClasses;

    private static Map<String, CoreEvent> events;

    // ---------------------------------------------------------------------------------------------------- Constructors

    private CoreEvent(Class listenerInterface, String handlerMethodName, Class... paramClasses) {
        this.listenerInterface = listenerInterface;
        this.handlerMethodName = handlerMethodName;
        this.paramClasses = paramClasses;
        // events.put(..);      // ### Doesn't compile: "illegal reference to static field from initializer".
                                // ### Enum constants are initialzed before other static fields.
        put(this);              // ### Lazy initialization outside the constructor solves it.
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public static CoreEvent fromListenerInterface(Class listenerInterface) {
        return events.get(listenerInterface.getSimpleName());
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void put(CoreEvent event) {
        // ### must initialize lazily, see above
        if (events == null) {
            events = new HashMap();
        }
        //
        events.put(event.listenerInterface.getSimpleName(), event);
    }
}
