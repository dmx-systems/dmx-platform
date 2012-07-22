package de.deepamehta.core.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.listener.*;

import java.util.HashMap;
import java.util.Map;



/**
 * Events fired by the DeepaMehta core.
 * Plugins can listen to these events by implementing the respective listener interfaces.
 *
 * There are 2 types of events:
 *   - regular events: are fired (usually) by the core and then delivered to all registered listeners (plugins).
 *   - internal plugin events: are fired by a plugin and then delivered only to itself. There are 4 internal events:
 *     - POST_INSTALL_PLUGIN
 *     - INTRODUCE_TOPIC_TYPE (has a double nature)
 *     - PLUGIN_SERVICE_ARRIVED
 *     - PLUGIN_SERVICE_GONE
 *
 * @see de.deepamehta.core.service.listener
 */
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

    POST_RETYPE_ASSOCIATION(PostRetypeAssociationListener.class,    // ### TODO: remove this event. Retype is special 
        "postRetypeAssociation", Association.class, String.class, Directives.class),             // case of update.

    PRE_SEND_TOPIC(PreSendTopicListener.class,
        "preSendTopic", Topic.class, ClientState.class),
    PRE_SEND_TOPIC_TYPE(PreSendTopicTypeListener.class,
        "preSendTopicType", TopicType.class, ClientState.class),

    POST_INSTALL_PLUGIN(PostInstallPluginListener.class,    // ### TODO: remove this event. Use migration 1 instead.
        "postInstallPlugin"),
    // Note: this is an internal plugin event (see {@link Plugin#installPluginInDB}).

    ALL_PLUGINS_ACTIVE(AllPluginsActiveListener.class,
        "allPluginsActive"),

    INTRODUCE_TOPIC_TYPE(IntroduceTopicTypeListener.class,
        "introduceTopicType", TopicType.class, ClientState.class),
    // Note: besides regular firing           (see {@link EmbeddedService#createTopicType})
    // this is an internal plugin event       (see {@link Plugin#introduceTypesToPlugin}).

    PLUGIN_SERVICE_ARRIVED(PluginServiceArrivedListener.class,
        "pluginServiceArrived", PluginService.class),
    // Note: this is an internal plugin event (see {@link Plugin#createServiceTracker}).
    PLUGIN_SERVICE_GONE(PluginServiceGoneListener.class,
        "pluginServiceGone", PluginService.class)
    // Note: this is an internal plugin event (see {@link Plugin#createServiceTracker}).
    ;

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
