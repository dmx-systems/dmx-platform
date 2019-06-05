package systems.dmx.core.impl;

import systems.dmx.core.Assoc;
import systems.dmx.core.AssociationType;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.model.AssociationTypeModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicTypeModel;
import systems.dmx.core.service.DMXEvent;
import systems.dmx.core.service.EventListener;
import systems.dmx.core.service.event.*;

// ### TODO: hide Jersey internals. Upgrade to JAX-RS 2.0.
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



/**
 * Events fired by the DMX core service.
 * Plugins can listen to these events by implementing the respective listener interfaces.
 *
 * @see systems.dmx.core.service.event
 */
class CoreEvent {

    static DMXEvent CHECK_TOPIC_READ_ACCESS = new DMXEvent(CheckTopicReadAccessListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((CheckTopicReadAccessListener) listener).checkTopicReadAccess(
                (Long) params[0]
            );
        }
    };

    static DMXEvent CHECK_ASSOCIATION_READ_ACCESS =
                                                         new DMXEvent(CheckAssociationReadAccessListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((CheckAssociationReadAccessListener) listener).checkAssociationReadAccess(
                (Long) params[0]
            );
        }
    };

    // ---

    static DMXEvent CHECK_TOPIC_WRITE_ACCESS = new DMXEvent(CheckTopicWriteAccessListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((CheckTopicWriteAccessListener) listener).checkTopicWriteAccess(
                (Long) params[0]
            );
        }
    };

    static DMXEvent CHECK_ASSOCIATION_WRITE_ACCESS =
                                                        new DMXEvent(CheckAssociationWriteAccessListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((CheckAssociationWriteAccessListener) listener).checkAssociationWriteAccess(
                (Long) params[0]
            );
        }
    };

    // ---

    static DMXEvent PRE_CREATE_TOPIC = new DMXEvent(PreCreateTopicListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreCreateTopicListener) listener).preCreateTopic(
                (TopicModel) params[0]
            );
        }
    };

    static DMXEvent PRE_CREATE_ASSOCIATION = new DMXEvent(PreCreateAssociationListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreCreateAssociationListener) listener).preCreateAssociation(
                (AssociationModel) params[0]
            );
        }
    };

    static DMXEvent PRE_CREATE_TOPIC_TYPE = new DMXEvent(PreCreateTopicTypeListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreCreateTopicTypeListener) listener).preCreateTopicType(
                (TopicTypeModel) params[0]
            );
        }
    };

    static DMXEvent PRE_CREATE_ASSOCIATION_TYPE = new DMXEvent(PreCreateAssociationTypeListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreCreateAssociationTypeListener) listener).preCreateAssociationType(
                (AssociationTypeModel) params[0]
            );
        }
    };

    // ---

    static DMXEvent POST_CREATE_TOPIC = new DMXEvent(PostCreateTopicListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PostCreateTopicListener) listener).postCreateTopic(
                (Topic) params[0]
            );
        }
    };

    static DMXEvent POST_CREATE_ASSOCIATION = new DMXEvent(PostCreateAssociationListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PostCreateAssociationListener) listener).postCreateAssociation(
                (Assoc) params[0]
            );
        }
    };

    // ---

    static DMXEvent PRE_UPDATE_TOPIC = new DMXEvent(PreUpdateTopicListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreUpdateTopicListener) listener).preUpdateTopic(
                (Topic) params[0], (TopicModel) params[1]
            );
        }
    };

    static DMXEvent PRE_UPDATE_ASSOCIATION = new DMXEvent(PreUpdateAssociationListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreUpdateAssociationListener) listener).preUpdateAssociation(
                (Assoc) params[0], (AssociationModel) params[1]
            );
        }
    };

    // ---

    static DMXEvent POST_UPDATE_TOPIC = new DMXEvent(PostUpdateTopicListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PostUpdateTopicListener) listener).postUpdateTopic(
                (Topic) params[0], (TopicModel) params[1], (TopicModel) params[2]
            );
        }
    };

    static DMXEvent POST_UPDATE_ASSOCIATION = new DMXEvent(PostUpdateAssociationListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PostUpdateAssociationListener) listener).postUpdateAssociation(
                (Assoc) params[0], (AssociationModel) params[1], (AssociationModel) params[2]
            );
        }
    };

    // ---

    static DMXEvent PRE_DELETE_TOPIC = new DMXEvent(PreDeleteTopicListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreDeleteTopicListener) listener).preDeleteTopic(
                (Topic) params[0]
            );
        }
    };

    static DMXEvent PRE_DELETE_ASSOCIATION = new DMXEvent(PreDeleteAssociationListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreDeleteAssociationListener) listener).preDeleteAssociation(
                (Assoc) params[0]
            );
        }
    };

    // ---

    static DMXEvent POST_DELETE_TOPIC = new DMXEvent(PostDeleteTopicListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PostDeleteTopicListener) listener).postDeleteTopic(
                (TopicModel) params[0]
            );
        }
    };

    static DMXEvent POST_DELETE_ASSOCIATION = new DMXEvent(PostDeleteAssociationListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PostDeleteAssociationListener) listener).postDeleteAssociation(
                (AssociationModel) params[0]
            );
        }
    };

    // ---

    static DMXEvent ALL_PLUGINS_ACTIVE = new DMXEvent(AllPluginsActiveListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((AllPluginsActiveListener) listener).allPluginsActive();
        }
    };

    // ---

    // This event has a double nature:
    //   a) it is fired regularily (see CoreServiceImpl.createTopicType()).
    //   b) it is fired locally (see PluginImpl.introduceTopicTypesToPlugin()).
    static DMXEvent INTRODUCE_TOPIC_TYPE = new DMXEvent(IntroduceTopicTypeListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((IntroduceTopicTypeListener) listener).introduceTopicType(
                (TopicType) params[0]
            );
        }
    };

    // This event has a double nature:
    //   a) it is fired regularily (see CoreServiceImpl.createAssociationType()).
    //   b) it is fired locally (see PluginImpl.introduceAssociationTypesToPlugin()).
    static DMXEvent INTRODUCE_ASSOCIATION_TYPE = new DMXEvent(IntroduceAssociationTypeListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((IntroduceAssociationTypeListener) listener).introduceAssociationType(
                (AssociationType) params[0]
            );
        }
    };



    // === WebPublishing Events ===

    static DMXEvent SERVICE_REQUEST_FILTER = new DMXEvent(ServiceRequestFilterListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((ServiceRequestFilterListener) listener).serviceRequestFilter(
                (ContainerRequest) params[0]
            );
        }
    };

    static DMXEvent SERVICE_RESPONSE_FILTER = new DMXEvent(ServiceResponseFilterListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((ServiceResponseFilterListener) listener).serviceResponseFilter(
                (ContainerResponse) params[0]
            );
        }
    };

    static DMXEvent STATIC_RESOURCE_FILTER = new DMXEvent(StaticResourceFilterListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((StaticResourceFilterListener) listener).staticResourceFilter(
                (HttpServletRequest) params[0], (HttpServletResponse) params[1]
            );
        }
    };

    // ---

    static DMXEvent PRE_SEND_TOPIC = new DMXEvent(PreSendTopicListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreSendTopicListener) listener).preSendTopic(
                (Topic) params[0]
            );
        }
    };

    static DMXEvent PRE_SEND_ASSOCIATION = new DMXEvent(PreSendAssociationListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreSendAssociationListener) listener).preSendAssociation(
                (Assoc) params[0]
            );
        }
    };

    static DMXEvent PRE_SEND_TOPIC_TYPE = new DMXEvent(PreSendTopicTypeListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreSendTopicTypeListener) listener).preSendTopicType(
                (TopicType) params[0]
            );
        }
    };

    static DMXEvent PRE_SEND_ASSOCIATION_TYPE = new DMXEvent(PreSendAssociationTypeListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreSendAssociationTypeListener) listener).preSendAssociationType(
                (AssociationType) params[0]
            );
        }
    };



    // === WebSockets Events ===

    static DMXEvent WEBSOCKET_TEXT_MESSAGE = new DMXEvent(WebsocketTextMessageListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((WebsocketTextMessageListener) listener).websocketTextMessage(
                (String) params[0]
            );
        }
    };
}
