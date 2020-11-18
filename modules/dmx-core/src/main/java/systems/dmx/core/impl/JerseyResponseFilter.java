package systems.dmx.core.impl;

import systems.dmx.core.Assoc;
import systems.dmx.core.AssocType;
import systems.dmx.core.DMXObject;
import systems.dmx.core.ChildTopics;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.impl.Messages.Message;
import systems.dmx.core.service.Directives;
import systems.dmx.core.service.DirectivesResponse;
import systems.dmx.core.service.QueryResult;
import systems.dmx.core.service.TopicResult;
import systems.dmx.core.service.websocket.WebSocketService;

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
 *     - the Timestamps plugin sets the <code>Last-Modified</code> response header
 * <li>Load child topics of the response object(s) if requested with the <code>children</code> and
 *     <code>assocChildren</code> query parameters.
 * <li>Fire the <code>CoreEvent.PRE_SEND_XXX</code> events for all response object(s) and objects contained in response
 *     directives. This let plugins operate on the response on a per-object basis, e.g.
 *     - the Geomaps plugin enriches an Address topic with its geo coordinate
 *     - the Timestamps plugin enriches topics/associations with creation/modification timestamps
 * <li>Broadcast directives.
 * <li>Remove the (thread-local) directives assembled while request processing.
 * </ol>
 */
class JerseyResponseFilter implements ContainerResponseFilter {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private EventManager em;
    private WebSocketServiceImpl wss;

    @Context
    private HttpServletRequest request;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    JerseyResponseFilter(EventManager em, WebSocketServiceImpl wss) {
        this.em = em;
        this.wss = wss;
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
                boolean includeChildren = getIncludeChildren(request);
                boolean includeAssocChildren = getIncludeAssocChildren(request);
                if (entity instanceof DMXObject) {
                    loadChildTopics((DMXObject) entity, includeChildren, includeAssocChildren);
                } else if (isIterable(response, DMXObject.class)) {
                    loadChildTopics((Iterable<DMXObject>) entity, includeChildren, includeAssocChildren);
                }
                // 2) Firing PRE_SEND events
                Directives directives = null;
                if (entity instanceof DMXObject) {
                    firePreSend((DMXObject) entity);
                } else if (isIterable(response, DMXObject.class)) {
                    firePreSend((Iterable<DMXObject>) entity);
                } else if (entity instanceof TopicResult) {
                    firePreSend(((TopicResult) entity).topics);
                } else if (entity instanceof QueryResult) {
                    firePreSend(((QueryResult) entity).objects);
                } else if (entity instanceof DirectivesResponse) {
                    firePreSend(((DirectivesResponse) entity).getObject());
                    //
                    // Note: some plugins rely on the PRE_SEND event to be fired for the individual DMX
                    // objects contained in the set of directives. E.g. the Timestamps plugin enriches updated objects
                    // with timestamps. The timestamps in turn are needed at client-side by the Caching plugin
                    // in order to issue conditional PUT requests.
                    // ### TODO: don't fire PRE_SEND events for the individual directives but only for the wrapped
                    // DMXObject? Let the update() Core Service calls return the updated object?
                    directives = ((DirectivesResponse) entity).getDirectives();
                    firePreSend(directives);
                }
                // 3) Client-Sync
                if (directives != null) {
                    broadcast(directives);
                }
            }
            broadcast(Messages.get());
            Messages.remove();
            Directives.remove();
            //
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Response filtering failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Loading child topics ===

    private void loadChildTopics(DMXObject object, boolean includeChildren, boolean includeAssocChildren) {
        if (includeChildren) {
            object.loadChildTopics();
            if (includeAssocChildren) {
                loadRelatingAssocChildTopics(object);
            }
        }
    }

    private void loadChildTopics(Iterable<DMXObject> objects, boolean includeChildren, boolean includeAssocChildren) {
        for (DMXObject object : objects) {
            loadChildTopics(object, includeChildren, includeChildren);
        }
    }

    // ---

    private void loadRelatingAssocChildTopics(DMXObject object) {
        ChildTopics childTopics = object.getChildTopics();
        for (String childTypeUri : childTopics) {
            Object value = childTopics.get(childTypeUri);
            if (value instanceof RelatedTopic) {
                RelatedTopic childTopic = (RelatedTopic) value;
                childTopic.getRelatingAssoc().loadChildTopics();
                loadRelatingAssocChildTopics(childTopic);         // recursion
            } else if (value instanceof List) {
                for (RelatedTopic childTopic : (List<RelatedTopic>) value) {
                    childTopic.getRelatingAssoc().loadChildTopics();
                    loadRelatingAssocChildTopics(childTopic);     // recursion
                }
            } else {
                throw new RuntimeException("Unexpected \"" + childTypeUri + "\" value in ChildTopics: " + value);
            }
        }
    }



    // === Firing PRE_SEND events ===

    private void firePreSend(DMXObject object) {
        if (object instanceof TopicType) {                  // Note: must take precedence over topic
            em.fireEvent(CoreEvent.PRE_SEND_TOPIC_TYPE, object);
        } else if (object instanceof AssocType) {           // Note: must take precedence over topic
            em.fireEvent(CoreEvent.PRE_SEND_ASSOCIATION_TYPE, object);
        } else if (object instanceof Topic) {
            em.fireEvent(CoreEvent.PRE_SEND_TOPIC, object);
        } else if (object instanceof Assoc) {
            Assoc assoc = (Assoc) object;
            em.fireEvent(CoreEvent.PRE_SEND_ASSOCIATION, assoc);
            firePreSend(assoc.getDMXObject1());
            firePreSend(assoc.getDMXObject2());
        }
    }

    private void firePreSend(Iterable<? extends DMXObject> objects) {
        for (DMXObject object : objects) {
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
                firePreSend((DMXObject) entry.arg);
                break;
            }
        }
    }



    // === Client-Sync ===

    private void broadcast(Messages messages) {
        for (Message message : messages) {
            message.dest.send(message, wss);
        }
    }

    private void broadcast(Directives directives) throws JSONException {
        JSONObject message = new JSONObject()
            .put("type", "processDirectives")
            .put("args", directives.toJSONArray());
        wss._sendToAllButOrigin(message.toString());
        // FIXME: don't send update directives to unauthorized parties
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

    private boolean getIncludeChildren(ContainerRequest request) {
        return getBooleanQueryParameter(request, "children");
    }

    private boolean getIncludeAssocChildren(ContainerRequest request) {
        return getBooleanQueryParameter(request, "assocChildren");
    }

    // ---

    private boolean getBooleanQueryParameter(ContainerRequest request, String param) {
        return Boolean.parseBoolean(request.getQueryParameters().getFirst(param));
    }
}
