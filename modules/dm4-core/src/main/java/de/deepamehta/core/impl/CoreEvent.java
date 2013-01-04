package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.event.*;

import java.util.HashMap;
import java.util.Map;



/**
 * Events fired by the DeepaMehta core service.
 * Plugins can listen to these events by implementing the respective listener interfaces.
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
    PRE_UPDATE_ASSOCIATION(PreUpdateAssociationListener.class,
        "preUpdateAssociation", Association.class, AssociationModel.class, Directives.class),

    POST_UPDATE_TOPIC(PostUpdateTopicListener.class,
        "postUpdateTopic", Topic.class, TopicModel.class, TopicModel.class, ClientState.class, Directives.class),
    POST_UPDATE_ASSOCIATION(PostUpdateAssociationListener.class,
        "postUpdateAssociation", Association.class, AssociationModel.class, ClientState.class, Directives.class),

    PRE_DELETE_ASSOCIATION(PreDeleteAssociationListener.class,
        "preDeleteAssociation", Association.class, Directives.class),
    POST_DELETE_ASSOCIATION(PostDeleteAssociationListener.class,
        "postDeleteAssociation", Association.class, Directives.class),

    PRE_SEND_TOPIC(PreSendTopicListener.class,
        "preSendTopic", Topic.class, ClientState.class),
    PRE_SEND_ASSOCIATION(PreSendAssociationListener.class,
        "preSendAssociation", Association.class, ClientState.class),
    PRE_SEND_TOPIC_TYPE(PreSendTopicTypeListener.class,
        "preSendTopicType", TopicType.class, ClientState.class),
    PRE_SEND_ASSOCIATION_TYPE(PreSendAssociationTypeListener.class,
        "preSendAssociationType", AssociationType.class, ClientState.class),

    ALL_PLUGINS_ACTIVE(AllPluginsActiveListener.class,
        "allPluginsActive"),

    // This event has a double nature:
    //   a) it is fired regularily (see EmbeddedService.createTopicType()).
    //   b) it is fired locally (see PluginImpl.introduceTopicTypesToPlugin()).
    INTRODUCE_TOPIC_TYPE(IntroduceTopicTypeListener.class,
        "introduceTopicType", TopicType.class, ClientState.class),
    // This event has a double nature:
    //   a) it is fired regularily (see EmbeddedService.createAssociationType()).
    //   b) it is fired locally (see PluginImpl.introduceAssociationTypesToPlugin()).
    INTRODUCE_ASSOCIATION_TYPE(IntroduceAssociationTypeListener.class,
        "introduceAssociationType", AssociationType.class, ClientState.class);


    // ------------------------------------------------------------------------------------------------- Class Variables

    private static Map<String, CoreEvent> events;

    // ---------------------------------------------------------------------------------------------- Instance Variables

    final Class listenerInterface;
    final String handlerMethodName;
    final Class[] paramClasses;

    // ---------------------------------------------------------------------------------------------------- Constructors

    private CoreEvent(Class listenerInterface, String handlerMethodName, Class... paramClasses) {
        this.listenerInterface = listenerInterface;
        this.handlerMethodName = handlerMethodName;
        this.paramClasses = paramClasses;
        // events.put(..);      // ### Doesn't compile: "illegal reference to static field from initializer".
                                // ### Enum constants are initialzed before other static fields.
        put(this);              // ### Lazy initialization outside the constructor solves it.
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    static CoreEvent fromListenerInterface(Class listenerInterface) {
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
