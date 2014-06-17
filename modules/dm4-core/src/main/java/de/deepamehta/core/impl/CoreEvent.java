package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.DeepaMehtaEvent;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.EventListener;
import de.deepamehta.core.service.event.*;

// ### TODO: hide Jersey internals. Move to JAX-RS 2.0.
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;

import javax.servlet.http.HttpServletRequest;



/**
 * Events fired by the DeepaMehta core service.
 * Plugins can listen to these events by implementing the respective listener interfaces.
 *
 * @see de.deepamehta.core.service.event
 */
class CoreEvent {

    static DeepaMehtaEvent POST_GET_TOPIC = new DeepaMehtaEvent(PostGetTopicListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PostGetTopicListener) listener).postGetTopic(
                (Topic) params[0]
            );
        }
    };

    static DeepaMehtaEvent POST_GET_ASSOCIATION = new DeepaMehtaEvent(PostGetAssociationListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PostGetAssociationListener) listener).postGetAssociation(
                (Association) params[0]
            );
        }
    };

    // ---

    static DeepaMehtaEvent PRE_CREATE_TOPIC = new DeepaMehtaEvent(PreCreateTopicListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PreCreateTopicListener) listener).preCreateTopic(
                (TopicModel) params[0], (ClientState) params[1]
            );
        }
    };

    static DeepaMehtaEvent PRE_CREATE_ASSOCIATION = new DeepaMehtaEvent(PreCreateAssociationListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PreCreateAssociationListener) listener).preCreateAssociation(
                (AssociationModel) params[0], (ClientState) params[1]
            );
        }
    };

    // ---

    static DeepaMehtaEvent POST_CREATE_TOPIC = new DeepaMehtaEvent(PostCreateTopicListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PostCreateTopicListener) listener).postCreateTopic(
                (Topic) params[0], (ClientState) params[1], (Directives) params[2]
            );
        }
    };

    static DeepaMehtaEvent POST_CREATE_ASSOCIATION = new DeepaMehtaEvent(PostCreateAssociationListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PostCreateAssociationListener) listener).postCreateAssociation(
                (Association) params[0], (ClientState) params[1], (Directives) params[2]
            );
        }
    };

    // ---

    static DeepaMehtaEvent PRE_UPDATE_TOPIC = new DeepaMehtaEvent(PreUpdateTopicListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PreUpdateTopicListener) listener).preUpdateTopic(
                (Topic) params[0], (TopicModel) params[1], (Directives) params[2]
            );
        }
    };

    static DeepaMehtaEvent PRE_UPDATE_ASSOCIATION = new DeepaMehtaEvent(PreUpdateAssociationListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PreUpdateAssociationListener) listener).preUpdateAssociation(
                (Association) params[0], (AssociationModel) params[1], (Directives) params[2]
            );
        }
    };

    // ---

    static DeepaMehtaEvent POST_UPDATE_TOPIC = new DeepaMehtaEvent(PostUpdateTopicListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PostUpdateTopicListener) listener).postUpdateTopic(
                (Topic) params[0], (TopicModel) params[1], (TopicModel) params[2], (ClientState) params[3],
                (Directives) params[4]
            );
        }
    };

    static DeepaMehtaEvent POST_UPDATE_ASSOCIATION = new DeepaMehtaEvent(PostUpdateAssociationListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PostUpdateAssociationListener) listener).postUpdateAssociation(
                (Association) params[0], (AssociationModel) params[1], (ClientState) params[2], (Directives) params[3]
            );
        }
    };

    // ---

    static DeepaMehtaEvent POST_UPDATE_TOPIC_REQUEST = new DeepaMehtaEvent(PostUpdateTopicRequestListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PostUpdateTopicRequestListener) listener).postUpdateTopicRequest(
                (Topic) params[0]
            );
        }
    };

    // Note: a corresponding POST_UPDATE_ASSOCIATION_REQUEST event is not necessary.
    // It would be equivalent to POST_UPDATE_ASSOCIATION.
    // Per request exactly one association is updated. Its childs are topics (never associations).

    // ---

    static DeepaMehtaEvent PRE_DELETE_TOPIC = new DeepaMehtaEvent(PreDeleteTopicListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PreDeleteTopicListener) listener).preDeleteTopic(
                (Topic) params[0], (Directives) params[1]
            );
        }
    };

    static DeepaMehtaEvent PRE_DELETE_ASSOCIATION = new DeepaMehtaEvent(PreDeleteAssociationListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PreDeleteAssociationListener) listener).preDeleteAssociation(
                (Association) params[0], (Directives) params[1]
            );
        }
    };

    // ---

    static DeepaMehtaEvent POST_DELETE_TOPIC = new DeepaMehtaEvent(PostDeleteTopicListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PostDeleteTopicListener) listener).postDeleteTopic(
                (Topic) params[0], (Directives) params[1]
            );
        }
    };

    static DeepaMehtaEvent POST_DELETE_ASSOCIATION = new DeepaMehtaEvent(PostDeleteAssociationListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PostDeleteAssociationListener) listener).postDeleteAssociation(
                (Association) params[0], (Directives) params[1]
            );
        }
    };

    // ---

    static DeepaMehtaEvent SERVICE_REQUEST_FILTER = new DeepaMehtaEvent(ServiceRequestFilterListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((ServiceRequestFilterListener) listener).serviceRequestFilter(
                (ContainerRequest) params[0]
            );
        }
    };

    static DeepaMehtaEvent SERVICE_RESPONSE_FILTER = new DeepaMehtaEvent(ServiceResponseFilterListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((ServiceResponseFilterListener) listener).serviceResponseFilter(
                (ContainerResponse) params[0]
            );
        }
    };

    static DeepaMehtaEvent RESOURCE_REQUEST_FILTER = new DeepaMehtaEvent(ResourceRequestFilterListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((ResourceRequestFilterListener) listener).resourceRequestFilter(
                (HttpServletRequest) params[0]
            );
        }
    };

    // ---

    static DeepaMehtaEvent PRE_SEND_TOPIC = new DeepaMehtaEvent(PreSendTopicListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PreSendTopicListener) listener).preSendTopic(
                (Topic) params[0], (ClientState) params[1]
            );
        }
    };

    static DeepaMehtaEvent PRE_SEND_ASSOCIATION = new DeepaMehtaEvent(PreSendAssociationListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PreSendAssociationListener) listener).preSendAssociation(
                (Association) params[0], (ClientState) params[1]
            );
        }
    };

    static DeepaMehtaEvent PRE_SEND_TOPIC_TYPE = new DeepaMehtaEvent(PreSendTopicTypeListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PreSendTopicTypeListener) listener).preSendTopicType(
                (TopicType) params[0], (ClientState) params[1]
            );
        }
    };

    static DeepaMehtaEvent PRE_SEND_ASSOCIATION_TYPE = new DeepaMehtaEvent(PreSendAssociationTypeListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PreSendAssociationTypeListener) listener).preSendAssociationType(
                (AssociationType) params[0], (ClientState) params[1]
            );
        }
    };

    // ---

    static DeepaMehtaEvent ALL_PLUGINS_ACTIVE = new DeepaMehtaEvent(AllPluginsActiveListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((AllPluginsActiveListener) listener).allPluginsActive();
        }
    };

    // ---

    // This event has a double nature:
    //   a) it is fired regularily (see EmbeddedService.createTopicType()).
    //   b) it is fired locally (see PluginImpl.introduceTopicTypesToPlugin()).
    static DeepaMehtaEvent INTRODUCE_TOPIC_TYPE = new DeepaMehtaEvent(IntroduceTopicTypeListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((IntroduceTopicTypeListener) listener).introduceTopicType(
                (TopicType) params[0], (ClientState) params[1]
            );
        }
    };

    // This event has a double nature:
    //   a) it is fired regularily (see EmbeddedService.createAssociationType()).
    //   b) it is fired locally (see PluginImpl.introduceAssociationTypesToPlugin()).
    static DeepaMehtaEvent INTRODUCE_ASSOCIATION_TYPE = new DeepaMehtaEvent(IntroduceAssociationTypeListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((IntroduceAssociationTypeListener) listener).introduceAssociationType(
                (AssociationType) params[0], (ClientState) params[1]
            );
        }
    };
}
