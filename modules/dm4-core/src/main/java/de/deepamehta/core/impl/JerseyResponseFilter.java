package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.ChildTopics;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.DirectivesResponse;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.logging.Logger;



class JerseyResponseFilter implements ContainerResponseFilter {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DeepaMehtaService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    JerseyResponseFilter(DeepaMehtaService dms) {
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        try {
            dms.fireEvent(CoreEvent.SERVICE_RESPONSE_FILTER, response);
            //
            Object entity = response.getEntity();
            boolean includeChilds = getIncludeChilds(request);
            boolean includeAssocChilds = getIncludeAssocChilds(request);
            if (entity != null) {
                //
                // 1) Loading child topics
                // ### TODO: move to Webservice module?
                if (entity instanceof DeepaMehtaObject) {
                    loadChildTopics((DeepaMehtaObject) entity, includeChilds, includeAssocChilds);
                } else if (isIterable(response, DeepaMehtaObject.class)) {
                    loadChildTopics((Iterable<DeepaMehtaObject>) entity, includeChilds, includeAssocChilds);
                }
                //
                // 2) Firing PRE_SEND events
                // ### TODO: move to Webservice module?
                if (entity instanceof TopicType) {          // Note: must take precedence over topic
                    firePreSend((TopicType) entity);
                } else if (entity instanceof AssociationType) {
                    firePreSend((AssociationType) entity);  // Note: must take precedence over topic
                } else if (entity instanceof Topic) {
                    firePreSend((Topic) entity);
                } else if (entity instanceof Association) {
                    firePreSend((Association) entity);
                } else if (entity instanceof DirectivesResponse) {
                    // Note: some plugins rely on the PRE_SEND event to be fired for the individual DeepaMehta
                    // objects contained in the set of directives. E.g. the Time plugin enriches updated objects
                    // with  timestamps. The timestamps in turn are needed at client-side by the Caching plugin
                    // in order to issue conditional PUT requests.
                    // ### TODO: don't fire PRE_SEND events for the individual directives but only for the wrapped
                    // DeepaMehtaObject? Let the update() Core Service calls return the updated object?
                    firePreSend(((DirectivesResponse) entity).getDirectives());
                } else if (isIterable(response, TopicType.class)) {
                    firePreSendTopicTypes((Iterable<TopicType>) entity);
                } else if (isIterable(response, AssociationType.class)) {
                    firePreSendAssociationTypes((Iterable<AssociationType>) entity);
                } else if (isIterable(response, Topic.class)) {
                    firePreSendTopics((Iterable<Topic>) entity);
                // ### FIXME: for Iterable<Association> no PRE_SEND_ASSOCIATION events are fired
                }
            }
            //
            Directives.remove();
            //
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Jersey response filtering failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    // === Loading child topics ===

    private void loadChildTopics(DeepaMehtaObject object, boolean includeChilds,
                                                          boolean includeAssocChilds) {
        if (includeChilds) {
            object.loadChildTopics();
            if (includeAssocChilds) {
                loadRelatingAssociationChildTopics(object);
            }
        }
    }

    private void loadChildTopics(Iterable<DeepaMehtaObject> objects, boolean includeChilds,
                                                                     boolean includeAssocChilds) {
        if (includeChilds) {
            for (DeepaMehtaObject object : objects) {
                object.loadChildTopics();
            }
            if (includeAssocChilds) {
                for (DeepaMehtaObject object : objects) {
                    loadRelatingAssociationChildTopics(object);
                }
            }
        }
    }

    // ---

    private void loadRelatingAssociationChildTopics(DeepaMehtaObject object) {
        ChildTopics childTopics = object.getChildTopics();
        for (String childTypeUri : childTopics) {
            Object value = childTopics.get(childTypeUri);
            if (value instanceof RelatedTopic) {
                RelatedTopic childTopic = (RelatedTopic) value;
                childTopic.getRelatingAssociation().loadChildTopics();
                loadRelatingAssociationChildTopics(childTopic);         // recursion
            } else if (value instanceof List) {
                for (RelatedTopic childTopic : (List<RelatedTopic>) value) {
                    childTopic.getRelatingAssociation().loadChildTopics();
                    loadRelatingAssociationChildTopics(childTopic);     // recursion
                }
            } else {
                throw new RuntimeException("Unexpected \"" + childTypeUri + "\" value in ChildTopics: " + value);
            }
        }
    }

    // === Firing PRE_SEND events ===

    private void firePreSend(Topic topic) {
        dms.fireEvent(CoreEvent.PRE_SEND_TOPIC, topic);
    }

    private void firePreSend(Association assoc) {
        dms.fireEvent(CoreEvent.PRE_SEND_ASSOCIATION, assoc);
    }

    private void firePreSend(TopicType topicType) {
        dms.fireEvent(CoreEvent.PRE_SEND_TOPIC_TYPE, topicType);
    }

    private void firePreSend(AssociationType assocType) {
        dms.fireEvent(CoreEvent.PRE_SEND_ASSOCIATION_TYPE, assocType);
    }

    private void firePreSend(Directives directives) {
        for (Directives.Entry entry : directives) {
            switch (entry.dir) {
            case UPDATE_TOPIC:
                firePreSend((Topic) entry.arg);
                break;
            case UPDATE_ASSOCIATION:
                firePreSend((Association) entry.arg);
                break;
            case UPDATE_TOPIC_TYPE:
                firePreSend((TopicType) entry.arg);
                break;
            case UPDATE_ASSOCIATION_TYPE:
                firePreSend((AssociationType) entry.arg);
                break;
            }
        }
    }

    private void firePreSendTopics(Iterable<Topic> topics) {
        for (Topic topic : topics) {
            firePreSend(topic);
        }
    }

    private void firePreSendTopicTypes(Iterable<TopicType> topicTypes) {
        for (TopicType topicType : topicTypes) {
            firePreSend(topicType);
        }
    }

    private void firePreSendAssociationTypes(Iterable<AssociationType> assocTypes) {
        for (AssociationType assocType : assocTypes) {
            firePreSend(assocType);
        }
    }

    // === Helper ===

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

    // ---

    private boolean getIncludeChilds(ContainerRequest request) {
        return getBooleanQueryParameter(request, "include_childs");
    }

    private boolean getIncludeAssocChilds(ContainerRequest request) {
        return getBooleanQueryParameter(request, "include_assoc_childs");
    }

    // ---

    private boolean getBooleanQueryParameter(ContainerRequest request, String param) {
        return Boolean.parseBoolean(request.getQueryParameters().getFirst(param));
    }    
}
