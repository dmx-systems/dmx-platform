package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.ChildTopics;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
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

    private EventManager em;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    JerseyResponseFilter(EventManager em) {
        this.em = em;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        try {
            em.fireEvent(CoreEvent.SERVICE_RESPONSE_FILTER, response);
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
                if (entity instanceof DeepaMehtaObject) {
                    firePreSend((DeepaMehtaObject) entity);
                } else if (isIterable(response, DeepaMehtaObject.class)) {
                    firePreSend((Iterable<DeepaMehtaObject>) entity);
                } else if (entity instanceof DirectivesResponse) {
                    firePreSend(((DirectivesResponse) entity).getObject());
                    //
                    // Note: some plugins rely on the PRE_SEND event to be fired for the individual DeepaMehta
                    // objects contained in the set of directives. E.g. the Time plugin enriches updated objects
                    // with  timestamps. The timestamps in turn are needed at client-side by the Caching plugin
                    // in order to issue conditional PUT requests.
                    // ### TODO: don't fire PRE_SEND events for the individual directives but only for the wrapped
                    // DeepaMehtaObject? Let the update() Core Service calls return the updated object?
                    firePreSend(((DirectivesResponse) entity).getDirectives());
                }
            }
            //
            Directives.remove();
            //
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Response filtering failed", e);
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
        for (DeepaMehtaObject object : objects) {
            loadChildTopics(object, includeChilds, includeChilds);
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

    private void firePreSend(DeepaMehtaObject object) {
        if (object instanceof TopicType) {                  // Note: must take precedence over topic
            em.fireEvent(CoreEvent.PRE_SEND_TOPIC_TYPE, object);
        } else if (object instanceof AssociationType) {     // Note: must take precedence over topic
            em.fireEvent(CoreEvent.PRE_SEND_ASSOCIATION_TYPE, object);
        } else if (object instanceof Topic) {
            em.fireEvent(CoreEvent.PRE_SEND_TOPIC, object);
        } else if (object instanceof Association) {
            em.fireEvent(CoreEvent.PRE_SEND_ASSOCIATION, object);
        }
    }

    private void firePreSend(Iterable<DeepaMehtaObject> objects) {
        for (DeepaMehtaObject object : objects) {
            firePreSend(object);
        }
    }

    private void firePreSend(Directives directives) {
        for (Directives.Entry entry : directives) {
            switch (entry.dir) {
            case UPDATE_TOPIC:
            case UPDATE_ASSOCIATION:
            case UPDATE_TOPIC_TYPE:
            case UPDATE_ASSOCIATION_TYPE:
                firePreSend((DeepaMehtaObject) entry.arg);
                break;
            }
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
