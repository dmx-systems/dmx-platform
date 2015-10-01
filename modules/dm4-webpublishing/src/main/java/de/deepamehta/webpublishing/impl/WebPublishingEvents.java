package de.deepamehta.webpublishing.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.service.DeepaMehtaEvent;
import de.deepamehta.core.service.EventListener;
import de.deepamehta.webpublishing.listeners.*;

// ### TODO: hide Jersey internals. Move to JAX-RS 2.0.
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;

import javax.servlet.http.HttpServletRequest;



class WebPublishingEvents {

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
                (Topic) params[0]
            );
        }
    };

    static DeepaMehtaEvent PRE_SEND_ASSOCIATION = new DeepaMehtaEvent(PreSendAssociationListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PreSendAssociationListener) listener).preSendAssociation(
                (Association) params[0]
            );
        }
    };

    static DeepaMehtaEvent PRE_SEND_TOPIC_TYPE = new DeepaMehtaEvent(PreSendTopicTypeListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PreSendTopicTypeListener) listener).preSendTopicType(
                (TopicType) params[0]
            );
        }
    };

    static DeepaMehtaEvent PRE_SEND_ASSOCIATION_TYPE = new DeepaMehtaEvent(PreSendAssociationTypeListener.class) {
        @Override
        public void deliver(EventListener listener, Object... params) {
            ((PreSendAssociationTypeListener) listener).preSendAssociationType(
                (AssociationType) params[0]
            );
        }
    };
}
