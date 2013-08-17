package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Listener;
import de.deepamehta.core.service.event.*;

// ### TODO: hide Jersey internals. Move to JAX-RS 2.0.
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;



/**
 * Events fired by the DeepaMehta core service.
 * Plugins can listen to these events by implementing the respective listener interfaces.
 *
 * @see de.deepamehta.core.service.event
 */
enum CoreEvent {

    PRE_CREATE_TOPIC(PreCreateTopicListener.class) {
        @Override
        void deliver(Listener listener, Object... params) {
            ((PreCreateTopicListener) listener).preCreateTopic(
                (TopicModel) params[0], (ClientState) params[1]
            );
        }
    },
    PRE_CREATE_ASSOCIATION(PreCreateAssociationListener.class) {
        @Override
        void deliver(Listener listener, Object... params) {
            ((PreCreateAssociationListener) listener).preCreateAssociation(
                (AssociationModel) params[0], (ClientState) params[1]
            );
        }
    },

    POST_CREATE_TOPIC(PostCreateTopicListener.class) {
        @Override
        void deliver(Listener listener, Object... params) {
            ((PostCreateTopicListener) listener).postCreateTopic(
                (Topic) params[0], (ClientState) params[1], (Directives) params[2]
            );
        }
    },
    POST_CREATE_ASSOCIATION(PostCreateAssociationListener.class) {
        @Override
        void deliver(Listener listener, Object... params) {
            ((PostCreateAssociationListener) listener).postCreateAssociation(
                (Association) params[0], (ClientState) params[1], (Directives) params[2]
            );
        }
    },

    PRE_UPDATE_TOPIC(PreUpdateTopicListener.class) {
        @Override
        void deliver(Listener listener, Object... params) {
            ((PreUpdateTopicListener) listener).preUpdateTopic(
                (Topic) params[0], (TopicModel) params[1], (Directives) params[2]
            );
        }
    },
    PRE_UPDATE_ASSOCIATION(PreUpdateAssociationListener.class) {
        @Override
        void deliver(Listener listener, Object... params) {
            ((PreUpdateAssociationListener) listener).preUpdateAssociation(
                (Association) params[0], (AssociationModel) params[1], (Directives) params[2]
            );
        }
    },

    POST_UPDATE_TOPIC(PostUpdateTopicListener.class) {
        @Override
        void deliver(Listener listener, Object... params) {
            ((PostUpdateTopicListener) listener).postUpdateTopic(
                (Topic) params[0], (TopicModel) params[1], (TopicModel) params[2], (ClientState) params[3],
                (Directives) params[4]
            );
        }
    },
    POST_UPDATE_ASSOCIATION(PostUpdateAssociationListener.class) {
        @Override
        void deliver(Listener listener, Object... params) {
            ((PostUpdateAssociationListener) listener).postUpdateAssociation(
                (Association) params[0], (AssociationModel) params[1], (ClientState) params[2], (Directives) params[3]
            );
        }
    },

    PRE_DELETE_ASSOCIATION(PreDeleteAssociationListener.class) {
        @Override
        void deliver(Listener listener, Object... params) {
            ((PreDeleteAssociationListener) listener).preDeleteAssociation(
                (Association) params[0], (Directives) params[1]
            );
        }
    },
    POST_DELETE_ASSOCIATION(PostDeleteAssociationListener.class) {
        @Override
        void deliver(Listener listener, Object... params) {
            ((PostDeleteAssociationListener) listener).postDeleteAssociation(
                (Association) params[0], (Directives) params[1]
            );
        }
    },

    SERVICE_REQUEST_FILTER(ServiceRequestFilterListener.class) {
        @Override
        void deliver(Listener listener, Object... params) {
            ((ServiceRequestFilterListener) listener).serviceRequestFilter(
                (ContainerRequest) params[0]
            );
        }
    },
    SERVICE_RESPONSE_FILTER(ServiceResponseFilterListener.class) {
        @Override
        void deliver(Listener listener, Object... params) {
            ((ServiceResponseFilterListener) listener).serviceResponseFilter(
                (ContainerResponse) params[0]
            );
        }
    },
    RESOURCE_REQUEST_FILTER(ResourceRequestFilterListener.class) {
        @Override
        void deliver(Listener listener, Object... params) {
            ((ResourceRequestFilterListener) listener).resourceRequestFilter(
                (HttpServletRequest) params[0]
            );
        }
    },

    PRE_SEND_TOPIC(PreSendTopicListener.class) {
        @Override
        void deliver(Listener listener, Object... params) {
            ((PreSendTopicListener) listener).preSendTopic(
                (Topic) params[0], (ClientState) params[1]
            );
        }
    },
    PRE_SEND_ASSOCIATION(PreSendAssociationListener.class) {
        @Override
        void deliver(Listener listener, Object... params) {
            ((PreSendAssociationListener) listener).preSendAssociation(
                (Association) params[0], (ClientState) params[1]
            );
        }
    },
    PRE_SEND_TOPIC_TYPE(PreSendTopicTypeListener.class) {
        @Override
        void deliver(Listener listener, Object... params) {
            ((PreSendTopicTypeListener) listener).preSendTopicType(
                (TopicType) params[0], (ClientState) params[1]
            );
        }
    },
    PRE_SEND_ASSOCIATION_TYPE(PreSendAssociationTypeListener.class) {
        @Override
        void deliver(Listener listener, Object... params) {
            ((PreSendAssociationTypeListener) listener).preSendAssociationType(
                (AssociationType) params[0], (ClientState) params[1]
            );
        }
    },

    ALL_PLUGINS_ACTIVE(AllPluginsActiveListener.class) {
        @Override
        void deliver(Listener listener, Object... params) {
            ((AllPluginsActiveListener) listener).allPluginsActive();
        }
    },

    // This event has a double nature:
    //   a) it is fired regularily (see EmbeddedService.createTopicType()).
    //   b) it is fired locally (see PluginImpl.introduceTopicTypesToPlugin()).
    INTRODUCE_TOPIC_TYPE(IntroduceTopicTypeListener.class) {
        @Override
        void deliver(Listener listener, Object... params) {
            ((IntroduceTopicTypeListener) listener).introduceTopicType(
                (TopicType) params[0], (ClientState) params[1]
            );
        }
    },
    // This event has a double nature:
    //   a) it is fired regularily (see EmbeddedService.createAssociationType()).
    //   b) it is fired locally (see PluginImpl.introduceAssociationTypesToPlugin()).
    INTRODUCE_ASSOCIATION_TYPE(IntroduceAssociationTypeListener.class) {
        @Override
        void deliver(Listener listener, Object... params) {
            ((IntroduceAssociationTypeListener) listener).introduceAssociationType(
                (AssociationType) params[0], (ClientState) params[1]
            );
        }
    };


    // ------------------------------------------------------------------------------------------------- Class Variables

    private static Map<String, CoreEvent> events;

    // ---------------------------------------------------------------------------------------------- Instance Variables

    final Class listenerInterface;

    // ---------------------------------------------------------------------------------------------------- Constructors

    private CoreEvent(Class listenerInterface) {
        this.listenerInterface = listenerInterface;
        // events.put(..);      // ### Doesn't compile: "illegal reference to static field from initializer".
                                // ### Enum constants are initialzed before other static fields.
        put(this);              // ### Lazy initialization outside the constructor solves it.
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    abstract void deliver(Listener listener, Object... params);

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
