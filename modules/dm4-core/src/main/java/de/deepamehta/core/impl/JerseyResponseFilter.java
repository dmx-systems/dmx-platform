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
import de.deepamehta.core.service.WebSocketsService;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.logging.Logger;



/**
 * Response post-processing.
 * Post-processing takes place <i>after</i> a request is processed, <i>before</i> the response is sent to the client.
 * <p>
 * Post-processing includes 5 steps:
 * <ol>
 * <li>Fire the <code>CoreEvent.SERVICE_RESPONSE_FILTER</code> event to let plugins operate on the response, e.g.
 *     - the Caching plugin sets the <code>Cache-Control</code> response header
 *     - the Time plugin sets the <code>Last-Modified</code> response header
 * <li>Load child topics of the response object(s) if requested with the <code>include_childs</code> and
 *     <code>include_assoc_childs</code> query parameters.
 * <li>Fire the <code>CoreEvent.PRE_SEND_XXX</code> events for all response object(s) and objects contained in response
 *     directives. This let plugins operate on the response on a per-object basis, e.g.
 *     - the Geomaps plugin enriches an Address topic with its geo coordinate
 *     - the Time plugin enriches topics/associations with creation/modification timestamps
 * <li>Broadcast directives.
 * <li>Remove the (thread-local) directives assembled while request processing.
 * </ol>
 */
class JerseyResponseFilter implements ContainerResponseFilter {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private EventManager em;
    private WebSocketsService ws;

    @Context
    private HttpServletRequest request;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    JerseyResponseFilter(EventManager em, WebSocketsService ws) {
        this.em = em;
        this.ws = ws;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        try {
            em.fireEvent(CoreEvent.SERVICE_RESPONSE_FILTER, response);
            //
            Object entity = response.getEntity();
            if (entity != null) {
                //
                // 1) Loading child topics
                boolean includeChilds = getIncludeChilds(request);
                boolean includeAssocChilds = getIncludeAssocChilds(request);
                if (entity instanceof DeepaMehtaObject) {
                    loadChildTopics((DeepaMehtaObject) entity, includeChilds, includeAssocChilds);
                } else if (isIterable(response, DeepaMehtaObject.class)) {
                    loadChildTopics((Iterable<DeepaMehtaObject>) entity, includeChilds, includeAssocChilds);
                }
                //
                // 2) Firing PRE_SEND events
                Directives directives = null;
                if (entity instanceof DeepaMehtaObject) {
                    firePreSend((DeepaMehtaObject) entity);
                } else if (isIterable(response, DeepaMehtaObject.class)) {
                    firePreSend((Iterable<DeepaMehtaObject>) entity);
                } else if (entity instanceof DirectivesResponse) {
                    firePreSend(((DirectivesResponse) entity).getObject());
                    //
                    // Note: some plugins rely on the PRE_SEND event to be fired for the individual DeepaMehta
                    // objects contained in the set of directives. E.g. the Time plugin enriches updated objects
                    // with timestamps. The timestamps in turn are needed at client-side by the Caching plugin
                    // in order to issue conditional PUT requests.
                    // ### TODO: don't fire PRE_SEND events for the individual directives but only for the wrapped
                    // DeepaMehtaObject? Let the update() Core Service calls return the updated object?
                    directives = ((DirectivesResponse) entity).getDirectives();
                    firePreSend(directives);
                }
                //
                // 3) Broadcast directives
                if (directives != null) {
                    broadcast(directives);
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



    // === Broadcast ===

    private void broadcast(Directives directives) throws JSONException {
        JSONObject message = new JSONObject()
            .put("type", "processDirectives")
            .put("args", directives.toJSONArray());
        ws.messageToAllButOne(request, "de.deepamehta.webclient", message.toString());
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
