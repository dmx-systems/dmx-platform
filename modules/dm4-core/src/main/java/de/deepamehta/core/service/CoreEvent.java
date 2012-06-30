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

    PRE_SEND_TOPIC(PreSendTopicListener.class,
        "preSendTopic", Topic.class, ClientState.class),
    PRE_SEND_TOPIC_TYPE(PreSendTopicTypeListener.class,
        "preSendTopicType", TopicType.class, ClientState.class),

    POST_INSTALL_PLUGIN(PostInstallPluginListener.class,
        "postInstallPlugin");
    // Note: this is an internal plugin event. It is fired for a specific plugin only.
    // (see {@link de.deepamehta.core.service.Plugin#initializePlugin}).
    // ### TODO: remove this event. Use migration 1 instead.

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
        // events.put(this);
        // ### Doesn't compile: "illegal reference to static field from initializer".
        // ### Enum constants are initialzed before other static fields.
        // ### Lazy initialization outside the constructor solves it.
        put(this);
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
