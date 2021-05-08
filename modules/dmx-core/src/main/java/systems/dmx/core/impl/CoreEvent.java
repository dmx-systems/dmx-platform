package systems.dmx.core.impl;

import systems.dmx.core.Assoc;
import systems.dmx.core.AssocType;
import systems.dmx.core.RoleType;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.AssocTypeModel;
import systems.dmx.core.model.RoleTypeModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicTypeModel;
import systems.dmx.core.service.ChangeReport;
import systems.dmx.core.service.DMXEvent;
import systems.dmx.core.service.EventListener;
import systems.dmx.core.service.event.*;

// ### TODO: hide Jersey internals. Upgrade to JAX-RS 2.0.
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



/**
 * Events fired by the DMX Core.
 * Plugins listen to these events by implementing the respective listener interfaces.
 *
 * @see systems.dmx.core.service.event
 */
class CoreEvent {

    static DMXEvent CHECK_TOPIC_READ_ACCESS = new DMXEvent(CheckTopicReadAccess.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((CheckTopicReadAccess) listener).checkTopicReadAccess(
                (Long) params[0]
            );
        }
    };

    static DMXEvent CHECK_ASSOCIATION_READ_ACCESS = new DMXEvent(CheckAssocReadAccess.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((CheckAssocReadAccess) listener).checkAssocReadAccess(
                (Long) params[0]
            );
        }
    };

    // ---

    static DMXEvent CHECK_TOPIC_WRITE_ACCESS = new DMXEvent(CheckTopicWriteAccess.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((CheckTopicWriteAccess) listener).checkTopicWriteAccess(
                (Long) params[0]
            );
        }
    };

    static DMXEvent CHECK_ASSOCIATION_WRITE_ACCESS = new DMXEvent(CheckAssocWriteAccess.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((CheckAssocWriteAccess) listener).checkAssocWriteAccess(
                (Long) params[0]
            );
        }
    };

    // ---

    static DMXEvent PRE_CREATE_TOPIC = new DMXEvent(PreCreateTopic.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreCreateTopic) listener).preCreateTopic(
                (TopicModel) params[0]
            );
        }
    };

    static DMXEvent PRE_CREATE_ASSOCIATION = new DMXEvent(PreCreateAssoc.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreCreateAssoc) listener).preCreateAssoc(
                (AssocModel) params[0]
            );
        }
    };

    static DMXEvent PRE_CREATE_TOPIC_TYPE = new DMXEvent(PreCreateTopicType.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreCreateTopicType) listener).preCreateTopicType(
                (TopicTypeModel) params[0]
            );
        }
    };

    static DMXEvent PRE_CREATE_ASSOCIATION_TYPE = new DMXEvent(PreCreateAssocType.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreCreateAssocType) listener).preCreateAssocType(
                (AssocTypeModel) params[0]
            );
        }
    };

    static DMXEvent PRE_CREATE_ROLE_TYPE = new DMXEvent(PreCreateRoleType.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreCreateRoleType) listener).preCreateRoleType(
                (RoleTypeModel) params[0]
            );
        }
    };

    // ---

    static DMXEvent POST_CREATE_TOPIC = new DMXEvent(PostCreateTopic.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PostCreateTopic) listener).postCreateTopic(
                (Topic) params[0]
            );
        }
    };

    static DMXEvent POST_CREATE_ASSOCIATION = new DMXEvent(PostCreateAssoc.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PostCreateAssoc) listener).postCreateAssoc(
                (Assoc) params[0]
            );
        }
    };

    // ---

    static DMXEvent PRE_UPDATE_TOPIC = new DMXEvent(PreUpdateTopic.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreUpdateTopic) listener).preUpdateTopic(
                (Topic) params[0], (TopicModel) params[1]
            );
        }
    };

    static DMXEvent PRE_UPDATE_ASSOCIATION = new DMXEvent(PreUpdateAssoc.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreUpdateAssoc) listener).preUpdateAssoc(
                (Assoc) params[0], (AssocModel) params[1]
            );
        }
    };

    // ---

    static DMXEvent POST_UPDATE_TOPIC = new DMXEvent(PostUpdateTopic.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PostUpdateTopic) listener).postUpdateTopic(
                (Topic) params[0], (ChangeReport) params[1], (TopicModel) params[2]
            );
        }
    };

    static DMXEvent POST_UPDATE_ASSOCIATION = new DMXEvent(PostUpdateAssoc.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PostUpdateAssoc) listener).postUpdateAssoc(
                (Assoc) params[0], (ChangeReport) params[1], (AssocModel) params[2]
            );
        }
    };

    // ---

    static DMXEvent PRE_DELETE_TOPIC = new DMXEvent(PreDeleteTopic.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreDeleteTopic) listener).preDeleteTopic(
                (Topic) params[0]
            );
        }
    };

    static DMXEvent PRE_DELETE_ASSOCIATION = new DMXEvent(PreDeleteAssoc.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreDeleteAssoc) listener).preDeleteAssoc(
                (Assoc) params[0]
            );
        }
    };

    // ---

    static DMXEvent POST_DELETE_TOPIC = new DMXEvent(PostDeleteTopic.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PostDeleteTopic) listener).postDeleteTopic(
                (TopicModel) params[0]
            );
        }
    };

    static DMXEvent POST_DELETE_ASSOCIATION = new DMXEvent(PostDeleteAssoc.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PostDeleteAssoc) listener).postDeleteAssoc(
                (AssocModel) params[0]
            );
        }
    };

    // ---

    static DMXEvent ALL_PLUGINS_ACTIVE = new DMXEvent(AllPluginsActive.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((AllPluginsActive) listener).allPluginsActive();
        }
    };

    // ---

    // This event has a double nature:
    //   a) it is fired regularily (see AccessLayer.createTopicType()).
    //   b) it is fired locally (see PluginImpl.introduceTopicTypesToPlugin()).
    static DMXEvent INTRODUCE_TOPIC_TYPE = new DMXEvent(IntroduceTopicType.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((IntroduceTopicType) listener).introduceTopicType(
                (TopicType) params[0]
            );
        }
    };

    // This event has a double nature:
    //   a) it is fired regularily (see AccessLayer.createAssocType()).
    //   b) it is fired locally (see PluginImpl.introduceAssocTypesToPlugin()).
    static DMXEvent INTRODUCE_ASSOCIATION_TYPE = new DMXEvent(IntroduceAssocType.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((IntroduceAssocType) listener).introduceAssocType(
                (AssocType) params[0]
            );
        }
    };

    // This event has a double nature:
    //   a) it is fired regularily (see AccessLayer.createRoleType()).
    //   b) it is fired locally (see PluginImpl.introduceRoleTypesToPlugin()).
    static DMXEvent INTRODUCE_ROLE_TYPE = new DMXEvent(IntroduceRoleType.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((IntroduceRoleType) listener).introduceRoleType(
                (RoleType) params[0]
            );
        }
    };



    // === WebPublishing Events ===

    static DMXEvent SERVICE_REQUEST_FILTER = new DMXEvent(ServiceRequestFilter.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((ServiceRequestFilter) listener).serviceRequestFilter(
                (ContainerRequest) params[0]
            );
        }
    };

    static DMXEvent SERVICE_RESPONSE_FILTER = new DMXEvent(ServiceResponseFilter.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((ServiceResponseFilter) listener).serviceResponseFilter(
                (ContainerResponse) params[0]
            );
        }
    };

    static DMXEvent STATIC_RESOURCE_FILTER = new DMXEvent(StaticResourceFilter.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((StaticResourceFilter) listener).staticResourceFilter(
                (HttpServletRequest) params[0], (HttpServletResponse) params[1]
            );
        }
    };

    // ---

    static DMXEvent PRE_SEND_TOPIC = new DMXEvent(PreSendTopic.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreSendTopic) listener).preSendTopic(
                (Topic) params[0]
            );
        }
    };

    static DMXEvent PRE_SEND_ASSOCIATION = new DMXEvent(PreSendAssoc.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreSendAssoc) listener).preSendAssoc(
                (Assoc) params[0]
            );
        }
    };

    static DMXEvent PRE_SEND_TOPIC_TYPE = new DMXEvent(PreSendTopicType.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreSendTopicType) listener).preSendTopicType(
                (TopicType) params[0]
            );
        }
    };

    static DMXEvent PRE_SEND_ASSOCIATION_TYPE = new DMXEvent(PreSendAssocType.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((PreSendAssocType) listener).preSendAssocType(
                (AssocType) params[0]
            );
        }
    };



    // === WebSockets Events ===

    static DMXEvent WEBSOCKET_TEXT_MESSAGE = new DMXEvent(WebsocketTextMessage.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((WebsocketTextMessage) listener).websocketTextMessage(
                (String) params[0]
            );
        }
    };
}
