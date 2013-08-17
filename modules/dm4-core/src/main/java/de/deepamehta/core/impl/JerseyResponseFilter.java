package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.logging.Logger;



class JerseyResponseFilter implements ContainerResponseFilter {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    JerseyResponseFilter(EmbeddedService dms) {
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        try {
            dms.fireEvent(CoreEvent.SERVICE_RESPONSE_FILTER, response);
            //
            Object entity = response.getEntity();
            if (entity != null) {
                ClientState clientState = clientState(request);
                if (entity instanceof TopicType) {                      // Note: must take precedence over topic
                    firePreSend((TopicType) entity, clientState);
                } else if (entity instanceof AssociationType) {
                    firePreSend((AssociationType) entity, clientState); // Note: must take precedence over topic
                } else if (entity instanceof Topic) {
                    firePreSend((Topic) entity, clientState);
                } else if (entity instanceof Association) {
                    firePreSend((Association) entity, clientState);
                } else if (entity instanceof Directives) {
                    // Note: some plugins rely on the PRE_SEND event in order to enrich updated objects, others don't.
                    // E.g. the Access Control plugin must enrich updated objects with permission information.
                    firePreSend((Directives) entity, clientState);
                } else if (isIterable(response, TopicType.class)) {
                    firePreSendTopicTypes((Iterable<TopicType>) entity, clientState);
                } else if (isIterable(response, AssociationType.class)) {
                    firePreSendAssociationTypes((Iterable<AssociationType>) entity, clientState);
                } else if (isIterable(response, Topic.class)) {
                    firePreSendTopics((Iterable<Topic>) entity, clientState);
                }
            }
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Jersey response filtering failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void firePreSend(Topic topic, ClientState clientState) {
        dms.fireEvent(CoreEvent.PRE_SEND_TOPIC, topic, clientState);
    }

    private void firePreSend(Association assoc, ClientState clientState) {
        dms.fireEvent(CoreEvent.PRE_SEND_ASSOCIATION, assoc, clientState);
    }

    private void firePreSend(TopicType topicType, ClientState clientState) {
        dms.fireEvent(CoreEvent.PRE_SEND_TOPIC_TYPE, topicType, clientState);
    }

    private void firePreSend(AssociationType assocType, ClientState clientState) {
        dms.fireEvent(CoreEvent.PRE_SEND_ASSOCIATION_TYPE, assocType, clientState);
    }

    private void firePreSend(Directives directives, ClientState clientState) {
        for (Directives.Entry entry : directives) {
            switch (entry.dir) {
            case UPDATE_TOPIC:
                firePreSend((Topic) entry.arg, clientState);
                break;
            case UPDATE_ASSOCIATION:
                firePreSend((Association) entry.arg, clientState);
                break;
            case UPDATE_TOPIC_TYPE:
                firePreSend((TopicType) entry.arg, clientState);
                break;
            case UPDATE_ASSOCIATION_TYPE:
                firePreSend((AssociationType) entry.arg, clientState);
                break;
            }
        }
    }

    private void firePreSendTopics(Iterable<Topic> topics, ClientState clientState) {
        for (Topic topic : topics) {
            firePreSend(topic, clientState);
        }
    }

    private void firePreSendTopicTypes(Iterable<TopicType> topicTypes, ClientState clientState) {
        for (TopicType topicType : topicTypes) {
            firePreSend(topicType, clientState);
        }
    }

    private void firePreSendAssociationTypes(Iterable<AssociationType> assocTypes, ClientState clientState) {
        for (AssociationType assocType : assocTypes) {
            firePreSend(assocType, clientState);
        }
    }

    // ---

    private boolean isIterable(ContainerResponse response, Class elementType) {
        Type genericType = response.getEntityType();
        if (genericType instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
            Class<?> type = response.getEntity().getClass();
            if (typeArgs.length == 1 && Iterable.class.isAssignableFrom(type) &&
                                           elementType.isAssignableFrom((Class) typeArgs[0])) {
                return true;
            }
        }
        return false;
    }

    private ClientState clientState(ContainerRequest request) {
        List<String> cookies = request.getRequestHeader("Cookie");
        if (cookies == null) {
            return new ClientState(null);
        }
        // ### FIXME: does this happen?
        if (cookies.size() > 1) {
            throw new RuntimeException("Request contains more than one Cookie header");
        }
        //
        return new ClientState(cookies.get(0));
    }
}
